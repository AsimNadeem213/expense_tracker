package com.asim.splitmate.domain.model

import androidx.compose.runtime.Immutable

enum class SplitType {
    EQUAL,
    EXACT,
    PERCENTAGE,
    SHARES
}

@Immutable
data class Split(
    val userId: String,
    val userName: String,
    val amount: Double = 0.0,
    val percentage: Double = 0.0,
    val shares: Int = 1
)
