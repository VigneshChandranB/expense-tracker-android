package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.CategoryBreakdown
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.repository.AnalyticsRepository
import javax.inject.Inject

/**
 * Use case for analyzing category breakdown and spending patterns
 * Implements requirement 5.2: Category breakdown charts with percentage and absolute values
 */
class AnalyzeCategoryBreakdownUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    /**
     * Analyze category breakdown for a specific date range
     * 
     * @param dateRange The period to analyze
     * @return CategoryBreakdown with detailed spending analysis by category
     */
    suspend operator fun invoke(dateRange: DateRange): Result<CategoryBreakdown> {
        return try {
            val breakdown = analyticsRepository.getCategoryBreakdown(dateRange)
            Result.success(breakdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get category breakdown for the current month
     */
    suspend fun getCurrentMonthBreakdown(): Result<CategoryBreakdown> {
        return invoke(DateRange.currentMonth())
    }
    
    /**
     * Get category breakdown for the previous month
     */
    suspend fun getPreviousMonthBreakdown(): Result<CategoryBreakdown> {
        return invoke(DateRange.previousMonth())
    }
    
    /**
     * Get category breakdown for the last 30 days
     */
    suspend fun getLast30DaysBreakdown(): Result<CategoryBreakdown> {
        return invoke(DateRange.lastNDays(30))
    }
    
    /**
     * Get category breakdown for year to date
     */
    suspend fun getYearToDateBreakdown(): Result<CategoryBreakdown> {
        return invoke(DateRange.yearToDate())
    }
    
    /**
     * Compare category breakdown between two periods
     */
    suspend fun compareBreakdowns(
        currentPeriod: DateRange,
        previousPeriod: DateRange
    ): Result<Pair<CategoryBreakdown, CategoryBreakdown>> {
        return try {
            val currentBreakdown = analyticsRepository.getCategoryBreakdown(currentPeriod)
            val previousBreakdown = analyticsRepository.getCategoryBreakdown(previousPeriod)
            Result.success(Pair(currentBreakdown, previousBreakdown))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}