package com.expensetracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.usecase.AnalyzeCategoryBreakdownUseCase
import com.expensetracker.domain.usecase.AnalyzeSpendingTrendsUseCase
import com.expensetracker.domain.usecase.GenerateMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val generateMonthlyReportUseCase: GenerateMonthlyReportUseCase,
    private val analyzeCategoryBreakdownUseCase: AnalyzeCategoryBreakdownUseCase,
    private val analyzeSpendingTrendsUseCase: AnalyzeSpendingTrendsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun selectAccount(account: Account?) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
        loadDashboardData()
    }

    fun refreshData() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Load accounts
                val accounts = accountRepository.getAllAccounts()
                
                // Calculate total balance
                val totalBalance = if (_uiState.value.selectedAccount == null) {
                    accounts.sumOf { it.currentBalance }
                } else {
                    _uiState.value.selectedAccount?.currentBalance ?: BigDecimal.ZERO
                }

                // Load monthly report
                val currentMonth = java.time.YearMonth.now()
                val monthlyReport = generateMonthlyReportUseCase(
                    month = currentMonth,
                    accountId = _uiState.value.selectedAccount?.id
                )

                // Load category breakdown
                val currentMonthRange = DateRange.currentMonth()
                val categoryBreakdown = analyzeCategoryBreakdownUseCase(
                    dateRange = currentMonthRange,
                    accountId = _uiState.value.selectedAccount?.id
                )

                // Load spending trends
                val spendingTrends = analyzeSpendingTrendsUseCase(
                    accountId = _uiState.value.selectedAccount?.id
                )

                // Load recent transactions
                val recentTransactions = if (_uiState.value.selectedAccount == null) {
                    transactionRepository.getRecentTransactions(limit = 5)
                } else {
                    transactionRepository.getTransactionsByAccount(
                        accountId = _uiState.value.selectedAccount!!.id,
                        limit = 5
                    )
                }

                // Generate alerts
                val alerts = generateAlerts(accounts, monthlyReport)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    accounts = accounts,
                    totalBalance = totalBalance,
                    monthlyReport = monthlyReport,
                    categoryBreakdown = categoryBreakdown,
                    spendingTrends = spendingTrends,
                    recentTransactions = recentTransactions,
                    alerts = alerts
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    private fun generateAlerts(
        accounts: List<Account>,
        monthlyReport: com.expensetracker.domain.model.MonthlyReport?
    ): List<FinancialAlert> {
        val alerts = mutableListOf<FinancialAlert>()

        // Low balance alerts
        accounts.forEach { account ->
            if (account.currentBalance < BigDecimal("1000")) { // Configurable threshold
                alerts.add(
                    FinancialAlert(
                        id = "low_balance_${account.id}",
                        type = AlertType.LOW_BALANCE,
                        title = "Low Balance Alert",
                        message = "Account ${account.nickname} has a low balance of ₹${account.currentBalance}",
                        severity = AlertSeverity.WARNING,
                        accountId = account.id
                    )
                )
            }
        }

        // Spending increase alerts
        monthlyReport?.let { report ->
            if (report.comparisonToPreviousMonth.expenseChangePercentage > 20f) {
                alerts.add(
                    FinancialAlert(
                        id = "spending_increase",
                        type = AlertType.UNUSUAL_SPENDING,
                        title = "Spending Increase",
                        message = "Your spending increased by ${report.comparisonToPreviousMonth.expenseChangePercentage.toInt()}% this month",
                        severity = AlertSeverity.INFO
                    )
                )
            }
        }

        return alerts
    }
}