package com.asim.splitmate.domain.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>
    fun getRecentExpenses(limit: Int = 10): Flow<List<Expense>>
    suspend fun getExpenseById(expenseId: String): Expense?
    suspend fun addExpense(expense: Expense): Resource<Expense>
    suspend fun updateExpense(expense: Expense): Resource<Expense>
    suspend fun deleteExpense(expenseId: String): Resource<Unit>
}
