package com.asim.splitmate.feature.expenses

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.common.Constants
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.components.PrimaryButton
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.core.utils.CurrencyFormatter
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.SplitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    groupId: String?,
    expenseId: String? = null,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isEditMode = !expenseId.isNullOrBlank()

    LaunchedEffect(groupId, expenseId) {
        if (isEditMode && expenseId != null) {
            viewModel.loadExpenseDetail(expenseId)
        } else {
            viewModel.setupForGroup(groupId)
        }
    }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.FOOD) }
    var notes by remember { mutableStateOf("") }
    var paidByExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentExpense) {
        val exp = state.currentExpense
        if (isEditMode && exp != null) {
            title = exp.title
            amountText = if (exp.amount > 0) exp.amount.toString() else ""
            selectedCategory = exp.category
            notes = exp.notes
            viewModel.setupForGroup(exp.groupId)
        }
    }

    val group = state.currentGroup
    val currencySymbol = group?.currencySymbol ?: Constants.DEFAULT_CURRENCY_SYMBOL

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            val msg = if (isEditMode) "Expense updated successfully!" else "Expense added successfully!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onNavigateBack()
            viewModel.resetState()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            Toast.makeText(context, state.error!!, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            ExpenseMateTopBar(
                title = if (isEditMode) "Edit Expense" else "Add Expense",
                canNavigateBack = true,
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!isEditMode && groupId.isNullOrBlank() && state.availableGroups.isNotEmpty()) {
                    Text(
                        text = "Group *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = groupExpanded,
                        onExpandedChange = { groupExpanded = !groupExpanded }
                    ) {
                        OutlinedTextField(
                            value = group?.name ?: "Select Group",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false }
                        ) {
                            state.availableGroups.forEach { grp ->
                                DropdownMenuItem(
                                    text = { Text(grp.name) },
                                    onClick = {
                                        viewModel.selectGroup(grp)
                                        groupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / Title *") },
                    placeholder = { Text("e.g. Dinner, Taxi, Groceries") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount *") },
                    prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Category.ALL_CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory.id == cat.id,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Paid By",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = !paidByExpanded }
                ) {
                    OutlinedTextField(
                        value = state.selectedPaidByUser?.name ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        group?.members?.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    viewModel.setPaidByUser(member)
                                    paidByExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Split Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = state.splitType.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SplitType.values().forEach { type ->
                        Tab(
                            selected = state.splitType == type,
                            onClick = { viewModel.setSplitType(type) },
                            text = { Text(type.name) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Split Allocation Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val count = state.selectedMembers.size.coerceAtLeast(1)
                val perPerson = if (amount > 0) amount / count else 0.0

                state.selectedMembers.forEach { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyFormatter.format(perPerson, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = if (isEditMode) "Update Expense" else "Save Expense",
                    onClick = {
                        viewModel.addExpense(
                            groupId = groupId ?: group?.id,
                            title = title,
                            amountText = amountText,
                            category = selectedCategory,
                            notes = notes
                        )
                    },
                    isLoading = state.isLoading
                )
            }
        }
    }
}
