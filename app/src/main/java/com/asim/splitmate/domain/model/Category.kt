package com.asim.splitmate.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String
) {
    companion object {
        val FOOD = Category("food", "Food & Drink", "Restaurant", "#FF6B6B")
        val TRANSPORT = Category("transport", "Transport", "DirectionsCar", "#4D96FF")
        val GROCERIES = Category("groceries", "Groceries", "ShoppingBag", "#6BCB77")
        val RENT = Category("rent", "Rent & Utilities", "Home", "#FFD93D")
        val ENTERTAINMENT = Category("entertainment", "Entertainment", "Movie", "#9B51E0")
        val SHOPPING = Category("shopping", "Shopping", "LocalMall", "#FF8E53")
        val TRAVEL = Category("travel", "Travel & Stay", "Flight", "#00B4D8")
        val OTHER = Category("other", "General / Other", "Category", "#8E9AAF")

        val ALL_CATEGORIES = listOf(
            FOOD, TRANSPORT, GROCERIES, RENT, ENTERTAINMENT, SHOPPING, TRAVEL, OTHER
        )

        fun fromId(id: String): Category {
            return ALL_CATEGORIES.find { it.id == id } ?: OTHER
        }
    }
}
