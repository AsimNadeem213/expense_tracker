package com.asim.splitmate.domain.usecase

import android.content.Context
import com.asim.splitmate.core.common.Resource
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
        existingExpenseId: String? = null,
        createdByUserId: String? = null
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

        if (splitType == SplitType.EXACT) {
            val totalAllocated = computedSplits.sumOf { it.amount }
            if (kotlin.math.abs(totalAllocated - amount) > 0.01) {
                return Resource.Error("The sum of split amounts (${String.format("%.2f", totalAllocated)}) must equal total amount (${String.format("%.2f", amount)})")
            }
        } else if (splitType == SplitType.PERCENTAGE) {
            val totalPct = computedSplits.sumOf { it.percentage }
            if (kotlin.math.abs(totalPct - 100.0) > 0.1) {
                return Resource.Error("The sum of split percentages (${String.format("%.1f", totalPct)}%) must equal 100%")
            }
        }

        val isEditMode = !existingExpenseId.isNullOrBlank()
        val expId = existingExpenseId?.takeIf { it.isNotBlank() }
            ?: ("exp_" + UUID.randomUUID().toString().take(8))

        val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        var originalCreatedBy = when {
            !currentAuthUid.isNullOrBlank() -> currentAuthUid
            !createdByUserId.isNullOrBlank() && createdByUserId != "usr_you" -> createdByUserId
            else -> paidByUser.id
        }
        if (isEditMode) {
            val existing = expenseRepository.getExpenseById(expId)
            if (existing != null && existing.createdBy.isNotBlank()) {
                val currentUid = currentAuthUid ?: createdByUserId?.takeIf { it.isNotBlank() }
                if (currentUid != null && existing.createdBy != currentUid && !(currentUid == "usr_you" && existing.createdBy.isBlank())) {
                    return Resource.Error("Only the member who added this expense can edit it")
                }
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

        if (result is Resource.Success && !isEditMode) {
            android.util.Log.d("AddExpenseUseCase", "Expense saved successfully")
        }

        return result
    }
}
