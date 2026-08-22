package com.asim.splitmate.core.utils

import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement

object CsvExportHelper {

    fun generateGroupCsv(group: Group, expenses: List<Expense>, settlements: List<Settlement>): String {
        val sb = StringBuilder()
        sb.append("Group Name,${group.name}\n")
        sb.append("Export Date,${DateFormatter.formatDate(System.currentTimeMillis())}\n\n")

        sb.append("--- EXPENSES ---\n")
        sb.append("Date,Title,Category,Paid By,Amount (${group.currencySymbol}),Splits\n")

        for (exp in expenses) {
            val splitsSummary = exp.splits.joinToString(" | ") { "${it.userName}: ${it.amount}" }
            sb.append("${DateFormatter.formatDate(exp.date)},\"${exp.title}\",${exp.category.name},\"${exp.paidByUserName}\",${exp.amount},\"$splitsSummary\"\n")
        }

        sb.append("\n--- SETTLEMENTS ---\n")
        sb.append("Date,Payer,Recipient,Amount (${group.currencySymbol}),Method\n")

        for (set in settlements) {
            sb.append("${DateFormatter.formatDate(set.date)},\"${set.payerName}\",\"${set.recipientName}\",${set.amount},\"${set.paymentMethod}\"\n")
        }

        return sb.toString()
    }
}
