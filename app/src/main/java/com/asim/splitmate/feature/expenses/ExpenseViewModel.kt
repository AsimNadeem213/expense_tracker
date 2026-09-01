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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val currentGroup: Group? = null,
    val availableGroups: List<Group> = emptyList(),
    val selectedPaidByUser: User? = null,
    val selectedMembers: List<User> = emptyList(),
    val splitType: SplitType = SplitType.EQUAL,
    val customSplits: List<Split> = emptyList(),
    val customSplitInputs: Map<String, String> = emptyMap(),
    val currentExpense: Expense? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSavedSuccess: Boolean = false,
    val currentUserId: String = ""
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
            _uiState.value = _uiState.value.copy(currentUserId = user.id)
            if (!groupId.isNullOrBlank()) {
                groupRepository.getGroupById(groupId).collect { group ->
                    if (group != null) {
                        _uiState.value = _uiState.value.copy(
                            currentGroup = group,
                            selectedPaidByUser = _uiState.value.selectedPaidByUser ?: (group.members.find { it.id == user.id } ?: group.members.firstOrNull() ?: user),
                            selectedMembers = if (_uiState.value.currentExpense == null || _uiState.value.selectedMembers.isEmpty()) group.members else _uiState.value.selectedMembers,
                            currentUserId = user.id
                        )
                    }
                }
            } else {
                groupRepository.getAllGroups().collect { groups ->
                    _uiState.value = _uiState.value.copy(availableGroups = groups, currentUserId = user.id)
                    if (_uiState.value.currentGroup == null && groups.isNotEmpty()) {
                        val firstGroup = groups.first()
                        _uiState.value = _uiState.value.copy(
                            currentGroup = firstGroup,
                            selectedPaidByUser = firstGroup.members.find { it.id == user.id } ?: firstGroup.members.firstOrNull() ?: user,
                            selectedMembers = firstGroup.members,
                            currentUserId = user.id
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
                selectedMembers = group.members,
                currentUserId = user.id
            )
        }
    }

    fun loadExpenseDetail(expenseId: String) {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()?.toDomain() ?: User("usr_you", "Asim", "asim@splitmate.app", isCurrentUser = true)
            val exp = expenseRepository.getExpenseById(expenseId)
            if (exp != null) {
                val group = _uiState.value.currentGroup ?: groupRepository.getGroupById(exp.groupId).firstOrNull()
                val members = group?.members ?: emptyList()
                val payer = members.find { it.id == exp.paidByUserId } ?: User(exp.paidByUserId, exp.paidByUserName, "")
                val splitUserIds = exp.splits.map { it.userId }.toSet()
                val participants = members.filter { splitUserIds.contains(it.id) }.ifEmpty { members }

                val inputMap = exp.splits.associate { split ->
                    val text = when (exp.splitType) {
                        SplitType.EXACT -> if (split.amount == 0.0) "0" else if (split.amount % 1.0 == 0.0) split.amount.toLong().toString() else split.amount.toString()
                        SplitType.PERCENTAGE -> if (split.percentage == 0.0) "0" else if (split.percentage % 1.0 == 0.0) split.percentage.toLong().toString() else split.percentage.toString()
                        SplitType.SHARES -> split.shares.toString()
                        SplitType.EQUAL -> ""
                    }
                    split.userId to text
                }

                _uiState.value = _uiState.value.copy(
                    currentExpense = exp,
                    currentGroup = group,
                    selectedPaidByUser = payer,
                    selectedMembers = participants,
                    splitType = exp.splitType,
                    customSplits = exp.splits,
                    customSplitInputs = inputMap,
                    selectedDate = exp.date,
                    currentUserId = user.id
                )
            }
        }
    }

    fun setSelectedDate(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = dateMillis)
    }

    fun addExpense(
        groupId: String?,
        title: String,
        amountText: String,
        category: Category,
        notes: String,
        dateMillis: Long = _uiState.value.selectedDate
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
                    notes = notes,
                    date = dateMillis,
                    existingExpenseId = _uiState.value.currentExpense?.id
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

    fun toggleMemberParticipant(user: User) {
        val currentList = _uiState.value.selectedMembers.toMutableList()
        if (currentList.any { it.id == user.id }) {
            if (currentList.size > 1) {
                currentList.removeAll { it.id == user.id }
            }
        } else {
            currentList.add(user)
        }
        _uiState.value = _uiState.value.copy(selectedMembers = currentList)
    }

    fun updateCustomSplitAmountInput(userId: String, userName: String, input: String) {
        val newInputs = _uiState.value.customSplitInputs.toMutableMap()
        newInputs[userId] = input

        val parsedAmount = input.toDoubleOrNull() ?: 0.0
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(amount = parsedAmount)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, amount = parsedAmount))
        }
        _uiState.value = _uiState.value.copy(
            customSplitInputs = newInputs,
            customSplits = currentSplits
        )
    }

    fun updateCustomSplitPercentageInput(userId: String, userName: String, input: String) {
        val newInputs = _uiState.value.customSplitInputs.toMutableMap()
        newInputs[userId] = input

        val parsedPct = input.toDoubleOrNull() ?: 0.0
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(percentage = parsedPct)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, percentage = parsedPct))
        }
        _uiState.value = _uiState.value.copy(
            customSplitInputs = newInputs,
            customSplits = currentSplits
        )
    }

    fun updateCustomSplitSharesInput(userId: String, userName: String, input: String) {
        val newInputs = _uiState.value.customSplitInputs.toMutableMap()
        newInputs[userId] = input

        val parsedShares = input.toIntOrNull() ?: 1
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(shares = parsedShares)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, shares = parsedShares))
        }
        _uiState.value = _uiState.value.copy(
            customSplitInputs = newInputs,
            customSplits = currentSplits
        )
    }

    fun updateCustomSplitAmount(userId: String, userName: String, amount: Double) {
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(amount = amount)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, amount = amount))
        }
        _uiState.value = _uiState.value.copy(customSplits = currentSplits)
    }

    fun updateCustomSplitPercentage(userId: String, userName: String, percentage: Double) {
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(percentage = percentage)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, percentage = percentage))
        }
        _uiState.value = _uiState.value.copy(customSplits = currentSplits)
    }

    fun updateCustomSplitShares(userId: String, userName: String, shares: Int) {
        val currentSplits = _uiState.value.customSplits.toMutableList()
        val index = currentSplits.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            currentSplits[index] = currentSplits[index].copy(shares = shares)
        } else {
            currentSplits.add(Split(userId = userId, userName = userName, shares = shares))
        }
        _uiState.value = _uiState.value.copy(customSplits = currentSplits)
    }

    fun resetState() {
        _uiState.value = ExpenseUiState()
    }
}
