package com.asim.splitmate.domain.model

data class NetBalance(
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val netAmount: Double // Positive = is owed, Negative = owes
)

data class SimplifiedDebt(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double
)
