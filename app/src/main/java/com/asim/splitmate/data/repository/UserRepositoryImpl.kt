package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.UserRepository

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource
) : UserRepository {

    override suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun updateUserProfile(user: User): Resource<User> {
        return try {
            userDao.insertUser(UserEntity.fromDomain(user))
            realtimeDatabaseDataSource.syncUser(user)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile", e)
        }
    }
}
