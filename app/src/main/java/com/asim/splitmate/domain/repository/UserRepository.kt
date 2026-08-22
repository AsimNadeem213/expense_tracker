package com.asim.splitmate.domain.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.User

interface UserRepository {
    suspend fun getUserById(userId: String): User?
    suspend fun updateUserProfile(user: User): Resource<User>
}
