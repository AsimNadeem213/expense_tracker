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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
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
            val firebaseUser = FirebaseHelper.auth.currentUser
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
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank()) return Resource.Error("Please enter your email address")
        if (cleanPass.isBlank()) return Resource.Error("Please enter your password")

        return try {
            val auth = FirebaseHelper.auth
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            val firebaseUser = result.user ?: return Resource.Error("Login failed: no user record returned")
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
            try {
                realtimeDatabaseDataSource.syncUser(user)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to sync user to RTDB: ${e.message}")
            }
            Resource.Success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Resource.Error("No account found with this email. Please check your email or register.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Resource.Error("Invalid email or password. Please try again.")
        } catch (e: FirebaseNetworkException) {
            Resource.Error("Network error. Please check your internet connection.")
        } catch (e: FirebaseAuthException) {
            Resource.Error(e.localizedMessage ?: "Authentication failed (${e.errorCode})")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Login failed. Please check your credentials and try again.")
        }
    }

    override suspend fun register(name: String, email: String, pass: String): Resource<User> {
        val cleanName = name.trim().ifBlank { "User" }
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank()) return Resource.Error("Please enter a valid email address")
        if (cleanPass.isBlank()) return Resource.Error("Please enter a password")
        if (cleanPass.length < 6) return Resource.Error("Password must be at least 6 characters long")

        return try {
            val auth = FirebaseHelper.auth
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            val firebaseUser = result.user ?: return Resource.Error("Registration failed: no user record returned")

            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to set display name: ${e.message}")
            }

            val user = User(
                id = firebaseUser.uid,
                name = cleanName,
                email = cleanEmail,
                avatarUrl = null,
                isCurrentUser = true
            )
            userDao.clearCurrentUser()
            userDao.insertUser(UserEntity.fromDomain(user))
            try {
                realtimeDatabaseDataSource.syncUser(user)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to sync user to RTDB: ${e.message}")
            }
            Resource.Success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Resource.Error("An account with this email already exists. Please login instead.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            Resource.Error("Password is too weak. Please use at least 6 characters.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Resource.Error("The email address is improperly formatted.")
        } catch (e: FirebaseNetworkException) {
            Resource.Error("Network error. Please check your internet connection.")
        } catch (e: FirebaseAuthException) {
            Resource.Error(e.localizedMessage ?: "Registration failed (${e.errorCode})")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed. Please try again.")
        }
    }

    override suspend fun loginAsGuest(name: String, email: String): Resource<User> {
        return try {
            val cleanName = name.trim().ifBlank { "Guest User" }
            val cleanEmail = when {
                email.isNotBlank() -> email.trim()
                else -> "${cleanName.lowercase().replace(" ", "")}@splitmate.app"
            }

            val auth = FirebaseHelper.auth
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Firebase anonymous sign in failed: ${e.message}")
                }
            }

            val userId = FirebaseHelper.currentUserId ?: ("usr_" + UUID.randomUUID().toString().replace("-", "").take(16))

            val guestUser = User(
                id = userId,
                name = cleanName,
                email = cleanEmail,
                avatarUrl = null,
                isCurrentUser = true
            )
            userDao.clearCurrentUser()
            userDao.insertUser(UserEntity.fromDomain(guestUser))
            try {
                realtimeDatabaseDataSource.syncUser(guestUser)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to sync guest user to RTDB: ${e.message}")
            }
            Resource.Success(guestUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create guest session", e)
        }
    }

    override suspend fun logout() {
        try {
            FirebaseHelper.auth.signOut()
        } catch (_: Exception) {}

        try {
            database.clearAllTables()
            userDao.clearCurrentUser()
        } catch (e: Exception) {
            userDao.clearCurrentUser()
        }

        try {
            val prefs = context.getSharedPreferences("splitmate_prefs", Context.MODE_PRIVATE)
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", true)
            prefs.edit().clear().putBoolean("onboarding_completed", onboardingCompleted).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
