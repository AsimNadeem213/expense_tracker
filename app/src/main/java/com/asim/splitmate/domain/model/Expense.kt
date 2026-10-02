package com.asim.splitmate.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val category: Category = Category.OTHER,
    val paidByUserId: String,
    val paidByUserName: String,
    val date: Long = System.currentTimeMillis(),
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<Split> = emptyList(),
    val notes: String = "",
    val createdBy: String = paidByUserId,
    val isEdited: Boolean = false
)
