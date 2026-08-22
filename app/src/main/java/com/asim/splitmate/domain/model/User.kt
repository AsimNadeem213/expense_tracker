package com.asim.splitmate.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val isCurrentUser: Boolean = false
)
