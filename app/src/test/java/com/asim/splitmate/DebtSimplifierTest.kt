package com.asim.splitmate

import com.asim.splitmate.core.utils.DebtSimplifier
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.NetBalance
import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtSimplifierTest {

    @Test
    fun testTwoUserEqualSplit_calculatesCorrectOwedAmount() {
        val user1 = User("1", "You", "you@example.com", isCurrentUser = true)
        val user2 = User("2", "Ali", "ali@example.com")
        val members = listOf(user1, user2)

        val expense = Expense(
            id = "e1",
            groupId = "g1",
            title = "Dinner",
            amount = 2000.0,
            paidByUserId = "1",
            paidByUserName = "You",
            splits = listOf(
                Split("1", "You", 1000.0),
                Split("2", "Ali", 1000.0)
            )
        )

        val balances = DebtSimplifier.calculateNetBalances(members, listOf(expense), emptyList())
        val simplifiedDebts = DebtSimplifier.simplifyDebts(balances)

        assertEquals(2, balances.size)

        val youBalance = balances.find { it.userId == "1" }?.netAmount
        val aliBalance = balances.find { it.userId == "2" }?.netAmount

        assertEquals(1000.0, youBalance!!, 0.01)
        assertEquals(-1000.0, aliBalance!!, 0.01)

        assertEquals(1, simplifiedDebts.size)
        val debt = simplifiedDebts.first()
        assertEquals("2", debt.fromUserId)
        assertEquals("1", debt.toUserId)
        assertEquals(1000.0, debt.amount, 0.01)
    }

    @Test
    fun testThreeUserCircularDebts_simplifiesToMinimumTransactions() {
        val u1 = User("1", "A", "a@example.com")
        val u2 = User("2", "B", "b@example.com")
        val u3 = User("3", "C", "c@example.com")
        val members = listOf(u1, u2, u3)

        val balances = listOf(
            NetBalance("1", "A", null, 100.0),
            NetBalance("2", "B", null, 100.0),
            NetBalance("3", "C", null, -200.0)
        )

        val simplified = DebtSimplifier.simplifyDebts(balances)

        assertEquals(2, simplified.size)
        val totalSimplifiedAmount = simplified.sumOf { it.amount }
        assertEquals(200.0, totalSimplifiedAmount, 0.01)
    }
}
