package com.asim.splitmate.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.utils.CurrencyFormatter
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.GroupType

@Composable
fun GroupCard(
    group: Group,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (icon, typeLabel) = remember(group.type) {
        when (group.type) {
            GroupType.TRIP -> Icons.Filled.BeachAccess to "Trip"
            GroupType.HOME -> Icons.Filled.HomeWork to "Home"
            GroupType.COUPLE -> Icons.Filled.Favorite to "Couple"
            GroupType.OTHER -> Icons.Filled.Group to "Other"
        }
    }

    val membersText = remember(group.members.size) { "${group.members.size} members" }
    val formattedSpent = remember(group.totalExpense, group.currencySymbol) {
        CurrencyFormatter.format(group.totalExpense, group.currencySymbol)
    }
    val badgeBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Group Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Group Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = membersText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBgColor)
                            .padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            )
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Total Spent
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedSpent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            // More menu
            if (onEditClick != null || onDeleteClick != null) {

                Box {
                    IconButton(
                        onClick = {
                            menuExpanded = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Group options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {

                        onEditClick?.let {
                            DropdownMenuItem(
                                text = {
                                    Text("Edit Group")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    it()
                                }
                            )
                        }

                        onDeleteClick?.let {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete Group",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    it()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}