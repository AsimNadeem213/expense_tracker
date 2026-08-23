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
import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }

    val showDatePicker = {
        calendar.timeInMillis = state.selectedDate
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, y, m, d ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                viewModel.setSelectedDate(selectedCal.timeInMillis)
            },
            year,
            month,
            day
        ).show()
    }

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

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFormatter.format(Date(state.selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Expense Date *") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = "Select Date",
                                    tint = EmeraldPrimary
                                )
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = EmeraldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker() }
                    )
                }
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
                    text = "Split Among Participants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap members to include or exclude them from this expense:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(group?.members ?: emptyList()) { member ->
                        val isSelected = state.selectedMembers.any { it.id == member.id }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleMemberParticipant(member) },
                            label = { Text(member.name) }
                        )
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
                val totalAmount = amountText.toDoubleOrNull() ?: 0.0
                val selectedMembers = state.selectedMembers

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split Allocation Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (state.splitType) {
                    SplitType.EQUAL -> {
                        val count = selectedMembers.size.coerceAtLeast(1)
                        val perPerson = if (totalAmount > 0) totalAmount / count else 0.0
                        selectedMembers.forEach { member ->
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

                    SplitType.EXACT -> {
                        var sumAllocated = 0.0
                        selectedMembers.forEach { member ->
                            val custom = state.customSplits.find { it.userId == member.id }
                            val currentAmt = custom?.amount ?: 0.0
                            sumAllocated += currentAmt

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
                                    OutlinedTextField(
                                        value = if (currentAmt > 0) currentAmt.toString() else "",
                                        onValueChange = { input ->
                                            val newVal = input.toDoubleOrNull() ?: 0.0
                                            viewModel.updateCustomSplitAmount(member.id, member.name, newVal)
                                        },
                                        placeholder = { Text("0.00") },
                                        prefix = { Text(currencySymbol) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                            }
                        }

                        val isMatched = kotlin.math.abs(sumAllocated - totalAmount) < 0.01
                        Text(
                            text = "Allocated: ${CurrencyFormatter.format(sumAllocated, currencySymbol)} / Total: ${CurrencyFormatter.format(totalAmount, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatched) EmeraldPrimary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    SplitType.PERCENTAGE -> {
                        var sumPct = 0.0
                        selectedMembers.forEach { member ->
                            val custom = state.customSplits.find { it.userId == member.id }
                            val currentPct = custom?.percentage ?: 0.0
                            sumPct += currentPct
                            val calculatedAmt = (totalAmount * currentPct) / 100.0

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(calculatedAmt, currencySymbol),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary
                                        )
                                    }
                                    OutlinedTextField(
                                        value = if (currentPct > 0) currentPct.toString() else "",
                                        onValueChange = { input ->
                                            val newVal = input.toDoubleOrNull() ?: 0.0
                                            viewModel.updateCustomSplitPercentage(member.id, member.name, newVal)
                                        },
                                        placeholder = { Text("0") },
                                        suffix = { Text("%") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }

                        val isMatched = kotlin.math.abs(sumPct - 100.0) < 0.1
                        Text(
                            text = "Total Percentage: ${String.format("%.1f", sumPct)}% / 100%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatched) EmeraldPrimary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    SplitType.SHARES -> {
                        val totalShares = selectedMembers.sumOf { m ->
                            state.customSplits.find { it.userId == m.id }?.shares ?: 1
                        }.coerceAtLeast(1)

                        selectedMembers.forEach { member ->
                            val custom = state.customSplits.find { it.userId == member.id }
                            val currentShare = custom?.shares ?: 1
                            val calculatedAmt = (totalAmount * currentShare) / totalShares.toDouble()

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(calculatedAmt, currencySymbol),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary
                                        )
                                    }
                                    OutlinedTextField(
                                        value = currentShare.toString(),
                                        onValueChange = { input ->
                                            val newVal = input.toIntOrNull() ?: 1
                                            viewModel.updateCustomSplitShares(member.id, member.name, newVal)
                                        },
                                        placeholder = { Text("1") },
                                        suffix = { Text("share(s)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                            }
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
