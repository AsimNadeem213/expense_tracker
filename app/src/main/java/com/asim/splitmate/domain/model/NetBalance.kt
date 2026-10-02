package com.asim.splitmate.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class NetBalance(
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val netAmount: Double // Positive = is owed, Negative = owes
)

@Immutable
data class SimplifiedDebt(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double
)
