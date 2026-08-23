package com.asim.splitmate.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.usecase.DashboardSummary
import com.asim.splitmate.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val userName: String = "You",
    val currentUserId: String = "",
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val groupRepository: GroupRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val user = userDao.getCurrentUserSync()
            val userId = user?.id ?: com.asim.splitmate.core.firebase.FirebaseHelper.currentUserId ?: "usr_you"
            val userName = user?.name ?: "You"

            _uiState.value = _uiState.value.copy(userName = userName, currentUserId = userId)

            launch {
                groupRepository.syncRemoteData(userId)
            }

            getDashboardDataUseCase.execute(userId).collect { summary ->
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    currentUserId = userId,
                    isLoading = false
                )
            }
        }
    }
}
