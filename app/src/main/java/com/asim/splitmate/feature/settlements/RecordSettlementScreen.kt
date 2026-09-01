package com.asim.splitmate.feature.settlements

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSettlementScreen(
    groupId: String,
    payerId: String?,
    recipientId: String?,
    amount: Double?,
    viewModel: SettlementViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.setup(groupId, payerId, recipientId, amount)
    }

    val state by viewModel.uiState.collectAsState()
    var amountText by remember(state.suggestedAmount) {
        mutableStateOf(if (state.suggestedAmount > 0) state.suggestedAmount.toString() else "")
    }
    var selectedPaymentMethod by remember { mutableStateOf("UPI / Online") }
    var notes by remember { mutableStateOf("") }
    var payerExpanded by remember { mutableStateOf(false) }
    var recipientExpanded by remember { mutableStateOf(false) }

    val currencySymbol = state.currentGroup?.currencySymbol ?: "₹"
    val context = LocalContext.current

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            Toast.makeText(context, "Settlement payment recorded!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            ExpenseMateTopBar(
                title = "Record Settlement Payment",
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

                Text(
                    text = "Payer (Who Paid) *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded }
                ) {
                    OutlinedTextField(
                        value = state.payer?.name ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        state.currentGroup?.members?.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    viewModel.setPayer(member)
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Recipient (Who Received) *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = recipientExpanded,
                    onExpandedChange = { recipientExpanded = !recipientExpanded }
                ) {
                    OutlinedTextField(
                        value = state.recipient?.name ?: "Select Recipient",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipientExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = recipientExpanded,
                        onDismissRequest = { recipientExpanded = false }
                    ) {
                        state.currentGroup?.members?.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    viewModel.setRecipient(member)
                                    recipientExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Settlement Amount *") },
                    prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            item {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI / Online", "Cash", "Bank Transfer").forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Paid via Google Pay / Paytm") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = "Record Payment",
                    onClick = {
                        viewModel.recordSettlement(
                            groupId = groupId,
                            amountText = amountText,
                            paymentMethod = selectedPaymentMethod,
                            notes = notes
                        )
                    },
                    isLoading = state.isLoading
                )
            }
        }
    }
}
