package com.asim.splitmate.core.utils

import com.asim.splitmate.core.common.Constants
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    fun format(amount: Double, symbol: String = Constants.DEFAULT_CURRENCY_SYMBOL): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(abs(amount))
        val sign = if (amount < 0) "-" else ""
        return "$sign$symbol$formattedNumber"
    }

    fun formatSigned(amount: Double, symbol: String = Constants.DEFAULT_CURRENCY_SYMBOL): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(abs(amount))
        return when {
            amount > 0.01 -> "+$symbol$formattedNumber"
            amount < -0.01 -> "-$symbol$formattedNumber"
            else -> "${symbol}0"
        }
    }
}
