package com.asim.splitmate.domain.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, pass: String): Resource<User>
    suspend fun register(name: String, email: String, pass: String): Resource<User>
    suspend fun loginAsGuest(name: String = "Guest User"): Resource<User>
    suspend fun logout()
}
