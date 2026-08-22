package com.asim.splitmate.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.common.Constants
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.components.PrimaryButton
import com.asim.splitmate.domain.model.GroupType

@Composable
fun CreateGroupScreen(
    groupId: String? = null,
    viewModel: GroupViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isEditMode = !groupId.isNullOrBlank()

    LaunchedEffect(groupId) {
        if (isEditMode && groupId != null) {
            viewModel.selectGroup(groupId)
        }
    }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GroupType.TRIP) }
    var selectedCurrency by remember { mutableStateOf(Constants.SUPPORTED_CURRENCIES.first()) }
    val memberNames = remember { mutableStateListOf<String>() }

    var isInitialized by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentGroup, isEditMode) {
        if (isEditMode && state.currentGroup != null && !isInitialized) {
            val group = state.currentGroup!!
            name = group.name
            description = group.description
            selectedType = group.type
            selectedCurrency = Constants.SUPPORTED_CURRENCIES.find { it.symbol == group.currencySymbol }
                ?: Constants.SUPPORTED_CURRENCIES.first()

            memberNames.clear()
            val otherMembers = group.members.filter { !it.isCurrentUser }.map { it.name }
            if (otherMembers.isEmpty()) {
                memberNames.addAll(listOf("Ali", "Sarah"))
            } else {
                memberNames.addAll(otherMembers)
            }
            isInitialized = true
        } else if (!isEditMode && !isInitialized) {
            memberNames.addAll(listOf("Ali", "Sarah"))
            isInitialized = true
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.groupCreatedSuccess) {
        if (state.groupCreatedSuccess) {
            val msg = if (isEditMode) "Group updated successfully!" else "Group created successfully!"
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
            viewModel.resetState()
        }
    }

    LaunchedEffect(state.groupDeletedSuccess) {
        if (state.groupDeletedSuccess) {
            android.widget.Toast.makeText(context, "Group deleted successfully!", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
            viewModel.resetState()
        }
    }

    if (showDeleteDialog && groupId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group? All associated expenses will be permanently deleted.") },
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

    Scaffold(
        topBar = {
            ExpenseMateTopBar(
                title = if (isEditMode) "Edit Group" else "Create New Group",
                canNavigateBack = true,
                onBackClick = onNavigateBack,
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete Group",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g., Goa Trip, Apartment 3B") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            item {
                Text(
                    text = "Group Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Default Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Constants.SUPPORTED_CURRENCIES.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency.code == curr.code,
                            onClick = { selectedCurrency = curr },
                            label = { Text("${curr.symbol} (${curr.code})") }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { memberNames.add("") }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Member")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    memberNames.forEachIndexed { index, memberName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = memberName,
                                onValueChange = { memberNames[index] = it },
                                label = { Text("Member ${index + 2} Name") },
                                placeholder = { Text("Enter member name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { memberNames.removeAt(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = if (isEditMode) "Update Group" else "Save Group",
                    onClick = {
                        if (isEditMode && groupId != null) {
                            viewModel.updateGroup(
                                groupId = groupId,
                                name = name,
                                description = description,
                                type = selectedType,
                                currencySymbol = selectedCurrency.symbol,
                                memberNames = memberNames
                            )
                        } else {
                            viewModel.createGroup(
                                name = name,
                                description = description,
                                type = selectedType,
                                currencySymbol = selectedCurrency.symbol,
                                memberNames = memberNames
                            )
                        }
                    },
                    isLoading = state.isLoading
                )

                if (isEditMode && groupId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Group")
                    }
                }
            }
        }
    }
}
