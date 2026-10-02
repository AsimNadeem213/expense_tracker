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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.asim.splitmate.core.ui.theme.CoralOwe
import com.asim.splitmate.core.ui.theme.CoralOweContainer
import com.asim.splitmate.core.ui.theme.CoralOweContainerDark
import com.asim.splitmate.core.ui.theme.CoralOweDark
import com.asim.splitmate.core.ui.theme.GreenOwed
import com.asim.splitmate.core.ui.theme.GreenOwedContainer
import com.asim.splitmate.core.ui.theme.GreenOwedContainerDark
import com.asim.splitmate.core.ui.theme.GreenOwedDark
import com.asim.splitmate.core.utils.CurrencyFormatter
import com.asim.splitmate.domain.model.SimplifiedDebt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface

@Composable
fun DebtFlowCard(
    debt: SimplifiedDebt,
    currentUserId: String,
    currencySymbol: String = "₹",
    onSettleClick: (SimplifiedDebt) -> Unit = {}
) {
    val isUserDebtor = remember(debt.fromUserId, debt.fromUserName, currentUserId) {
        debt.fromUserId == currentUserId || debt.fromUserId == "usr_you" || debt.fromUserName.equals("You", ignoreCase = true)
    }
    val isUserCreditor = remember(debt.toUserId, debt.toUserName, currentUserId) {
        debt.toUserId == currentUserId || debt.toUserId == "usr_you" || debt.toUserName.equals("You", ignoreCase = true)
    }

    val isDark = isSystemInDarkTheme()
    val defaultSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val containerColor = remember(isUserDebtor, isUserCreditor, defaultSurfaceVariant, isDark) {
        when {
            isUserDebtor -> if (isDark) CoralOweContainerDark else CoralOweContainer.copy(alpha = 0.6f)
            isUserCreditor -> if (isDark) GreenOwedContainerDark else GreenOwedContainer.copy(alpha = 0.6f)
            else -> defaultSurfaceVariant.copy(alpha = if (isDark) 0.5f else 0.6f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = remember(isUserDebtor, isUserCreditor, isDark, primaryColor) {
        when {
            isUserDebtor -> if (isDark) CoralOweDark else CoralOwe
            isUserCreditor -> if (isDark) GreenOwedDark else GreenOwed
            else -> primaryColor
        }
    }

    val borderColor = remember(accentColor, isDark) { accentColor.copy(alpha = if (isDark) 0.35f else 0.25f) }
    val accentAlpha15 = remember(accentColor, isDark) { accentColor.copy(alpha = if (isDark) 0.22f else 0.15f) }
    val accentAlpha20 = remember(accentColor, isDark) { accentColor.copy(alpha = if (isDark) 0.30f else 0.2f) }

    val fromNameText = remember(isUserDebtor, debt.fromUserName) { if (isUserDebtor) "You" else debt.fromUserName }
    val toNameText = remember(isUserCreditor, debt.toUserName) { if (isUserCreditor) "You" else debt.toUserName }
    val labelText = remember(isUserDebtor, isUserCreditor, debt.fromUserName, debt.toUserName) {
        when {
            isUserDebtor -> "You owe ${debt.toUserName}"
            isUserCreditor -> "${debt.fromUserName} owes you"
            else -> "${debt.fromUserName} owes ${debt.toUserName}"
        }
    }

    val formattedAmount = remember(debt.amount, currencySymbol) { CurrencyFormatter.format(debt.amount, currencySymbol) }
    val subtitleTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentAlpha15,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = fromNameText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentAlpha20),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "owes",
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = accentAlpha15,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = toNameText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleTextColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onSettleClick(debt) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF0F172A) else androidx.compose.ui.graphics.Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(text = "Settle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
