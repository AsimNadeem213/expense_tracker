package com.asim.splitmate.domain.model

enum class SplitType {
    EQUAL,
    EXACT,
    PERCENTAGE,
    SHARES
}

data class Split(
    val userId: String,
    val userName: String,
    val amount: Double = 0.0,
    val percentage: Double = 0.0,
    val shares: Int = 1
)
