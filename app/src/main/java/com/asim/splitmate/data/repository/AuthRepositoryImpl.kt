package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
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
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { it?.toDomain() }
    }

    override suspend fun login(email: String, pass: String): Resource<User> {
        return try {
            val auth = FirebaseHelper.auth
            if (auth != null) {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = result.user ?: return Resource.Error("User login failed")
                val user = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: email.substringBefore("@"),
                    email = firebaseUser.email ?: email,
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    isCurrentUser = true
                )
                userDao.clearCurrentUser()
                userDao.insertUser(UserEntity.fromDomain(user))
                realtimeDatabaseDataSource.syncUser(user)
                Resource.Success(user)
            } else {
                loginAsGuest(email.substringBefore("@"))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Authentication error", e)
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Resource<User> {
        return try {
            val auth = FirebaseHelper.auth
            if (auth != null) {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = result.user ?: return Resource.Error("User registration failed")
                val user = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    isCurrentUser = true
                )
                userDao.clearCurrentUser()
                userDao.insertUser(UserEntity.fromDomain(user))
                realtimeDatabaseDataSource.syncUser(user)
                Resource.Success(user)
            } else {
                loginAsGuest(name)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration error", e)
        }
    }

    override suspend fun loginAsGuest(name: String): Resource<User> {
        return try {
            val current = userDao.getCurrentUserSync()
            if (current != null) {
                return Resource.Success(current.toDomain())
            }
            val guestUser = User(
                id = "usr_guest_" + UUID.randomUUID().toString().take(8),
                name = if (name.isNotBlank()) name else "Asim",
                email = "asim@splitmate.app",
                avatarUrl = null,
                isCurrentUser = true
            )
            userDao.clearCurrentUser()
            userDao.insertUser(UserEntity.fromDomain(guestUser))
            Resource.Success(guestUser)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create local guest profile", e)
        }
    }

    override suspend fun logout() {
        try {
            FirebaseHelper.auth?.signOut()
        } catch (_: Exception) {}
        userDao.clearCurrentUser()
    }
}
