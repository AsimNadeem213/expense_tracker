package com.asim.splitmate.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Groups : Screen("groups")
    object CreateGroup : Screen("create_group")
    object EditGroup : Screen("edit_group/{groupId}") {
        fun createRoute(groupId: String) = "edit_group/$groupId"
    }
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object AddExpense : Screen("add_expense?groupId={groupId}") {
        fun createRoute(groupId: String? = null) = if (groupId != null) "add_expense?groupId=$groupId" else "add_expense"
    }
    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
    object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }
    object Balances : Screen("balances")
    object RecordSettlement : Screen("record_settlement?groupId={groupId}&payerId={payerId}&recipientId={recipientId}&amount={amount}") {
        fun createRoute(groupId: String, payerId: String? = null, recipientId: String? = null, amount: Double? = null): String {
            val p = payerId ?: ""
            val r = recipientId ?: ""
            val a = amount ?: 0.0
            return "record_settlement?groupId=$groupId&payerId=$p&recipientId=$r&amount=$a"
        }
    }
    object Profile : Screen("profile")
    object QrScanner : Screen("qr_scanner")
}
