package com.asim.splitmate.core.utils

import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import kotlin.math.roundToInt

object SplitCalculator {

    fun calculateSplits(
        totalAmount: Double,
        splitType: SplitType,
        selectedMembers: List<User>,
        customSplits: List<Split> = emptyList()
    ): List<Split> {
        if (selectedMembers.isEmpty() || totalAmount <= 0.0) return emptyList()

        return when (splitType) {
            SplitType.EQUAL -> {
                val count = selectedMembers.size
                val perPersonRaw = totalAmount / count
                val perPerson = (perPersonRaw * 100).roundToInt() / 100.0
                var remainder = totalAmount - (perPerson * count)

                selectedMembers.mapIndexed { index, user ->
                    val extra = if (index == 0) (remainder * 100).roundToInt() / 100.0 else 0.0
                    Split(
                        userId = user.id,
                        userName = user.name,
                        amount = perPerson + extra,
                        percentage = 100.0 / count,
                        shares = 1
                    )
                }
            }

            SplitType.EXACT -> {
                selectedMembers.map { user ->
                    val custom = customSplits.find { it.userId == user.id }
                    Split(
                        userId = user.id,
                        userName = user.name,
                        amount = custom?.amount ?: 0.0,
                        percentage = if (totalAmount > 0) ((custom?.amount ?: 0.0) / totalAmount) * 100 else 0.0,
                        shares = 1
                    )
                }
            }

            SplitType.PERCENTAGE -> {
                selectedMembers.map { user ->
                    val custom = customSplits.find { it.userId == user.id }
                    val pct = custom?.percentage ?: 0.0
                    val amt = (totalAmount * (pct / 100.0) * 100).roundToInt() / 100.0
                    Split(
                        userId = user.id,
                        userName = user.name,
                        amount = amt,
                        percentage = pct,
                        shares = 1
                    )
                }
            }

            SplitType.SHARES -> {
                val totalShares = selectedMembers.sumOf { user ->
                    customSplits.find { it.userId == user.id }?.shares ?: 1
                }.coerceAtLeast(1)

                selectedMembers.map { user ->
                    val shareCount = customSplits.find { it.userId == user.id }?.shares ?: 1
                    val amt = (totalAmount * shareCount.toDouble() / totalShares * 100).roundToInt() / 100.0
                    Split(
                        userId = user.id,
                        userName = user.name,
                        amount = amt,
                        percentage = (shareCount.toDouble() / totalShares) * 100.0,
                        shares = shareCount
                    )
                }
            }
        }
    }
}
