package com.asim.splitmate.data.repository

import android.content.Context
import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.database.ExpenseMateDatabase
import com.asim.splitmate.core.firebase.FirebaseHelper
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource,
    private val database: ExpenseMateDatabase,
    private val context: Context
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            val firebaseUser = FirebaseHelper.auth?.currentUser
            if (firebaseUser != null) {
                val fName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: entity?.name?.takeIf { it.isNotBlank() }
                    ?: (firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User")
                val fEmail = firebaseUser.email?.takeIf { it.isNotBlank() }
                    ?: entity?.email?.takeIf { it.isNotBlank() }
                    ?: ""
                User(
                    id = firebaseUser.uid,
                    name = fName,
                    email = fEmail,
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: entity?.avatarUrl,
                    isCurrentUser = true
                )
            } else {
                entity?.toDomain()
            }
        }
    }

    override suspend fun login(email: String, pass: String): Resource<User> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return Resource.Error("Please enter a valid email address")

        return try {
            val auth = FirebaseHelper.auth
            if (auth != null) {
                val result = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
                val firebaseUser = result.user ?: return Resource.Error("User login failed")
                val user = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                        ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = firebaseUser.email?.takeIf { it.isNotBlank() } ?: cleanEmail,
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    isCurrentUser = true
                )
                userDao.clearCurrentUser()
                userDao.insertUser(UserEntity.fromDomain(user))
                realtimeDatabaseDataSource.syncUser(user)
                Resource.Success(user)
            } else {
                val nameToUse = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                loginAsGuest(name = nameToUse, email = cleanEmail)
            }
        } catch (e: Exception) {
            val nameToUse = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            loginAsGuest(name = nameToUse, email = cleanEmail)
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Resource<User> {
        val cleanName = name.trim().ifBlank { "User" }
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return Resource.Error("Please enter a valid email address")

        return try {
            val auth = FirebaseHelper.auth
            if (auth != null) {
                val result = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val firebaseUser = result.user ?: return Resource.Error("User registration failed")
                val user = User(
                    id = firebaseUser.uid,
                    name = cleanName,
                    email = cleanEmail,
                    isCurrentUser = true
                )
                userDao.clearCurrentUser()
                userDao.insertUser(UserEntity.fromDomain(user))
                realtimeDatabaseDataSource.syncUser(user)
                Resource.Success(user)
            } else {
                loginAsGuest(name = cleanName, email = cleanEmail)
            }
        } catch (e: Exception) {
            loginAsGuest(name = cleanName, email = cleanEmail)
        }
    }

    override suspend fun loginAsGuest(name: String, email: String): Resource<User> {
        return try {
            val cleanName = name.trim().ifBlank { "Guest User" }
            val cleanEmail = when {
                email.isNotBlank() -> email.trim()
                else -> "${cleanName.lowercase().replace(" ", "")}@splitmate.app"
            }
            val userId = FirebaseHelper.currentUserId ?: ("usr_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16))

            val guestUser = User(
                id = userId,
                name = cleanName,
                email = cleanEmail,
                avatarUrl = null,
                isCurrentUser = true
            )
            userDao.clearCurrentUser()
            userDao.insertUser(UserEntity.fromDomain(guestUser))
            Resource.Success(guestUser)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create account profile", e)
        }
    }

    override suspend fun logout() {
        try {
            FirebaseHelper.auth?.signOut()
        } catch (_: Exception) {}

        try {
            database.clearAllTables()
            userDao.clearCurrentUser()
        } catch (e: Exception) {
            userDao.clearCurrentUser()
        }

        try {
            val prefs = context.getSharedPreferences("splitmate_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
