package com.asim.splitmate.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Split
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.usecase.AddExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val currentGroup: Group? = null,
    val availableGroups: List<Group> = emptyList(),
    val selectedPaidByUser: User? = null,
    val selectedMembers: List<User> = emptyList(),
    val splitType: SplitType = SplitType.EQUAL,
    val customSplits: List<Split> = emptyList(),
    val currentExpense: Expense? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSavedSuccess: Boolean = false
)

class ExpenseViewModel(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    fun setupForGroup(groupId: String?) {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()?.toDomain() ?: User("usr_you", "Asim", "asim@splitmate.app", isCurrentUser = true)
            if (!groupId.isNullOrBlank()) {
                groupRepository.getGroupById(groupId).collect { group ->
                    if (group != null) {
                        _uiState.value = _uiState.value.copy(
                            currentGroup = group,
                            selectedPaidByUser = _uiState.value.selectedPaidByUser ?: (group.members.find { it.id == user.id } ?: group.members.firstOrNull() ?: user),
                            selectedMembers = if (_uiState.value.selectedMembers.isEmpty()) group.members else _uiState.value.selectedMembers
                        )
                    }
                }
            } else {
                groupRepository.getAllGroups().collect { groups ->
                    _uiState.value = _uiState.value.copy(availableGroups = groups)
                    if (_uiState.value.currentGroup == null && groups.isNotEmpty()) {
                        val firstGroup = groups.first()
                        _uiState.value = _uiState.value.copy(
                            currentGroup = firstGroup,
                            selectedPaidByUser = firstGroup.members.find { it.id == user.id } ?: firstGroup.members.firstOrNull() ?: user,
                            selectedMembers = firstGroup.members
                        )
                    }
                }
            }
        }
    }

    fun selectGroup(group: Group) {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()?.toDomain() ?: User("usr_you", "Asim", "asim@splitmate.app", isCurrentUser = true)
            _uiState.value = _uiState.value.copy(
                currentGroup = group,
                selectedPaidByUser = group.members.find { it.id == user.id } ?: group.members.firstOrNull() ?: user,
                selectedMembers = group.members
            )
        }
    }

    fun loadExpenseDetail(expenseId: String) {
        viewModelScope.launch {
            val exp = expenseRepository.getExpenseById(expenseId)
            _uiState.value = _uiState.value.copy(currentExpense = exp)
        }
    }

    fun addExpense(
        groupId: String?,
        title: String,
        amountText: String,
        category: Category,
        notes: String
    ) {
        val targetGroupId = if (!groupId.isNullOrBlank()) groupId else _uiState.value.currentGroup?.id
        if (targetGroupId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Please select a group")
            return
        }

        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Expense title cannot be empty")
            return
        }

        val amount = amountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Please enter a valid amount greater than 0")
            return
        }

        val paidByUser = _uiState.value.selectedPaidByUser
        if (paidByUser == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Please select who paid for this expense")
            return
        }

        val selectedMembers = _uiState.value.selectedMembers
        if (selectedMembers.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Please select at least one participant")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                when (val res = addExpenseUseCase.execute(
                    groupId = targetGroupId,
                    title = title,
                    amount = amount,
                    category = category,
                    paidByUser = paidByUser,
                    selectedMembers = selectedMembers,
                    splitType = _uiState.value.splitType,
                    customSplits = _uiState.value.customSplits,
                    notes = notes
                )) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSavedSuccess = true)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = res.message ?: "Failed to save expense")
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "An error occurred while saving expense")
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = expenseRepository.deleteExpense(expenseId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSavedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message ?: "Failed to delete expense")
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setSplitType(splitType: SplitType) {
        _uiState.value = _uiState.value.copy(splitType = splitType)
    }

    fun setPaidByUser(user: User) {
        _uiState.value = _uiState.value.copy(selectedPaidByUser = user)
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(isSavedSuccess = false, isLoading = false, error = null)
    }
}
