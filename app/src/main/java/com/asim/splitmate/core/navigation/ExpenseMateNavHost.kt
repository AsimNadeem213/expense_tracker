package com.asim.splitmate.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.asim.splitmate.feature.auth.AuthViewModel
import com.asim.splitmate.feature.auth.LoginScreen
import com.asim.splitmate.feature.auth.RegisterScreen
import com.asim.splitmate.feature.balances.BalanceSummaryScreen
import com.asim.splitmate.feature.balances.BalancesViewModel
import com.asim.splitmate.feature.dashboard.DashboardViewModel
import com.asim.splitmate.feature.dashboard.HomeScreen
import com.asim.splitmate.feature.expenses.AddEditExpenseScreen
import com.asim.splitmate.feature.expenses.ExpenseDetailScreen
import com.asim.splitmate.feature.expenses.ExpenseViewModel
import com.asim.splitmate.feature.groups.CreateGroupScreen
import com.asim.splitmate.feature.groups.GroupDetailScreen
import com.asim.splitmate.feature.groups.GroupListScreen
import com.asim.splitmate.feature.groups.GroupViewModel
import com.asim.splitmate.feature.profile.ProfileScreen
import com.asim.splitmate.feature.profile.ProfileViewModel
import com.asim.splitmate.feature.settlements.RecordSettlementScreen
import com.asim.splitmate.feature.settlements.SettlementViewModel
import com.asim.splitmate.feature.qr.QrScannerScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpenseMateNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = koinViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = koinViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = koinViewModel()
            HomeScreen(
                viewModel = dashboardViewModel,
                onNavigateToGroup = { groupId -> navController.navigate(Screen.GroupDetail.createRoute(groupId)) },
                onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                onNavigateToEditGroup = { groupId -> navController.navigate(Screen.EditGroup.createRoute(groupId)) },
                onNavigateToAddExpense = { groupId -> navController.navigate(Screen.AddExpense.createRoute(groupId)) },
                onNavigateToExpenseDetail = { expId -> navController.navigate(Screen.ExpenseDetail.createRoute(expId)) },
                onNavigateTab = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Groups.route) {
            val groupViewModel: GroupViewModel = koinViewModel()
            GroupListScreen(
                viewModel = groupViewModel,
                onNavigateToGroupDetail = { groupId -> navController.navigate(Screen.GroupDetail.createRoute(groupId)) },
                onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                onNavigateToEditGroup = { groupId -> navController.navigate(Screen.EditGroup.createRoute(groupId)) },
                onNavigateToQrScanner = { navController.navigate(Screen.QrScanner.route) },
                onNavigateTab = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable(Screen.CreateGroup.route) {
            val groupViewModel: GroupViewModel = koinViewModel()
            CreateGroupScreen(
                groupId = null,
                viewModel = groupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditGroup.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            val groupViewModel: GroupViewModel = koinViewModel()
            CreateGroupScreen(
                groupId = groupId,
                viewModel = groupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupViewModel: GroupViewModel = koinViewModel()
            GroupDetailScreen(
                groupId = groupId,
                viewModel = groupViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { gId -> navController.navigate(Screen.AddExpense.createRoute(gId)) },
                onNavigateToRecordSettlement = { gId, pId, rId, amt ->
                    navController.navigate(Screen.RecordSettlement.createRoute(gId, pId, rId, amt))
                },
                onNavigateToExpenseDetail = { expId -> navController.navigate(Screen.ExpenseDetail.createRoute(expId)) },
                onNavigateToEditGroup = { gId -> navController.navigate(Screen.EditGroup.createRoute(gId)) }
            )
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(navArgument("groupId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            val expenseViewModel: ExpenseViewModel = koinViewModel()
            AddEditExpenseScreen(
                groupId = groupId,
                viewModel = expenseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val expenseViewModel: ExpenseViewModel = koinViewModel()
            AddEditExpenseScreen(
                groupId = null,
                expenseId = expenseId,
                viewModel = expenseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            val expenseViewModel: ExpenseViewModel = koinViewModel()
            ExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = expenseViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditExpense = { expId -> navController.navigate(Screen.EditExpense.createRoute(expId)) }
            )
        }

        composable(Screen.Balances.route) {
            val balancesViewModel: BalancesViewModel = koinViewModel()
            BalanceSummaryScreen(
                viewModel = balancesViewModel,
                onNavigateToRecordSettlement = { gId, pId, rId, amt ->
                    navController.navigate(Screen.RecordSettlement.createRoute(gId, pId, rId, amt))
                },
                onNavigateTab = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable(
            route = Screen.RecordSettlement.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("payerId") { type = NavType.StringType; nullable = true },
                navArgument("recipientId") { type = NavType.StringType; nullable = true },
                navArgument("amount") { type = NavType.FloatType; defaultValue = 0f }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val payerId = backStackEntry.arguments?.getString("payerId")
            val recipientId = backStackEntry.arguments?.getString("recipientId")
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble()
            val settlementViewModel: SettlementViewModel = koinViewModel()

            RecordSettlementScreen(
                groupId = groupId,
                payerId = payerId,
                recipientId = recipientId,
                amount = amount,
                viewModel = settlementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = koinViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onLogout = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                onNavigateTab = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable(Screen.QrScanner.route) {
            val groupViewModel: GroupViewModel = koinViewModel()
            QrScannerScreen(
                onQrCodeScanned = { code ->
                    groupViewModel.joinGroup(code)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                onEnterCodeManually = { navController.popBackStack() }
            )
        }
    }
}
