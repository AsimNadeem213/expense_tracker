package com.asim.splitmate.domain.usecase

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.utils.SplitCalculator
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.ExpenseRepository
import java.util.UUID

class AddExpenseUseCase(
    private val expenseRepository: ExpenseRepository
) {
    suspend fun execute(
        groupId: String,
        title: String,
        amount: Double,
        category: Category,
        paidByUser: User,
        selectedMembers: List<User>,
        splitType: SplitType,
        customSplits: List<Split>,
        notes: String = ""
    ): Resource<Expense> {
        if (title.isBlank()) return Resource.Error("Expense title cannot be empty")
        if (amount <= 0.0) return Resource.Error("Expense amount must be greater than zero")
        if (selectedMembers.isEmpty()) return Resource.Error("Select at least one participant")

        val computedSplits = SplitCalculator.calculateSplits(
            totalAmount = amount,
            splitType = splitType,
            selectedMembers = selectedMembers,
            customSplits = customSplits
        )

        val expense = Expense(
            id = "exp_" + UUID.randomUUID().toString().take(8),
            groupId = groupId,
            title = title.trim(),
            amount = amount,
            category = category,
            paidByUserId = paidByUser.id,
            paidByUserName = paidByUser.name,
            date = System.currentTimeMillis(),
            splitType = splitType,
            splits = computedSplits,
            notes = notes
        )

        return expenseRepository.addExpense(expense)
    }
}
