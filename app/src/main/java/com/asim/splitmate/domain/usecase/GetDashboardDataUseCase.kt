package com.asim.splitmate.domain.usecase

import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DashboardSummary(
    val groups: List<Group>,
    val recentExpenses: List<Expense>,
    val recentSettlements: List<Settlement>,
    val totalYouOwe: Double,
    val totalYouAreOwed: Double,
    val netOverallBalance: Double
)

class GetDashboardDataUseCase(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository
) {
    fun execute(currentUserId: String): Flow<DashboardSummary> {
        return combine(
            groupRepository.getAllGroups(),
            expenseRepository.getRecentExpenses(10),
            settlementRepository.getRecentSettlements(10)
        ) { groups, expenses, settlements ->
            var youOweTotal = 0.0
            var youAreOwedTotal = 0.0

            for (expense in expenses) {
                if (expense.paidByUserId == currentUserId) {
                    for (split in expense.splits) {
                        if (split.userId != currentUserId) {
                            youAreOwedTotal += split.amount
                        }
                    }
                } else {
                    val userSplit = expense.splits.find { it.userId == currentUserId }
                    if (userSplit != null) {
                        youOweTotal += userSplit.amount
                    }
                }
            }

            for (settlement in settlements) {
                if (settlement.payerId == currentUserId) {
                    youOweTotal -= settlement.amount
                } else if (settlement.recipientId == currentUserId) {
                    youAreOwedTotal -= settlement.amount
                }
            }

            youOweTotal = youOweTotal.coerceAtLeast(0.0)
            youAreOwedTotal = youAreOwedTotal.coerceAtLeast(0.0)
            val net = youAreOwedTotal - youOweTotal

            DashboardSummary(
                groups = groups,
                recentExpenses = expenses,
                recentSettlements = settlements,
                totalYouOwe = youOweTotal,
                totalYouAreOwed = youAreOwedTotal,
                netOverallBalance = net
            )
        }
    }
}
