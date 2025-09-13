package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Domain model for comprehensive spending trends analysis
 * Provides insights into spending patterns over time
 */
data class SpendingTrends(
    val monthlyTrends: List<MonthlyTrend>,
    val categoryTrends: Map<Category, CategoryTrend>,
    val overallTrend: OverallTrend,
    val seasonalPatterns: List<SeasonalPattern>,
    val predictions: SpendingPrediction?
)

/**
 * Monthly spending trend data
 */
data class MonthlyTrend(
    val month: YearMonth,
    val totalSpending: BigDecimal,
    val totalIncome: BigDecimal,
    val savingsRate: Float,
    val changeFromPreviousMonth: Float,
    val topCategories: List<CategorySpending>
)

/**
 * Category-specific trend analysis
 */
data class CategoryTrend(
    val category: Category,
    val monthlyData: List<MonthlySpending>,
    val averageMonthlySpending: BigDecimal,
    val trendDirection: TrendDirection,
    val volatility: Float,
    val seasonality: Float
)

/**
 * Monthly spending data for a category
 */
data class MonthlySpending(
    val month: YearMonth,
    val amount: BigDecimal,
    val transactionCount: Int
)

/**
 * Overall spending trend summary
 */
data class OverallTrend(
    val direction: TrendDirection,
    val averageMonthlyChange: Float,
    val consistency: Float, // How consistent the trend is (0-1)
    val volatility: Float,  // How volatile spending is (0-1)
    val savingsRate: Float,
    val savingsRateTrend: TrendDirection
)

/**
 * Seasonal spending patterns
 */
data class SeasonalPattern(
    val season: Season,
    val averageSpending: BigDecimal,
    val topCategories: List<Category>,
    val spendingIncrease: Float // Percentage increase compared to average
)

enum class Season {
    SPRING, SUMMER, FALL, WINTER
}

/**
 * Spending predictions based on historical data
 */
data class SpendingPrediction(
    val nextMonthPrediction: BigDecimal,
    val confidence: Float,
    val categoryPredictions: Map<Category, BigDecimal>,
    val factors: List<String> // Factors influencing the prediction
)