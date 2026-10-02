package com.asim.splitmate.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

import androidx.compose.runtime.Immutable

enum class ReportPeriod(val title: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

@Immutable
data class CategoryExpenseStat(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float
)

@Immutable
data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val totalPaidByYou: Double = 0.0,
    val totalYourShare: Double = 0.0,
    val totalGroupSpending: Double = 0.0,
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
                computeAndEmitReportData(currentUserId, currentUserName, currentUser)
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
                computeAndEmitReportData(currentUserId, currentUserName, currentUser)
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
            computeAndEmitReportData(currentUserId, currentUserName, currentUser)
        }
    }

    fun loadReportData() {
        viewModelScope.launch {
            val currentUser = userDao.getCurrentUserSync()
            val currentUserId = currentUser?.id ?: "usr_you"
            val currentUserName = currentUser?.name ?: "You"
            computeAndEmitReportData(currentUserId, currentUserName, currentUser)
        }
    }

    private fun computeAndEmitReportData(
        currentUserId: String,
        currentUserName: String,
        currentUserDb: UserEntity? = null
    ) {
        val (startTime, endTime) = getTimeBounds(
            _uiState.value.selectedPeriod,
            _uiState.value.customStartDate,
            _uiState.value.customEndDate
        )

        val firebaseUid = com.asim.splitmate.core.firebase.FirebaseHelper.currentUserId
        val possibleUserIds = setOfNotNull(
            currentUserId,
            currentUserDb?.id,
            currentUserDb?.name,
            "usr_you",
            "You",
            firebaseUid
        ).filter { it.isNotBlank() }.toSet()

        fun isMatchUser(userId: String?, userName: String? = null): Boolean {
            if (userId != null && possibleUserIds.contains(userId)) return true
            if (userId != null && userId.equals("You", ignoreCase = true)) return true
            if (userName != null && currentUserName.isNotBlank() && (userName.equals("You", ignoreCase = true) || userName.equals(currentUserName, ignoreCase = true))) return true
            return false
        }

        val filteredExpenses = cachedExpenses.filter { exp ->
            (exp.date in startTime..endTime) && (
                isMatchUser(exp.paidByUserId, exp.paidByUserName) ||
                exp.splits.any { isMatchUser(it.userId, it.userName) } ||
                isMatchUser(exp.createdBy)
            )
        }

        var paidByYou = 0.0
        var yourShare = 0.0
        var totalGroupSpending = 0.0
        val categoryTotals = mutableMapOf<Category, Double>()

        for (exp in filteredExpenses) {
            totalGroupSpending += exp.amount

            val userPaid = isMatchUser(exp.paidByUserId, exp.paidByUserName)
            if (userPaid) {
                paidByYou += exp.amount
            }

            val userSplit = exp.splits.find { isMatchUser(it.userId, it.userName) }
            val userSplitAmt = if (exp.splits.isNotEmpty()) {
                userSplit?.amount ?: 0.0
            } else {
                if (userPaid) exp.amount else 0.0
            }
            yourShare += userSplitAmt

            if (userSplitAmt > 0.0) {
                val cat = exp.category
                categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + userSplitAmt
            }
        }

        val roundedPaid = kotlin.math.round(paidByYou * 100.0) / 100.0
        val roundedYourShare = kotlin.math.round(yourShare * 100.0) / 100.0
        val roundedTotalGroup = kotlin.math.round(totalGroupSpending * 100.0) / 100.0

        val totalCatSpend = categoryTotals.values.sum()
        val catStats = categoryTotals.filter { it.value > 0.001 }.map { (cat, amount) ->
            val roundedAmount = kotlin.math.round(amount * 100.0) / 100.0
            val pct = if (totalCatSpend > 0.0) ((amount / totalCatSpend) * 100.0).toFloat() else 0f
            CategoryExpenseStat(
                category = cat,
                totalAmount = roundedAmount,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmount }

        val count = filteredExpenses.size
        val avg = if (count > 0) kotlin.math.round((roundedYourShare / count) * 100.0) / 100.0 else 0.0

        _uiState.value = _uiState.value.copy(
            totalPaidByYou = roundedPaid,
            totalYourShare = roundedYourShare,
            totalGroupSpending = roundedTotalGroup,
            totalExpenseCount = count,
            averageExpenseAmount = avg,
            categoryStats = catStats,
            periodExpenses = filteredExpenses,
            isLoading = false,
            currentUserName = currentUserName
        )
    }

    private fun getTimeBounds(period: ReportPeriod, customStart: Long?, customEnd: Long?): Pair<Long, Long> {
        return when (period) {
            ReportPeriod.TODAY -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.THIS_WEEK -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_WEEK, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.THIS_MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.THIS_YEAR -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            ReportPeriod.CUSTOM -> Pair(customStart ?: 0L, customEnd ?: Long.MAX_VALUE)
        }
    }
}
