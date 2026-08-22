package com.asim.splitmate.feature.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.ui.components.EmptyState
import com.asim.splitmate.core.ui.components.ExpenseCard
import com.asim.splitmate.core.ui.components.ExpenseMateBottomBar
import com.asim.splitmate.core.ui.components.GroupCard
import com.asim.splitmate.core.ui.theme.CoralOwe
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.core.ui.theme.GreenOwed
import com.asim.splitmate.core.utils.CurrencyFormatter

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.feature.groups.GroupViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    groupViewModel: GroupViewModel = koinViewModel(),
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToEditGroup: (String) -> Unit,
    onNavigateToAddExpense: (String?) -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var groupToDelete by remember { mutableStateOf<Group?>(null) }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete '${groupToDelete?.name}'? All associated expenses will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val gId = groupToDelete?.id
                        groupToDelete = null
                        if (gId != null) {
                            groupViewModel.deleteGroup(gId)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            ExpenseMateBottomBar(currentRoute = "dashboard", onNavigate = onNavigateTab)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddExpense(null) },
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val summary = state.summary

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hello, ${state.userName} 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Here's your financial summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                item {
                    // Balance Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Net Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val net = summary?.netOverallBalance ?: 0.0
                            Text(
                                text = CurrencyFormatter.formatSigned(net),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (net >= 0) EmeraldPrimary else CoralOwe
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BalanceSubItem(
                                    label = "You Owe",
                                    amount = summary?.totalYouOwe ?: 0.0,
                                    color = CoralOwe,
                                    icon = Icons.Filled.ArrowDownward
                                )

                                BalanceSubItem(
                                    label = "You are Owed",
                                    amount = summary?.totalYouAreOwed ?: 0.0,
                                    color = GreenOwed,
                                    icon = Icons.Filled.ArrowUpward
                                )
                            }
                        }
                    }
                }

                item {
                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Filled.Add,
                            label = "Add Expense",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToAddExpense(null) }
                        )

                        QuickActionButton(
                            icon = Icons.Filled.GroupAdd,
                            label = "New Group",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCreateGroup
                        )
                    }
                }

                item {
                    // Groups Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Groups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = onNavigateToCreateGroup) {
                            Text("+ Group")
                        }
                    }
                }

                if (summary?.groups.isNullOrEmpty()) {
                    item {
                        EmptyState(
                            title = "No Active Groups",
                            description = "Create a group for your trip, house, or friends to start splitting expenses.",
                            actionText = "Create Group",
                            onActionClick = onNavigateToCreateGroup
                        )
                    }
                } else {
                    items(summary?.groups ?: emptyList()) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onNavigateToGroup(group.id) },
                            onEditClick = { onNavigateToEditGroup(group.id) },
                            onDeleteClick = { groupToDelete = group }
                        )
                    }
                }

                item {
                    // Recent Activity Section
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (summary?.recentExpenses.isNullOrEmpty()) {
                    item {
                        EmptyState(
                            title = "No Recent Expenses",
                            description = "Add an expense inside a group to track balances automatically."
                        )
                    }
                } else {
                    items(summary?.recentExpenses ?: emptyList()) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onClick = { onNavigateToExpenseDetail(expense.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun BalanceSubItem(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Icon(icon, contentDescription = label, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
