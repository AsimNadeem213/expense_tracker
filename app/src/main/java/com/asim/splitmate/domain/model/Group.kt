package com.asim.splitmate.domain.model

import androidx.compose.runtime.Immutable

enum class GroupType {
    TRIP,
    HOME,
    COUPLE,
    OTHER
}

@Immutable
data class Group(
    val id: String,
    val name: String,
    val description: String = "",
    val type: GroupType = GroupType.OTHER,
    val currencySymbol: String = "",
    val currencyCode: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<User> = emptyList(),
    val totalExpense: Double = 0.0,
    val inviteCode: String = ""
)
