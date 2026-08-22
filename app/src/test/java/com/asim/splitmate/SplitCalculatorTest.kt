package com.asim.splitmate

import com.asim.splitmate.core.utils.SplitCalculator
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun testEqualSplit_dividesEquallyWithoutRemainderLoss() {
        val members = listOf(
            User("1", "Alice", "a@test.com"),
            User("2", "Bob", "b@test.com"),
            User("3", "Charlie", "c@test.com")
        )

        val splits = SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.EQUAL,
            selectedMembers = members
        )

        assertEquals(3, splits.size)
        val totalSplit = splits.sumOf { it.amount }
        assertEquals(100.0, totalSplit, 0.01)
    }
}
