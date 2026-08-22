package com.asim.splitmate.feature.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.usecase.CalculateGroupBalancesUseCase
import com.asim.splitmate.domain.usecase.GroupBalancesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BalancesUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val balancesResult: GroupBalancesResult? = null,
    val isLoading: Boolean = true
)

class BalancesViewModel(
    private val groupRepository: GroupRepository,
    private val calculateGroupBalancesUseCase: CalculateGroupBalancesUseCase,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalancesUiState())
    val uiState: StateFlow<BalancesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: "usr_you"

            groupRepository.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
                if (groups.isNotEmpty() && _uiState.value.selectedGroup == null) {
                    selectGroup(groups.first().id)
                }
            }
        }
    }

    fun selectGroup(groupId: String) {
        val group = _uiState.value.groups.find { it.id == groupId }
        _uiState.value = _uiState.value.copy(selectedGroup = group)

        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: "usr_you"

            calculateGroupBalancesUseCase.execute(groupId, userId).collect { res ->
                _uiState.value = _uiState.value.copy(balancesResult = res)
            }
        }
    }
}
