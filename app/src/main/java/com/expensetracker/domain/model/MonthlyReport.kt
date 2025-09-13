package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Domain model for monthly financial report
 * Contains comprehensive spending and income analysis for a specific month
 */
data class MonthlyReport(
    val month: YearMonth,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netAmount: BigDecimal,
    val categoryBreakdown: Map<Category, BigDecimal>,
    val topMerchants: List<MerchantSpending>,
    val comparisonToPreviousMonth: MonthComparison,
    val accountBreakdown: Map<Account, AccountSummary>
)

/**
 * Spending information for a specific merchant
 */
data class MerchantSpending(
    val merchant: String,
    val totalAmount: BigDecimal,
    val transactionCount: Int,
    val category: Category,
    val averageAmount: BigDecimal
)

/**
 * Comparison data between current and previous month
 */
data class MonthComparison(
    val incomeChange: BigDecimal,
    val expenseChange: BigDecimal,
    val incomeChangePercentage: Float,
    val expenseChangePercentage: Float,
    val significantChanges: List<CategoryChange>
)

/**
 * Category-wise spending change information
 */
data class CategoryChange(
    val category: Category,
    val currentAmount: BigDecimal,
    val previousAmount: BigDecimal,
    val changeAmount: BigDecimal,
    val changePercentage: Float
)

/**
 * Account-specific summary for the month
 */
data class AccountSummary(
    val account: Account,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val transactionCount: Int,
    val balanceChange: BigDecimal
)