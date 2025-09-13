package com.expensetracker.domain.repository

import com.expensetracker.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Repository interface for analytics and insights operations
 * Provides access to aggregated financial data for analysis
 */
interface AnalyticsRepository {
    
    /**
     * Generate monthly report for a specific month
     */
    suspend fun generateMonthlyReport(month: YearMonth): MonthlyReport
    
    /**
     * Get category breakdown for a specific date range
     */
    suspend fun getCategoryBreakdown(dateRange: DateRange): CategoryBreakdown
    
    /**
     * Calculate spending trends over time
     */
    suspend fun calculateSpendingTrends(months: Int = 12): SpendingTrends
    
    /**
     * Detect spending anomalies
     */
    suspend fun detectSpendingAnomalies(
        dateRange: DateRange,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly>
    
    /**
     * Get total spending for a date range
     */
    suspend fun getTotalSpending(dateRange: DateRange): BigDecimal
    
    /**
     * Get total income for a date range
     */
    suspend fun getTotalIncome(dateRange: DateRange): BigDecimal
    
    /**
     * Get spending by category for a date range
     */
    suspend fun getSpendingByCategory(dateRange: DateRange): Map<Category, BigDecimal>
    
    /**
     * Get spending by account for a date range
     */
    suspend fun getSpendingByAccount(dateRange: DateRange): Map<Account, BigDecimal>
    
    /**
     * Get top merchants by spending for a date range
     */
    suspend fun getTopMerchants(dateRange: DateRange, limit: Int = 10): List<MerchantSpending>
    
    /**
     * Get monthly spending totals for trend analysis
     */
    suspend fun getMonthlySpendingTotals(months: Int): List<MonthlyTrend>
    
    /**
     * Get category spending trends over time
     */
    suspend fun getCategorySpendingTrends(
        category: Category,
        months: Int
    ): List<MonthlySpending>
    
    /**
     * Calculate average daily spending for a period
     */
    suspend fun getAverageDailySpending(dateRange: DateRange): BigDecimal
    
    /**
     * Get transaction count for a date range
     */
    suspend fun getTransactionCount(dateRange: DateRange): Int
    
    /**
     * Get largest transactions for a date range
     */
    suspend fun getLargestTransactions(
        dateRange: DateRange,
        limit: Int = 5
    ): List<Transaction>
    
    /**
     * Calculate savings rate for a period
     */
    suspend fun calculateSavingsRate(dateRange: DateRange): Float
    
    /**
     * Observe real-time analytics updates
     */
    fun observeAnalyticsUpdates(): Flow<Unit>
}