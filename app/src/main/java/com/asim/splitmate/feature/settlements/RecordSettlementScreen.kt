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
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.components.PrimaryButton

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
                    text = "${state.payer?.name ?: "Payer"} pays ${state.recipient?.name ?: "Recipient"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

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
