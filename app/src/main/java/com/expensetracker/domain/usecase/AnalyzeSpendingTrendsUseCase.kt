package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AnalyticsRepository
import javax.inject.Inject

/**
 * Use case for analyzing spending trends and patterns over time
 * Implements requirement 5.3: Month-over-month and year-over-year comparisons
 * Implements requirement 5.5: Transaction trends analysis
 */
class AnalyzeSpendingTrendsUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    /**
     * Calculate comprehensive spending trends for the specified number of months
     * 
     * @param months Number of months to analyze (default: 12)
     * @return SpendingTrends containing detailed trend analysis
     */
    suspend operator fun invoke(months: Int = 12): Result<SpendingTrends> {
        return try {
            val trends = analyticsRepository.calculateSpendingTrends(months)
            Result.success(trends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get spending trends for the last 6 months
     */
    suspend fun getShortTermTrends(): Result<SpendingTrends> {
        return invoke(6)
    }
    
    /**
     * Get spending trends for the last 12 months
     */
    suspend fun getYearlyTrends(): Result<SpendingTrends> {
        return invoke(12)
    }
    
    /**
     * Get spending trends for the last 24 months for long-term analysis
     */
    suspend fun getLongTermTrends(): Result<SpendingTrends> {
        return invoke(24)
    }
    
    /**
     * Analyze category-specific spending trends
     * 
     * @param category The category to analyze
     * @param months Number of months to analyze
     * @return CategoryTrend for the specified category
     */
    suspend fun analyzeCategoryTrend(
        category: Category,
        months: Int = 12
    ): Result<CategoryTrend> {
        return try {
            val monthlyData = analyticsRepository.getCategorySpendingTrends(category, months)
            val categoryTrend = calculateCategoryTrend(category, monthlyData)
            Result.success(categoryTrend)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Compare current month trends with previous periods
     * Implements requirement 5.3: Month-over-month comparisons
     */
    suspend fun compareMonthOverMonth(): Result<MonthComparison> {
        return try {
            val currentMonth = DateRange.currentMonth()
            val previousMonth = DateRange.previousMonth()
            
            val currentSpending = analyticsRepository.getTotalSpending(currentMonth)
            val previousSpending = analyticsRepository.getTotalSpending(previousMonth)
            val currentIncome = analyticsRepository.getTotalIncome(currentMonth)
            val previousIncome = analyticsRepository.getTotalIncome(previousMonth)
            
            val incomeChange = currentIncome - previousIncome
            val expenseChange = currentSpending - previousSpending
            
            val incomeChangePercentage = if (previousIncome.compareTo(java.math.BigDecimal.ZERO) != 0) {
                (incomeChange.divide(previousIncome, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100)
            } else 0f
            
            val expenseChangePercentage = if (previousSpending.compareTo(java.math.BigDecimal.ZERO) != 0) {
                (expenseChange.divide(previousSpending, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100)
            } else 0f
            
            // Get category-wise changes
            val currentCategorySpending = analyticsRepository.getSpendingByCategory(currentMonth)
            val previousCategorySpending = analyticsRepository.getSpendingByCategory(previousMonth)
            
            val significantChanges = calculateSignificantCategoryChanges(
                currentCategorySpending,
                previousCategorySpending
            )
            
            val comparison = MonthComparison(
                incomeChange = incomeChange,
                expenseChange = expenseChange,
                incomeChangePercentage = incomeChangePercentage,
                expenseChangePercentage = expenseChangePercentage,
                significantChanges = significantChanges
            )
            
            Result.success(comparison)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate year-over-year comparison
     * Implements requirement 5.3: Year-over-year comparisons
     */
    suspend fun compareYearOverYear(): Result<YearComparison> {
        return try {
            val currentYearRange = DateRange.yearToDate()
            val previousYearStart = currentYearRange.startDate.minusYears(1)
            val previousYearEnd = currentYearRange.endDate.minusYears(1)
            val previousYearRange = DateRange(previousYearStart, previousYearEnd)
            
            val currentYearSpending = analyticsRepository.getTotalSpending(currentYearRange)
            val previousYearSpending = analyticsRepository.getTotalSpending(previousYearRange)
            val currentYearIncome = analyticsRepository.getTotalIncome(currentYearRange)
            val previousYearIncome = analyticsRepository.getTotalIncome(previousYearRange)
            
            val spendingChange = currentYearSpending - previousYearSpending
            val incomeChange = currentYearIncome - previousYearIncome
            
            val spendingChangePercentage = if (previousYearSpending.compareTo(java.math.BigDecimal.ZERO) != 0) {
                (spendingChange.divide(previousYearSpending, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100)
            } else 0f
            
            val incomeChangePercentage = if (previousYearIncome.compareTo(java.math.BigDecimal.ZERO) != 0) {
                (incomeChange.divide(previousYearIncome, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100)
            } else 0f
            
            val comparison = YearComparison(
                currentYearSpending = currentYearSpending,
                previousYearSpending = previousYearSpending,
                currentYearIncome = currentYearIncome,
                previousYearIncome = previousYearIncome,
                spendingChange = spendingChange,
                incomeChange = incomeChange,
                spendingChangePercentage = spendingChangePercentage,
                incomeChangePercentage = incomeChangePercentage
            )
            
            Result.success(comparison)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate category trend from monthly data
     */
    private fun calculateCategoryTrend(
        category: Category,
        monthlyData: List<MonthlySpending>
    ): CategoryTrend {
        val averageMonthlySpending = if (monthlyData.isNotEmpty()) {
            monthlyData.map { it.amount }.reduce { acc, amount -> acc + amount }
                .divide(java.math.BigDecimal(monthlyData.size), 2, java.math.RoundingMode.HALF_UP)
        } else {
            java.math.BigDecimal.ZERO
        }
        
        val trendDirection = calculateTrendDirection(monthlyData)
        val volatility = calculateVolatility(monthlyData)
        val seasonality = calculateSeasonality(monthlyData)
        
        return CategoryTrend(
            category = category,
            monthlyData = monthlyData,
            averageMonthlySpending = averageMonthlySpending,
            trendDirection = trendDirection,
            volatility = volatility,
            seasonality = seasonality
        )
    }
    
    /**
     * Calculate trend direction from monthly spending data
     */
    private fun calculateTrendDirection(monthlyData: List<MonthlySpending>): TrendDirection {
        if (monthlyData.size < 2) return TrendDirection.STABLE
        
        val recentMonths = monthlyData.takeLast(3)
        val earlierMonths = monthlyData.dropLast(3).takeLast(3)
        
        if (recentMonths.isEmpty() || earlierMonths.isEmpty()) return TrendDirection.STABLE
        
        val recentAverage = recentMonths.map { it.amount }.reduce { acc, amount -> acc + amount }
            .divide(java.math.BigDecimal(recentMonths.size), 2, java.math.RoundingMode.HALF_UP)
        
        val earlierAverage = earlierMonths.map { it.amount }.reduce { acc, amount -> acc + amount }
            .divide(java.math.BigDecimal(earlierMonths.size), 2, java.math.RoundingMode.HALF_UP)
        
        val changePercentage = if (earlierAverage.compareTo(java.math.BigDecimal.ZERO) != 0) {
            (recentAverage - earlierAverage).divide(earlierAverage, 4, java.math.RoundingMode.HALF_UP).toFloat()
        } else 0f
        
        return when {
            changePercentage > 0.1f -> TrendDirection.INCREASING
            changePercentage < -0.1f -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    /**
     * Calculate volatility (standard deviation) of spending
     */
    private fun calculateVolatility(monthlyData: List<MonthlySpending>): Float {
        if (monthlyData.size < 2) return 0f
        
        val amounts = monthlyData.map { it.amount.toFloat() }
        val mean = amounts.average().toFloat()
        val variance = amounts.map { (it - mean) * (it - mean) }.average()
        
        return kotlin.math.sqrt(variance).toFloat()
    }
    
    /**
     * Calculate seasonality score (simplified)
     */
    private fun calculateSeasonality(monthlyData: List<MonthlySpending>): Float {
        if (monthlyData.size < 12) return 0f
        
        // Group by month of year and calculate variance
        val monthlyAverages = monthlyData.groupBy { it.month.monthValue }
            .mapValues { (_, values) ->
                values.map { it.amount.toFloat() }.average().toFloat()
            }
        
        if (monthlyAverages.size < 4) return 0f
        
        val overallMean = monthlyAverages.values.average().toFloat()
        val seasonalVariance = monthlyAverages.values.map { (it - overallMean) * (it - overallMean) }.average()
        
        return kotlin.math.sqrt(seasonalVariance).toFloat()
    }
    
    /**
     * Calculate significant category changes between periods
     */
    private fun calculateSignificantCategoryChanges(
        currentSpending: Map<Category, java.math.BigDecimal>,
        previousSpending: Map<Category, java.math.BigDecimal>
    ): List<CategoryChange> {
        val changes = mutableListOf<CategoryChange>()
        
        // Get all categories from both periods
        val allCategories = (currentSpending.keys + previousSpending.keys).distinct()
        
        for (category in allCategories) {
            val currentAmount = currentSpending[category] ?: java.math.BigDecimal.ZERO
            val previousAmount = previousSpending[category] ?: java.math.BigDecimal.ZERO
            val changeAmount = currentAmount - previousAmount
            
            val changePercentage = if (previousAmount.compareTo(java.math.BigDecimal.ZERO) != 0) {
                changeAmount.divide(previousAmount, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100
            } else if (currentAmount.compareTo(java.math.BigDecimal.ZERO) != 0) {
                100f // New spending in this category
            } else {
                0f
            }
            
            // Consider changes significant if they're > 20% or > ₹1000
            if (kotlin.math.abs(changePercentage) > 20f || 
                changeAmount.abs().compareTo(java.math.BigDecimal("1000")) > 0) {
                changes.add(
                    CategoryChange(
                        category = category,
                        currentAmount = currentAmount,
                        previousAmount = previousAmount,
                        changeAmount = changeAmount,
                        changePercentage = changePercentage
                    )
                )
            }
        }
        
        return changes.sortedByDescending { kotlin.math.abs(it.changePercentage) }
    }
}

/**
 * Year-over-year comparison data
 */
data class YearComparison(
    val currentYearSpending: java.math.BigDecimal,
    val previousYearSpending: java.math.BigDecimal,
    val currentYearIncome: java.math.BigDecimal,
    val previousYearIncome: java.math.BigDecimal,
    val spendingChange: java.math.BigDecimal,
    val incomeChange: java.math.BigDecimal,
    val spendingChangePercentage: Float,
    val incomeChangePercentage: Float
)