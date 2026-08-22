package com.asim.splitmate.domain.usecase

import com.asim.splitmate.core.utils.DebtSimplifier
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.domain.model.NetBalance
import com.asim.splitmate.domain.model.SimplifiedDebt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class GroupBalancesResult(
    val netBalances: List<NetBalance>,
    val simplifiedDebts: List<SimplifiedDebt>,
    val userNetBalance: Double
)

class CalculateGroupBalancesUseCase(
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao
) {
    fun execute(groupId: String, currentUserId: String): Flow<GroupBalancesResult> {
        return combine(
            groupDao.getGroupMembers(groupId),
            expenseDao.getExpensesForGroup(groupId),
            settlementDao.getSettlementsForGroup(groupId)
        ) { members, expenseEntities, settlementEntities ->
            val domainMembers = members.map { it.toDomain() }
            val domainExpenses = expenseEntities.map { entity ->
                val splits = expenseDao.getSplitsForExpense(entity.id).map { it.toDomain() }
                entity.toDomain(splits)
            }
            val domainSettlements = settlementEntities.map { it.toDomain() }

            val netBalances = DebtSimplifier.calculateNetBalances(domainMembers, domainExpenses, domainSettlements)
            val simplifiedDebts = DebtSimplifier.simplifyDebts(netBalances)
            val userNet = netBalances.find { it.userId == currentUserId }?.netAmount ?: 0.0

            GroupBalancesResult(
                netBalances = netBalances,
                simplifiedDebts = simplifiedDebts,
                userNetBalance = userNet
            )
        }
    }
}
