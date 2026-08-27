package com.asim.splitmate.domain.usecase

import com.asim.splitmate.core.utils.DebtSimplifier
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.abs

data class DashboardSummary(
    val groups: List<Group>,
    val recentExpenses: List<Expense>,
    val recentSettlements: List<Settlement>,
    val totalYouPaid: Double,
    val totalYouOwe: Double,
    val totalYouAreOwed: Double,
    val netOverallBalance: Double
)

class GetDashboardDataUseCase(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val userDao: UserDao
) {
    fun execute(currentUserId: String): Flow<DashboardSummary> {
        return combine(
            groupRepository.getAllGroups(),
            expenseRepository.getRecentExpenses(10),
            settlementRepository.getRecentSettlements(10)
        ) { groups, recentExpenses, recentSettlements ->
            var totalPaidByYou = 0.0
            var youOweTotal = 0.0
            var youAreOwedTotal = 0.0

            val currentUserDb = userDao.getCurrentUserSync()
            val firebaseUid = com.asim.splitmate.core.firebase.FirebaseHelper.currentUserId
            val possibleUserIds = setOfNotNull(
                currentUserId,
                currentUserDb?.id,
                currentUserDb?.name,
                "usr_you",
                "You",
                firebaseUid
            ).filter { it.isNotBlank() }.toSet()

            val allExpenses = expenseDao.getAllExpensesSync()
            for (exp in allExpenses) {
                if (possibleUserIds.contains(exp.paidByUserId) || exp.paidByUserId.equals("You", ignoreCase = true)) {
                    totalPaidByYou += exp.amount
                }
            }

            for (group in groups) {
                val groupMembers = groupDao.getGroupMembersSync(group.id).map { it.toDomain() }
                val expenseEntities = expenseDao.getExpensesForGroupSync(group.id)
                val groupExpenses = expenseEntities.map { entity ->
                    val splits = expenseDao.getSplitsForExpense(entity.id).map { it.toDomain() }
                    entity.toDomain(splits)
                }
                val settlementEntities = settlementDao.getSettlementsForGroupSync(group.id)
                val groupSettlements = settlementEntities.map { it.toDomain() }

                val netBalances = DebtSimplifier.calculateNetBalances(groupMembers, groupExpenses, groupSettlements)

                val currentUserBalance = netBalances.find { balance ->
                    possibleUserIds.contains(balance.userId) ||
                            groupMembers.find { it.id == balance.userId }?.isCurrentUser == true
                }

                if (currentUserBalance != null) {
                    val netAmt = currentUserBalance.netAmount
                    if (netAmt > 0.01) {
                        youAreOwedTotal += netAmt
                    } else if (netAmt < -0.01) {
                        youOweTotal += abs(netAmt)
                    }
                }
            }

            val roundedPaid = (totalPaidByYou * 100).let { kotlin.math.round(it) } / 100.0
            val roundedOwed = (youAreOwedTotal * 100).let { kotlin.math.round(it) } / 100.0
            val roundedOwe = (youOweTotal * 100).let { kotlin.math.round(it) } / 100.0

            DashboardSummary(
                groups = groups,
                recentExpenses = recentExpenses,
                recentSettlements = recentSettlements,
                totalYouPaid = roundedPaid,
                totalYouOwe = roundedOwe,
                totalYouAreOwed = roundedOwed,
                netOverallBalance = roundedOwed - roundedOwe
            )
        }
    }
}
