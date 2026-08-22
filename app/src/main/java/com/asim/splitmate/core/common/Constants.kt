package com.asim.splitmate.core.common

object Constants {
    const val DATABASE_NAME = "expense_mate_db"

    const val DEFAULT_CURRENCY_SYMBOL = "Rs"
    const val DEFAULT_CURRENCY_CODE = "PKR"

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("PKR", "Rs", "Pakistani Rupee"),
        CurrencyInfo("INR", "₹", "Indian Rupee"),
        CurrencyInfo("USD", "$", "US Dollar"),
        CurrencyInfo("EUR", "€", "Euro"),
        CurrencyInfo("GBP", "£", "British Pound")
    )
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)
