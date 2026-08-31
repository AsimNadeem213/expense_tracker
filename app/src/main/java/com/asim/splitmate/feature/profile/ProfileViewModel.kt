package com.asim.splitmate.feature.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.core.utils.CsvExportHelper
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.AuthRepository
import com.asim.splitmate.domain.repository.ExpenseRepository
import com.asim.splitmate.domain.repository.GroupRepository
import com.asim.splitmate.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class ProfileUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false,
    val isLoggedOut: Boolean = false
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Name cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val current = _uiState.value.currentUser ?: return@launch
                val updated = current.copy(name = name.trim(), email = email.trim())
                userDao.insertUser(UserEntity.fromDomain(updated))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUser = updated,
                    updateSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }

    fun exportExpenses(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val groups = groupRepository.getAllGroups().first()
                if (groups.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No group expenses available to export")
                    return@launch
                }

                val expensesMap = mutableMapOf<String, List<Expense>>()
                for (g in groups) {
                    val expList = expenseRepository.getExpensesForGroup(g.id).first()
                    expensesMap[g.id] = expList
                }

                val file = com.asim.splitmate.core.utils.XlsxExportHelper.generateReportXlsx(
                    context = context,
                    groups = groups,
                    expensesMap = expensesMap
                )

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Expense Report (XLSX)"))
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                // Fallback text share if file provider or spreadsheet reader fails
                val groups = groupRepository.getAllGroups().first()
                if (groups.isNotEmpty()) {
                    val mainGroup = groups.first()
                    val expenses = expenseRepository.getExpensesForGroup(mainGroup.id).first()
                    val csvData = CsvExportHelper.generateGroupCsv(mainGroup, expenses, emptyList())
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, csvData)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Expense Summary"))
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun exportExpensesPdf(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val groups = groupRepository.getAllGroups().first()
                if (groups.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No group expenses available to export")
                    return@launch
                }

                val expensesMap = mutableMapOf<String, List<Expense>>()
                for (g in groups) {
                    val expList = expenseRepository.getExpensesForGroup(g.id).first()
                    expensesMap[g.id] = expList
                }

                val file = com.asim.splitmate.core.utils.PdfExportHelper.generateReportPdf(
                    context = context,
                    groups = groups,
                    expensesMap = expensesMap
                )

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Expense Report (PDF)"))
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to export PDF: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(updateSuccess = false, error = null)
    }
}
