package com.asim.splitmate.feature.groups

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asim.splitmate.core.ui.components.EmptyState
import com.asim.splitmate.core.ui.components.ExpenseMateBottomBar
import com.asim.splitmate.core.ui.components.ExpenseMateTopBar
import com.asim.splitmate.core.ui.components.GroupCard
import com.asim.splitmate.core.ui.theme.EmeraldPrimary
import com.asim.splitmate.domain.model.Group

import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

import androidx.activity.compose.BackHandler

@Composable
fun GroupListScreen(
    viewModel: GroupViewModel,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToEditGroup: (String) -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateTab: (String) -> Unit
) {
    BackHandler {
        onNavigateTab("dashboard")
    }

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var inviteCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            Toast.makeText(context, state.error!!, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.groupCreatedSuccess, state.joinedGroupId) {
        if (state.groupCreatedSuccess && !state.joinedGroupId.isNullOrBlank()) {
            val groupId = state.joinedGroupId!!
            Toast.makeText(context, "Successfully joined group!", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            onNavigateToGroupDetail(groupId)
        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Group") },
            text = {
                Column {
                    Button(
                        onClick = {
                            showJoinDialog = false
                            onNavigateToQrScanner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Scan QR Code")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Or enter the group invite code manually:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it },
                        label = { Text("Invite Code") },
                        placeholder = { Text("e.g. TRIP1234") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val code = inviteCodeInput.trim()
                        if (code.isNotEmpty()) {
                            showJoinDialog = false
                            viewModel.joinGroup(code)
                            inviteCodeInput = ""
                        }
                    }
                ) {
                    Text("Join via Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                            viewModel.deleteGroup(gId)
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
        topBar = {
            ExpenseMateTopBar(
                title = "Groups",
                actions = {
                    IconButton(onClick = onNavigateToQrScanner) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Group QR Code")
                    }
                    IconButton(onClick = { showJoinDialog = true }) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = "Join Group via Code")
                    }
                }
            )
        },
        bottomBar = {
            ExpenseMateBottomBar(currentRoute = "groups", onNavigate = onNavigateTab)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Group")
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
        } else if (state.groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyState(
                    title = "No Groups Created Yet",
                    description = "Form a group, scan a QR code, or join using an invite code to start splitting expenses.",
                    actionText = "Create Group",
                    onActionClick = onNavigateToCreateGroup
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.groups.size} Active Groups",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                items(state.groups) { group ->
                    val currentUserId = state.currentUserId
                    val isCreator = when {
                        group.createdBy.isBlank() -> false
                        currentUserId.isNotBlank() && group.createdBy == currentUserId -> true
                        else -> {
                            val currentMember = group.members.find { it.isCurrentUser || (currentUserId.isNotBlank() && it.id == currentUserId) }
                            currentMember != null && currentMember.id == group.createdBy
                        }
                    }
                    GroupCard(
                        group = group,
                        onClick = { onNavigateToGroupDetail(group.id) },
                        onEditClick = if (isCreator) { { onNavigateToEditGroup(group.id) } } else null,
                        onDeleteClick = if (isCreator) { { groupToDelete = group } } else null
                    )
                }
            }
        }
    }
}
