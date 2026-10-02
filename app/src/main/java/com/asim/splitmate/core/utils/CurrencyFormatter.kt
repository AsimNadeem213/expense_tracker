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
        val cleanSymbol = symbol.trim()
        val spacing = if (cleanSymbol.isNotEmpty() && cleanSymbol.last().isLetter()) " " else ""
        return "$sign$cleanSymbol$spacing$formattedNumber"
    }

    fun formatSigned(amount: Double, symbol: String = Constants.DEFAULT_CURRENCY_SYMBOL): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(abs(amount))
        val cleanSymbol = symbol.trim()
        val spacing = if (cleanSymbol.isNotEmpty() && cleanSymbol.last().isLetter()) " " else ""
        return when {
            amount > 0.01 -> "+$cleanSymbol$spacing$formattedNumber"
            amount < -0.01 -> "-$cleanSymbol$spacing$formattedNumber"
            else -> "$cleanSymbol${spacing}0"
        }
    }
}
