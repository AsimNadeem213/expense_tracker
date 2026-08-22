package com.asim.splitmate.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.ui.theme.CoralOwe
import com.asim.splitmate.core.ui.theme.CoralOweContainer
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.core.ui.theme.GreenOwed
import com.asim.splitmate.core.ui.theme.GreenOwedContainer
import com.asim.splitmate.core.utils.CurrencyFormatter
import com.asim.splitmate.domain.model.SimplifiedDebt

@Composable
fun DebtFlowCard(
    debt: SimplifiedDebt,
    currentUserId: String,
    currencySymbol: String = "₹",
    onSettleClick: (SimplifiedDebt) -> Unit = {}
) {
    val isUserDebtor = debt.fromUserId == currentUserId
    val isUserCreditor = debt.toUserId == currentUserId

    val containerColor = when {
        isUserDebtor -> CoralOweContainer
        isUserCreditor -> GreenOwedContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val accentColor = when {
        isUserDebtor -> CoralOwe
        isUserCreditor -> GreenOwed
        else -> EmeraldPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUserDebtor) "You" else debt.fromUserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "owes",
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUserCreditor) "You" else debt.toUserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val label = when {
                    isUserDebtor -> "You owe ${debt.toUserName}"
                    isUserCreditor -> "${debt.fromUserName} owes you"
                    else -> "${debt.fromUserName} owes ${debt.toUserName}"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(debt.amount, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onSettleClick(debt) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(text = "Settle", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
