package com.asim.splitmate.domain.usecase

import android.content.Context
import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.notification.NotificationHelper
import com.asim.splitmate.core.utils.SplitCalculator
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AddExpenseUseCase(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val context: Context
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
        notes: String = "",
        date: Long = System.currentTimeMillis(),
        existingExpenseId: String? = null
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

        val isEditMode = !existingExpenseId.isNullOrBlank()
        val expId = existingExpenseId?.takeIf { it.isNotBlank() }
            ?: ("exp_" + UUID.randomUUID().toString().take(8))

        var originalCreatedBy = paidByUser.id
        if (isEditMode) {
            val existing = expenseRepository.getExpenseById(expId)
            if (existing != null && existing.createdBy.isNotBlank()) {
                originalCreatedBy = existing.createdBy
            }
        }

        val expense = Expense(
            id = expId,
            groupId = groupId,
            title = title.trim(),
            amount = amount,
            category = category,
            paidByUserId = paidByUser.id,
            paidByUserName = paidByUser.name,
            date = date,
            splitType = splitType,
            splits = computedSplits,
            notes = notes,
            createdBy = originalCreatedBy,
            isEdited = isEditMode
        )

        val result = if (isEditMode) expenseRepository.updateExpense(expense) else expenseRepository.addExpense(expense)

        if (result is Resource.Success) {
            try {
                NotificationHelper.subscribeToGroupTopic(groupId)
            } catch (e: Exception) {
                // Ignore non-fatal subscription errors
            }
        }

        return result
    }
}
