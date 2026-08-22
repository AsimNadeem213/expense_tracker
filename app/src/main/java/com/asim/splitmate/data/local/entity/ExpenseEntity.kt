package com.asim.splitmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val categoryId: String,
    val paidByUserId: String,
    val paidByUserName: String,
    val date: Long,
    val splitType: String,
    val notes: String
) {
    fun toDomain(splits: List<Split>): Expense = Expense(
        id = id,
        groupId = groupId,
        title = title,
        amount = amount,
        category = Category.fromId(categoryId),
        paidByUserId = paidByUserId,
        paidByUserName = paidByUserName,
        date = date,
        splitType = try { SplitType.valueOf(splitType) } catch (e: Exception) { SplitType.EQUAL },
        splits = splits,
        notes = notes
    )

    companion object {
        fun fromDomain(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            groupId = expense.groupId,
            title = expense.title,
            amount = expense.amount,
            categoryId = expense.category.id,
            paidByUserId = expense.paidByUserId,
            paidByUserName = expense.paidByUserName,
            date = expense.date,
            splitType = expense.splitType.name,
            notes = expense.notes
        )
    }
}

@Entity(tableName = "expense_splits", primaryKeys = ["expenseId", "userId"])
data class ExpenseSplitEntity(
    val expenseId: String,
    val userId: String,
    val userName: String,
    val amount: Double,
    val percentage: Double = 0.0,
    val shares: Int = 1
) {
    fun toDomain(): Split = Split(
        userId = userId,
        userName = userName,
        amount = amount,
        percentage = percentage,
        shares = shares
    )

    companion object {
        fun fromDomain(expenseId: String, split: Split): ExpenseSplitEntity = ExpenseSplitEntity(
            expenseId = expenseId,
            userId = split.userId,
            userName = split.userName,
            amount = split.amount,
            percentage = split.percentage,
            shares = split.shares
        )
    }
}
