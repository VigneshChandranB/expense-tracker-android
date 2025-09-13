package com.expensetracker.presentation.dashboard

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.CategoryBreakdown
import com.expensetracker.domain.model.MonthlyReport
import com.expensetracker.domain.model.SpendingTrends
import com.expensetracker.domain.model.Transaction
import java.math.BigDecimal

/**
 * UI state for the dashboard screen
 * Contains all data needed to display the multi-account overview
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null, // null means all accounts view
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val monthlyReport: MonthlyReport? = null,
    val categoryBreakdown: CategoryBreakdown? = null,
    val spendingTrends: SpendingTrends? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val alerts: List<FinancialAlert> = emptyList(),
    val error: String? = null
)

/**
 * Financial alerts for the dashboard
 */
data class FinancialAlert(
    val id: String,
    val type: AlertType,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val accountId: Long? = null
)

enum class AlertType {
    LOW_BALANCE,
    SPENDING_LIMIT_EXCEEDED,
    UNUSUAL_SPENDING,
    BILL_DUE_SOON
}

enum class AlertSeverity {
    INFO, WARNING, ERROR
}

/**
 * Dashboard view modes
 */
enum class DashboardViewMode {
    ALL_ACCOUNTS,
    SINGLE_ACCOUNT
}