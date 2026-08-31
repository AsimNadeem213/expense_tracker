package com.asim.splitmate.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.ui.components.DebtFlowCard
import com.asim.splitmate.core.ui.components.EmptyState
import com.asim.splitmate.core.ui.components.ExpenseCard
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.theme.CoralOwe
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.core.ui.theme.GreenOwed
import com.asim.splitmate.core.utils.CurrencyFormatter
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.SimplifiedDebt
import com.asim.splitmate.domain.usecase.GroupBalancesResult

import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.foundation.clickable
import com.asim.splitmate.core.ui.components.GroupQrDialog

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToRecordSettlement: (String, String?, String?, Double?) -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToEditGroup: (String) -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.selectGroup(groupId)
    }

    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }

    val group = state.currentGroup
    val balancesRes = state.balancesResult

    LaunchedEffect(state.groupDeletedSuccess) {
        if (state.groupDeletedSuccess) {
            onNavigateBack()
            viewModel.resetState()
        }
    }

    if (showQrDialog && group != null) {
        GroupQrDialog(
            group = group,
            onDismissRequest = { showQrDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete '${group?.name ?: "this group"}'? All associated expenses will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGroup(groupId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val currentUserId = state.currentUserId
    val isGroupCreator = if (group == null || group.createdBy.isBlank()) false else when {
        currentUserId.isNotBlank() && group.createdBy == currentUserId -> true
        else -> {
            val currentMember = group.members.find { it.isCurrentUser || (currentUserId.isNotBlank() && it.id == currentUserId) }
            currentMember != null && currentMember.id == group.createdBy
        }
    }

    Scaffold(
        topBar = {
            ExpenseMateTopBar(
                title = group?.name ?: "Group Details",
                canNavigateBack = true,
                onBackClick = onNavigateBack,
                actions = {
                    if (group != null) {
                        IconButton(onClick = { showQrDialog = true }) {
                            Icon(Icons.Filled.QrCode2, contentDescription = "Show Group QR Code")
                        }
                        if (isGroupCreator) {
                            Box {
                                IconButton(onClick = { showActionMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showActionMenu,
                                    onDismissRequest = { showActionMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Group") },
                                        onClick = {
                                            showActionMenu = false
                                            onNavigateToEditGroup(group.id)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit Group"
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete Group",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showActionMenu = false
                                            showDeleteDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Group",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },

        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { onNavigateToAddExpense(groupId) },
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                }
            }
        }

    ) { padding ->
        if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Group Summary Banner Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (group.description.isNotBlank()) {
                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showQrDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = "Show QR Code",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Code: ${group.inviteCode} • Tap for QR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = CurrencyFormatter.format(group.totalExpense, group.currencySymbol),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                // Tabs: Expenses | Balances | Analytics
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Expenses") },
                        icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Settlements") },
                        icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Analytics") },
                        icon = { Icon(Icons.Filled.Analytics, contentDescription = null) }
                    )
                }

                when (selectedTabIndex) {
                    0 -> ExpensesTab(
                        groupId = groupId,
                        currencySymbol = group.currencySymbol,
                        expenses = state.groupExpenses,
                        onNavigateToExpenseDetail = onNavigateToExpenseDetail,
                        onNavigateToAddExpense = { onNavigateToAddExpense(groupId) }
                    )
                    1 -> BalancesTab(
                        groupId = groupId,
                        currencySymbol = group.currencySymbol,
                        balancesResult = balancesRes,
                        onSettleClick = { debt ->
                            onNavigateToRecordSettlement(groupId, debt.fromUserId, debt.toUserId, debt.amount)
                        }
                    )
                    2 -> GroupStatsScreen(group = group, expenses = state.groupExpenses)
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    groupId: String,
    currencySymbol: String,
    expenses: List<Expense>,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToAddExpense: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Group Expenses (${expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onNavigateToAddExpense) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense")
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    title = "No Expenses Yet",
                    description = "Tap 'Add Expense' above to record the first expense in this group.",
                    actionText = "Add Expense",
                    onActionClick = onNavigateToAddExpense
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    currencySymbol = currencySymbol,
                    onClick = { onNavigateToExpenseDetail(expense.id) }
                )
            }
        }
    }
}

@Composable
private fun BalancesTab(
    groupId: String,
    currencySymbol: String,
    balancesResult: GroupBalancesResult?,
    onSettleClick: (SimplifiedDebt) -> Unit
) {
    val debts = balancesResult?.simplifiedDebts ?: emptyList()
    val netBalances = balancesResult?.netBalances ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Optimized Debt Settlement Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Automatically simplified to minimum required payments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (debts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉 Everyone is settled up! Zero net debts in this group.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        } else {
            items(debts) { debt ->
                DebtFlowCard(
                    debt = debt,
                    currentUserId = "usr_you",
                    currencySymbol = currencySymbol,
                    onSettleClick = onSettleClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Individual Net Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(netBalances) { balance ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = balance.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = balance.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    val color = when {
                        balance.netAmount > 0.01 -> GreenOwed
                        balance.netAmount < -0.01 -> CoralOwe
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }

                    Text(
                        text = CurrencyFormatter.formatSigned(balance.netAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}
