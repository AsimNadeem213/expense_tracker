package com.asim.splitmate.feature.balances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.ui.components.DebtFlowCard
import com.asim.splitmate.core.ui.components.EmptyState
import com.asim.splitmate.core.ui.components.ExpenseMateBottomBar
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.domain.model.SimplifiedDebt

import androidx.activity.compose.BackHandler

@Composable
fun BalanceSummaryScreen(
    viewModel: BalancesViewModel,
    onNavigateToRecordSettlement: (String, String, String, Double) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    BackHandler {
        onNavigateTab("dashboard")
    }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ExpenseMateTopBar(title = "Balances & Settlement")
        },
        bottomBar = {
            ExpenseMateBottomBar(currentRoute = "balances", onNavigate = onNavigateTab)
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
        } else if (state.groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyState(
                    title = "No Groups Found",
                    description = "Join or create a group to view optimized balance settlements."
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Group selector chip row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.groups) { grp ->
                        FilterChip(
                            selected = state.selectedGroup?.id == grp.id,
                            onClick = { viewModel.selectGroup(grp.id) },
                            label = { Text(grp.name) }
                        )
                    }
                }

                val grp = state.selectedGroup
                val res = state.balancesResult
                val debts = res?.simplifiedDebts ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Optimized Settle Up Plan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Net debts simplified to minimize transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
                                        text = "✨ All debts settled up in ${grp?.name ?: "this group"}!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
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
                                currencySymbol = grp?.currencySymbol ?: "₹",
                                onSettleClick = {
                                    if (grp != null) {
                                        onNavigateToRecordSettlement(grp.id, debt.fromUserId, debt.toUserId, debt.amount)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
