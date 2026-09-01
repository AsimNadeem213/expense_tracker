package com.asim.splitmate.core.utils

import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.NetBalance
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.model.SimplifiedDebt
import com.asim.splitmate.domain.model.User
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

object DebtSimplifier {

    /**
     * Calculates net balance for every member in a group based on expenses and recorded settlements.
     * Net balance > 0 => Person is OWED money.
     * Net balance < 0 => Person OWES money.
     */
    private fun resolveUserId(id: String, name: String, members: List<User>): String {
        if (members.any { it.id == id }) return id
        val matched = members.find {
            it.id.equals(id, ignoreCase = true) ||
            (it.name.isNotBlank() && it.name.equals(name, ignoreCase = true)) ||
            (it.isCurrentUser && (id == "usr_you" || id == "You" || name.equals("You", ignoreCase = true)))
        }
        return matched?.id ?: id
    }

    fun calculateNetBalances(
        members: List<User>,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<NetBalance> {
        val balanceMap = mutableMapOf<String, Double>()
        val memberMap = members.associateBy { it.id }.toMutableMap()

        members.forEach { balanceMap[it.id] = 0.0 }

        // Augment memberMap from expenses & settlements if any member is missing
        for (expense in expenses) {
            val pId = resolveUserId(expense.paidByUserId, expense.paidByUserName, members)
            if (!memberMap.containsKey(pId)) {
                memberMap[pId] = User(id = pId, name = expense.paidByUserName.ifBlank { "Member" }, email = "")
                balanceMap[pId] = 0.0
            }
            for (split in expense.splits) {
                val sId = resolveUserId(split.userId, split.userName, members)
                if (!memberMap.containsKey(sId)) {
                    memberMap[sId] = User(id = sId, name = split.userName.ifBlank { "Member" }, email = "")
                    balanceMap[sId] = 0.0
                }
            }
        }

        for (settlement in settlements) {
            val pId = resolveUserId(settlement.payerId, settlement.payerName, members)
            if (!memberMap.containsKey(pId)) {
                memberMap[pId] = User(id = pId, name = settlement.payerName.ifBlank { "Member" }, email = "")
                balanceMap[pId] = 0.0
            }
            val rId = resolveUserId(settlement.recipientId, settlement.recipientName, members)
            if (!memberMap.containsKey(rId)) {
                memberMap[rId] = User(id = rId, name = settlement.recipientName.ifBlank { "Member" }, email = "")
                balanceMap[rId] = 0.0
            }
        }

        // Process Expenses
        for (expense in expenses) {
            val payerId = resolveUserId(expense.paidByUserId, expense.paidByUserName, members)
            balanceMap[payerId] = (balanceMap[payerId] ?: 0.0) + expense.amount

            val effectiveSplits = if (expense.splits.isNotEmpty()) {
                expense.splits
            } else if (members.isNotEmpty()) {
                val perPersonRaw = expense.amount / members.size
                val perPerson = (perPersonRaw * 100).roundToInt() / 100.0
                val remainder = expense.amount - (perPerson * members.size)
                members.mapIndexed { index, user ->
                    val extra = if (index == 0) (remainder * 100).roundToInt() / 100.0 else 0.0
                    com.asim.splitmate.domain.model.Split(
                        userId = user.id,
                        userName = user.name,
                        amount = perPerson + extra
                    )
                }
            } else {
                emptyList()
            }

            for (split in effectiveSplits) {
                val targetId = resolveUserId(split.userId, split.userName, members)
                balanceMap[targetId] = (balanceMap[targetId] ?: 0.0) - split.amount
            }
        }

        // Process Settlements (payer pays recipient)
        for (settlement in settlements) {
            val payerId = resolveUserId(settlement.payerId, settlement.payerName, members)
            val recipientId = resolveUserId(settlement.recipientId, settlement.recipientName, members)
            balanceMap[payerId] = (balanceMap[payerId] ?: 0.0) + settlement.amount
            balanceMap[recipientId] = (balanceMap[recipientId] ?: 0.0) - settlement.amount
        }

        return balanceMap.map { (userId, rawAmount) ->
            val rounded = (rawAmount * 100).roundToInt() / 100.0
            val user = memberMap[userId]
            NetBalance(
                userId = userId,
                userName = user?.name ?: "Member",
                userAvatar = user?.avatarUrl,
                netAmount = rounded
            )
        }
    }

    /**
     * Greedy algorithm for Debt Simplification (Min-cash-flow problem)
     * Reduces N-party group debts into minimum transaction paths.
     */
    fun simplifyDebts(netBalances: List<NetBalance>): List<SimplifiedDebt> {
        val debtors = mutableListOf<Pair<NetBalance, Double>>()
        val creditors = mutableListOf<Pair<NetBalance, Double>>()

        for (balance in netBalances) {
            val amt = (balance.netAmount * 100).roundToInt() / 100.0
            if (amt < -0.01) {
                debtors.add(balance to abs(amt))
            } else if (amt > 0.01) {
                creditors.add(balance to amt)
            }
        }

        // Sort debtors and creditors descending by amount
        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val result = mutableListOf<SimplifiedDebt>()
        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            val settleAmount = min(debtor.second, creditor.second)
            val roundedSettle = (settleAmount * 100).roundToInt() / 100.0

            if (roundedSettle > 0.01) {
                result.add(
                    SimplifiedDebt(
                        fromUserId = debtor.first.userId,
                        fromUserName = debtor.first.userName,
                        toUserId = creditor.first.userId,
                        toUserName = creditor.first.userName,
                        amount = roundedSettle
                    )
                )
            }

            val newDebtorAmount = debtor.second - settleAmount
            val newCreditorAmount = creditor.second - settleAmount

            debtors[i] = debtor.first to newDebtorAmount
            creditors[j] = creditor.first to newCreditorAmount

            if (newDebtorAmount < 0.01) i++
            if (newCreditorAmount < 0.01) j++
        }

        return result
    }
}
