package com.asim.splitmate.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val isCurrentUser: Boolean = false
)
