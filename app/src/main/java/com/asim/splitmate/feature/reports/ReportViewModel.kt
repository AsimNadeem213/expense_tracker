package com.asim.splitmate.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod(val title: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

data class CategoryExpenseStat(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float
)

data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val totalPaidByYou: Double = 0.0,
    val totalYourShare: Double = 0.0,
    val totalExpenseCount: Int = 0,
    val averageExpenseAmount: Double = 0.0,
    val categoryStats: List<CategoryExpenseStat> = emptyList(),
    val periodExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val currentUserName: String = "You"
)

class ReportViewModel(
    private val expenseRepository: ExpenseRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var cachedExpenses: List<Expense> = emptyList()

    init {
        observeExpenses()
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentUser = userDao.getCurrentUserSync()
            val currentUserId = currentUser?.id ?: "usr_you"
            val currentUserName = currentUser?.name ?: "You"

            expenseRepository.getAllExpenses().collect { allExpenses ->
                cachedExpenses = allExpenses
                computeAndEmitReportData(currentUserId, currentUserName)
            }
        }
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        if (period != ReportPeriod.CUSTOM) {
            viewModelScope.launch {
                val currentUser = userDao.getCurrentUserSync()
                val currentUserId = currentUser?.id ?: "usr_you"
                val currentUserName = currentUser?.name ?: "You"
                computeAndEmitReportData(currentUserId, currentUserName)
            }
        }
    }

    fun setCustomDateRange(startDateMillis: Long, endDateMillis: Long) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        _uiState.value = _uiState.value.copy(
            selectedPeriod = ReportPeriod.CUSTOM,
            customStartDate = startCal.timeInMillis,
            customEndDate = endCal.timeInMillis
        )
        viewModelScope.launch {
            val currentUser = userDao.getCurrentUserSync()
            val currentUserId = currentUser?.id ?: "usr_you"
            val currentUserName = currentUser?.name ?: "You"
            computeAndEmitReportData(currentUserId, currentUserName)
        }
    }

    fun loadReportData() {
        viewModelScope.launch {
            val currentUser = userDao.getCurrentUserSync()
            val currentUserId = currentUser?.id ?: "usr_you"
            val currentUserName = currentUser?.name ?: "You"
            computeAndEmitReportData(currentUserId, currentUserName)
        }
    }

    private fun computeAndEmitReportData(currentUserId: String, currentUserName: String) {
        val (startTime, endTime) = getTimeBounds(
            _uiState.value.selectedPeriod,
            _uiState.value.customStartDate,
            _uiState.value.customEndDate
        )

        val filteredExpenses = cachedExpenses.filter { exp ->
            (exp.date in startTime..endTime) && (
                exp.paidByUserId == currentUserId || exp.splits.any { it.userId == currentUserId }
            )
        }

        var paidByYou = 0.0
        var yourShare = 0.0
        val categoryTotals = mutableMapOf<Category, Double>()

        for (exp in filteredExpenses) {
            if (exp.paidByUserId == currentUserId) {
                paidByYou += exp.amount
            }
            val userSplit = exp.splits.find { it.userId == currentUserId }
            val userSplitAmt = userSplit?.amount ?: if (exp.paidByUserId == currentUserId) exp.amount else 0.0
            yourShare += userSplitAmt

            val cat = exp.category
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + userSplitAmt
        }

        val totalCatSpend = categoryTotals.values.sum().coerceAtLeast(1.0)
        val catStats = categoryTotals.map { (cat, amount) ->
            CategoryExpenseStat(
                category = cat,
                totalAmount = amount,
                percentage = (amount / totalCatSpend * 100).toFloat()
            )
        }.sortedByDescending { it.totalAmount }

        val count = filteredExpenses.size
        val avg = if (count > 0) yourShare / count else 0.0

        _uiState.value = _uiState.value.copy(
            totalPaidByYou = paidByYou,
            totalYourShare = yourShare,
            totalExpenseCount = count,
            averageExpenseAmount = avg,
            categoryStats = catStats,
            periodExpenses = filteredExpenses,
            isLoading = false,
            currentUserName = currentUserName
        )
    }

    private fun getTimeBounds(period: ReportPeriod, customStart: Long?, customEnd: Long?): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (period) {
            ReportPeriod.TODAY -> Pair(cal.timeInMillis, Long.MAX_VALUE)
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                Pair(cal.timeInMillis, Long.MAX_VALUE)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            ReportPeriod.CUSTOM -> Pair(customStart ?: 0L, customEnd ?: Long.MAX_VALUE)
        }
    }
}
