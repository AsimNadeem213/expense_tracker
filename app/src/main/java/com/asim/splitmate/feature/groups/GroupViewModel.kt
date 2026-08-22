package com.asim.splitmate.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.GroupType
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.SettlementRepository
import com.asim.splitmate.domain.usecase.CalculateGroupBalancesUseCase
import com.asim.splitmate.domain.usecase.GroupBalancesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

import com.asim.splitmate.domain.model.Expense

data class GroupUiState(
    val groups: List<Group> = emptyList(),
    val currentGroup: Group? = null,
    val groupExpenses: List<Expense> = emptyList(),
    val balancesResult: GroupBalancesResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupCreatedSuccess: Boolean = false,
    val groupUpdatedSuccess: Boolean = false,
    val groupDeletedSuccess: Boolean = false
)

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val calculateGroupBalancesUseCase: CalculateGroupBalancesUseCase,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: "usr_you"
            launch {
                groupRepository.syncRemoteData(userId)
            }
            groupRepository.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
            }
        }
    }

    fun selectGroup(groupId: String) {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: "usr_you"

            groupRepository.getGroupById(groupId).collect { group ->
                _uiState.value = _uiState.value.copy(currentGroup = group)
            }
        }

        viewModelScope.launch {
            expenseRepository.getExpensesForGroup(groupId).collect { expenses ->
                _uiState.value = _uiState.value.copy(groupExpenses = expenses)
            }
        }

        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: "usr_you"

            calculateGroupBalancesUseCase.execute(groupId, userId).collect { res ->
                _uiState.value = _uiState.value.copy(balancesResult = res)
            }
        }
    }

    fun createGroup(name: String, description: String, type: GroupType, currencySymbol: String, memberNames: List<String>) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = userDao.getCurrentUserSync()?.toDomain() ?: User("usr_you", "Asim", "asim@splitmate.app", isCurrentUser = true)

            val membersList = mutableListOf(currentUser)
            memberNames.filter { it.isNotBlank() }.forEach { mName ->
                membersList.add(User(id = "usr_" + UUID.randomUUID().toString().take(8), name = mName, email = ""))
            }

            val group = Group(
                id = "grp_" + UUID.randomUUID().toString().take(8),
                name = name.trim(),
                description = description.trim(),
                type = type,
                currencySymbol = currencySymbol,
                createdBy = currentUser.id,
                createdAt = System.currentTimeMillis(),
                members = membersList,
                inviteCode = name.take(3).uppercase() + UUID.randomUUID().toString().take(4).uppercase()
            )

            when (val res = groupRepository.createGroup(group)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, groupCreatedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateGroup(groupId: String, name: String, description: String, type: GroupType, currencySymbol: String, memberNames: List<String>) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = userDao.getCurrentUserSync()?.toDomain() ?: User("usr_you", "Asim", "asim@splitmate.app", isCurrentUser = true)
            val currentGroup = _uiState.value.currentGroup

            val existingNonCurrentMembers = currentGroup?.members?.filter { !it.isCurrentUser } ?: emptyList()

            val membersList = mutableListOf(currentUser)
            memberNames.filter { it.isNotBlank() }.forEachIndexed { index, mName ->
                val existingMember = existingNonCurrentMembers.getOrNull(index)
                val mId = existingMember?.id ?: ("usr_" + UUID.randomUUID().toString().take(8))
                membersList.add(User(id = mId, name = mName.trim(), email = existingMember?.email ?: ""))
            }

            val group = Group(
                id = groupId,
                name = name.trim(),
                description = description.trim(),
                type = type,
                currencySymbol = currencySymbol,
                createdBy = currentGroup?.createdBy ?: currentUser.id,
                createdAt = currentGroup?.createdAt ?: System.currentTimeMillis(),
                members = membersList,
                inviteCode = currentGroup?.inviteCode ?: (name.take(3).uppercase() + UUID.randomUUID().toString().take(4).uppercase())
            )

            when (val res = groupRepository.updateGroup(group)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, groupCreatedSuccess = true, groupUpdatedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = groupRepository.deleteGroup(groupId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, groupDeletedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun joinGroup(inviteCode: String) {
        if (inviteCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Invite code cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = groupRepository.joinGroupWithInviteCode(inviteCode)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, groupCreatedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message ?: "Failed to join group")
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            groupCreatedSuccess = false,
            groupUpdatedSuccess = false,
            groupDeletedSuccess = false,
            error = null
        )
    }
}
