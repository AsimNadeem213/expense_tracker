package com.asim.splitmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asim.splitmate.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val isCurrentUser: Boolean = false
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        avatarUrl = avatarUrl,
        phoneNumber = phoneNumber,
        isCurrentUser = isCurrentUser
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            avatarUrl = user.avatarUrl,
            phoneNumber = user.phoneNumber,
            isCurrentUser = user.isCurrentUser
        )
    }
}
