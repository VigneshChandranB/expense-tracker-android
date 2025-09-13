package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Domain model for category breakdown analysis
 * Provides detailed spending analysis by category for a specific period
 */
data class CategoryBreakdown(
    val period: DateRange,
    val totalAmount: BigDecimal,
    val categorySpending: List<CategorySpending>,
    val topCategories: List<CategorySpending>,
    val unusedCategories: List<Category>
)

/**
 * Spending information for a specific category
 */
data class CategorySpending(
    val category: Category,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int,
    val averageTransactionAmount: BigDecimal,
    val trend: SpendingTrend
)

/**
 * Date range for analysis periods
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    companion object {
        fun currentMonth(): DateRange {
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            return DateRange(startOfMonth, endOfMonth)
        }
        
        fun previousMonth(): DateRange {
            val now = LocalDate.now()
            val previousMonth = now.minusMonths(1)
            val startOfMonth = previousMonth.withDayOfMonth(1)
            val endOfMonth = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth())
            return DateRange(startOfMonth, endOfMonth)
        }
        
        fun lastNDays(days: Int): DateRange {
            val now = LocalDate.now()
            return DateRange(now.minusDays(days.toLong()), now)
        }
        
        fun yearToDate(): DateRange {
            val now = LocalDate.now()
            val startOfYear = now.withDayOfYear(1)
            return DateRange(startOfYear, now)
        }
    }
}

/**
 * Spending trend information
 */
data class SpendingTrend(
    val direction: TrendDirection,
    val changePercentage: Float,
    val isSignificant: Boolean
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}