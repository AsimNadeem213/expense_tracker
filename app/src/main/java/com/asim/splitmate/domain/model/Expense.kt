package com.asim.splitmate.domain.model

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
    val notes: String = ""
)
