package com.asim.splitmate.feature.settlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SettlementUiState(
    val currentGroup: Group? = null,
    val payer: User? = null,
    val recipient: User? = null,
    val suggestedAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSavedSuccess: Boolean = false
)

class SettlementViewModel(
    private val settlementRepository: SettlementRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    fun setup(groupId: String, payerId: String?, recipientId: String?, amount: Double?) {
        viewModelScope.launch {
            groupRepository.getGroupById(groupId).collect { group ->
                if (group != null) {
                    val p = group.members.find { it.id == payerId } ?: group.members.firstOrNull()
                    val r = group.members.find { it.id == recipientId } ?: group.members.getOrNull(1)

                    _uiState.value = _uiState.value.copy(
                        currentGroup = group,
                        payer = p,
                        recipient = r,
                        suggestedAmount = amount ?: 0.0
                    )
                }
            }
        }
    }

    fun recordSettlement(
        groupId: String,
        amountText: String,
        paymentMethod: String,
        notes: String
    ) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val payer = _uiState.value.payer ?: return
        val recipient = _uiState.value.recipient ?: return

        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(error = "Enter a valid settlement amount")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val settlement = Settlement(
                id = "set_" + UUID.randomUUID().toString().take(8),
                groupId = groupId,
                payerId = payer.id,
                payerName = payer.name,
                recipientId = recipient.id,
                recipientName = recipient.name,
                amount = amount,
                date = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                notes = notes
            )

            when (val res = settlementRepository.recordSettlement(settlement)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSavedSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setPayer(user: User) {
        _uiState.value = _uiState.value.copy(payer = user)
    }

    fun setRecipient(user: User) {
        _uiState.value = _uiState.value.copy(recipient = user)
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(isSavedSuccess = false, error = null)
    }
}
