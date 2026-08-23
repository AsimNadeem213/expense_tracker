package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.entity.ExpenseEntity
import com.asim.splitmate.data.local.entity.ExpenseSplitEntity
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource
) : ExpenseRepository {

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesForGroup(groupId).map { entities ->
            entities.map { entity ->
                val splits = expenseDao.getSplitsForExpense(entity.id).map { it.toDomain() }
                entity.toDomain(splits)
            }
        }
    }

    override fun getRecentExpenses(limit: Int): Flow<List<Expense>> {
        return expenseDao.getRecentExpenses(limit).map { entities ->
            entities.map { entity ->
                val splits = expenseDao.getSplitsForExpense(entity.id).map { it.toDomain() }
                entity.toDomain(splits)
            }
        }
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map { entities ->
            entities.map { entity ->
                val splits = expenseDao.getSplitsForExpense(entity.id).map { it.toDomain() }
                entity.toDomain(splits)
            }
        }
    }

    override suspend fun getExpenseById(expenseId: String): Expense? {
        val entity = expenseDao.getExpenseById(expenseId) ?: return null
        val splits = expenseDao.getSplitsForExpense(expenseId).map { it.toDomain() }
        return entity.toDomain(splits)
    }

    override suspend fun addExpense(expense: Expense): Resource<Expense> {
        return try {
            val entity = ExpenseEntity.fromDomain(expense)
            val splitEntities = expense.splits.map { ExpenseSplitEntity.fromDomain(expense.id, it) }

            expenseDao.insertSplits(splitEntities)
            expenseDao.insertExpense(entity)

            realtimeDatabaseDataSource.syncExpense(expense)
            Resource.Success(expense)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add expense", e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Resource<Expense> {
        return try {
            expenseDao.deleteSplitsForExpense(expense.id)
            val entity = ExpenseEntity.fromDomain(expense)
            val splitEntities = expense.splits.map { ExpenseSplitEntity.fromDomain(expense.id, it) }

            expenseDao.insertSplits(splitEntities)
            expenseDao.insertExpense(entity)

            realtimeDatabaseDataSource.syncExpense(expense)
            Resource.Success(expense)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update expense", e)
        }
    }

    override suspend fun deleteExpense(expenseId: String): Resource<Unit> {
        return try {
            val expense = expenseDao.getExpenseById(expenseId)
            expenseDao.deleteSplitsForExpense(expenseId)
            expenseDao.deleteExpense(expenseId)
            if (expense != null) {
                realtimeDatabaseDataSource.deleteExpense(expense.groupId, expenseId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete expense", e)
        }
    }
}
