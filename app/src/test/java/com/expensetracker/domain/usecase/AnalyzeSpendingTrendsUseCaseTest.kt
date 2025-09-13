package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AnalyticsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Unit tests for AnalyzeSpendingTrendsUseCase
 * Tests spending trends analysis functionality
 */
class AnalyzeSpendingTrendsUseCaseTest {

    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var analyzeSpendingTrendsUseCase: AnalyzeSpendingTrendsUseCase

    @Before
    fun setup() {
        analyticsRepository = mockk()
        analyzeSpendingTrendsUseCase = AnalyzeSpendingTrendsUseCase(analyticsRepository)
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        val months = 12
        val expectedTrends = createSampleSpendingTrends()
        coEvery { analyticsRepository.calculateSpendingTrends(months) } returns expectedTrends

        // When
        val result = analyzeSpendingTrendsUseCase(months)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTrends, result.getOrNull())
        coVerify { analyticsRepository.calculateSpendingTrends(months) }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        // Given
        val months = 12
        val exception = RuntimeException("Database error")
        coEvery { analyticsRepository.calculateSpendingTrends(months) } throws exception

        // When
        val result = analyzeSpendingTrendsUseCase(months)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should use default 12 months when no parameter provided`() = runTest {
        // Given
        val expectedTrends = createSampleSpendingTrends()
        coEvery { analyticsRepository.calculateSpendingTrends(12) } returns expectedTrends

        // When
        val result = analyzeSpendingTrendsUseCase()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.calculateSpendingTrends(12) }
    }

    @Test
    fun `getShortTermTrends should use 6 months`() = runTest {
        // Given
        val expectedTrends = createSampleSpendingTrends()
        coEvery { analyticsRepository.calculateSpendingTrends(6) } returns expectedTrends

        // When
        val result = analyzeSpendingTrendsUseCase.getShortTermTrends()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.calculateSpendingTrends(6) }
    }

    @Test
    fun `getYearlyTrends should use 12 months`() = runTest {
        // Given
        val expectedTrends = createSampleSpendingTrends()
        coEvery { analyticsRepository.calculateSpendingTrends(12) } returns expectedTrends

        // When
        val result = analyzeSpendingTrendsUseCase.getYearlyTrends()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.calculateSpendingTrends(12) }
    }

    @Test
    fun `getLongTermTrends should use 24 months`() = runTest {
        // Given
        val expectedTrends = createSampleSpendingTrends()
        coEvery { analyticsRepository.calculateSpendingTrends(24) } returns expectedTrends

        // When
        val result = analyzeSpendingTrendsUseCase.getLongTermTrends()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.calculateSpendingTrends(24) }
    }

    @Test
    fun `analyzeCategoryTrend should return category trend when successful`() = runTest {
        // Given
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val months = 12
        val monthlyData = createSampleMonthlySpending()
        val expectedTrend = createSampleCategoryTrend(category, monthlyData)
        
        coEvery { analyticsRepository.getCategorySpendingTrends(category, months) } returns monthlyData

        // When
        val result = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(category, months)

        // Then
        assertTrue(result.isSuccess)
        val trend = result.getOrNull()!!
        assertEquals(category, trend.category)
        assertEquals(monthlyData, trend.monthlyData)
        coVerify { analyticsRepository.getCategorySpendingTrends(category, months) }
    }

    @Test
    fun `analyzeCategoryTrend should use default 12 months when not specified`() = runTest {
        // Given
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val monthlyData = createSampleMonthlySpending()
        
        coEvery { analyticsRepository.getCategorySpendingTrends(category, 12) } returns monthlyData

        // When
        val result = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(category)

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.getCategorySpendingTrends(category, 12) }
    }

    @Test
    fun `compareMonthOverMonth should calculate correct comparison`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("10000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal("8000")
        coEvery { analyticsRepository.getTotalIncome(currentMonth) } returns BigDecimal("15000")
        coEvery { analyticsRepository.getTotalIncome(previousMonth) } returns BigDecimal("12000")
        coEvery { analyticsRepository.getSpendingByCategory(currentMonth) } returns emptyMap()
        coEvery { analyticsRepository.getSpendingByCategory(previousMonth) } returns emptyMap()

        // When
        val result = analyzeSpendingTrendsUseCase.compareMonthOverMonth()

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrNull()!!
        assertEquals(BigDecimal("3000"), comparison.incomeChange)
        assertEquals(BigDecimal("2000"), comparison.expenseChange)
        assertEquals(25f, comparison.incomeChangePercentage, 0.01f)
        assertEquals(25f, comparison.expenseChangePercentage, 0.01f)
    }

    @Test
    fun `compareMonthOverMonth should handle zero previous spending`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("5000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal.ZERO
        coEvery { analyticsRepository.getTotalIncome(currentMonth) } returns BigDecimal("8000")
        coEvery { analyticsRepository.getTotalIncome(previousMonth) } returns BigDecimal.ZERO
        coEvery { analyticsRepository.getSpendingByCategory(currentMonth) } returns emptyMap()
        coEvery { analyticsRepository.getSpendingByCategory(previousMonth) } returns emptyMap()

        // When
        val result = analyzeSpendingTrendsUseCase.compareMonthOverMonth()

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrNull()!!
        assertEquals(0f, comparison.incomeChangePercentage, 0.01f)
        assertEquals(0f, comparison.expenseChangePercentage, 0.01f)
    }

    @Test
    fun `compareYearOverYear should calculate correct comparison`() = runTest {
        // Given
        val currentYearRange = DateRange.yearToDate()
        val previousYearStart = currentYearRange.startDate.minusYears(1)
        val previousYearEnd = currentYearRange.endDate.minusYears(1)
        val previousYearRange = DateRange(previousYearStart, previousYearEnd)
        
        coEvery { analyticsRepository.getTotalSpending(currentYearRange) } returns BigDecimal("120000")
        coEvery { analyticsRepository.getTotalSpending(previousYearRange) } returns BigDecimal("100000")
        coEvery { analyticsRepository.getTotalIncome(currentYearRange) } returns BigDecimal("180000")
        coEvery { analyticsRepository.getTotalIncome(previousYearRange) } returns BigDecimal("150000")

        // When
        val result = analyzeSpendingTrendsUseCase.compareYearOverYear()

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrNull()!!
        assertEquals(BigDecimal("120000"), comparison.currentYearSpending)
        assertEquals(BigDecimal("100000"), comparison.previousYearSpending)
        assertEquals(BigDecimal("20000"), comparison.spendingChange)
        assertEquals(20f, comparison.spendingChangePercentage, 0.01f)
        assertEquals(20f, comparison.incomeChangePercentage, 0.01f)
    }

    @Test
    fun `should handle empty monthly data for category trend calculation`() = runTest {
        // Given
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val emptyMonthlyData = emptyList<MonthlySpending>()
        
        coEvery { analyticsRepository.getCategorySpendingTrends(category, 12) } returns emptyMonthlyData

        // When
        val result = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(category)

        // Then
        assertTrue(result.isSuccess)
        val trend = result.getOrNull()!!
        assertEquals(BigDecimal.ZERO, trend.averageMonthlySpending)
        assertEquals(TrendDirection.STABLE, trend.trendDirection)
        assertEquals(0f, trend.volatility, 0.01f)
    }

    @Test
    fun `should calculate increasing trend direction correctly`() = runTest {
        // Given
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val increasingData = listOf(
            MonthlySpending(YearMonth.of(2024, 1), BigDecimal("1000"), 10),
            MonthlySpending(YearMonth.of(2024, 2), BigDecimal("1100"), 11),
            MonthlySpending(YearMonth.of(2024, 3), BigDecimal("1200"), 12),
            MonthlySpending(YearMonth.of(2024, 4), BigDecimal("1300"), 13),
            MonthlySpending(YearMonth.of(2024, 5), BigDecimal("1400"), 14),
            MonthlySpending(YearMonth.of(2024, 6), BigDecimal("1500"), 15)
        )
        
        coEvery { analyticsRepository.getCategorySpendingTrends(category, 12) } returns increasingData

        // When
        val result = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(category)

        // Then
        assertTrue(result.isSuccess)
        val trend = result.getOrNull()!!
        assertEquals(TrendDirection.INCREASING, trend.trendDirection)
    }

    @Test
    fun `should calculate decreasing trend direction correctly`() = runTest {
        // Given
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val decreasingData = listOf(
            MonthlySpending(YearMonth.of(2024, 1), BigDecimal("1500"), 15),
            MonthlySpending(YearMonth.of(2024, 2), BigDecimal("1400"), 14),
            MonthlySpending(YearMonth.of(2024, 3), BigDecimal("1300"), 13),
            MonthlySpending(YearMonth.of(2024, 4), BigDecimal("1200"), 12),
            MonthlySpending(YearMonth.of(2024, 5), BigDecimal("1100"), 11),
            MonthlySpending(YearMonth.of(2024, 6), BigDecimal("1000"), 10)
        )
        
        coEvery { analyticsRepository.getCategorySpendingTrends(category, 12) } returns decreasingData

        // When
        val result = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(category)

        // Then
        assertTrue(result.isSuccess)
        val trend = result.getOrNull()!!
        assertEquals(TrendDirection.DECREASING, trend.trendDirection)
    }

    private fun createSampleSpendingTrends(): SpendingTrends {
        return SpendingTrends(
            monthlyTrends = createSampleMonthlyTrends(),
            categoryTrends = emptyMap(),
            overallTrend = OverallTrend(
                direction = TrendDirection.INCREASING,
                averageMonthlyChange = 5f,
                consistency = 0.8f,
                volatility = 0.2f,
                savingsRate = 0.3f,
                savingsRateTrend = TrendDirection.STABLE
            ),
            seasonalPatterns = emptyList(),
            predictions = null
        )
    }

    private fun createSampleMonthlyTrends(): List<MonthlyTrend> {
        return listOf(
            MonthlyTrend(
                month = YearMonth.of(2024, 1),
                totalSpending = BigDecimal("10000"),
                totalIncome = BigDecimal("15000"),
                savingsRate = 0.33f,
                changeFromPreviousMonth = 5f,
                topCategories = emptyList()
            )
        )
    }

    private fun createSampleMonthlySpending(): List<MonthlySpending> {
        return listOf(
            MonthlySpending(YearMonth.of(2024, 1), BigDecimal("1000"), 10),
            MonthlySpending(YearMonth.of(2024, 2), BigDecimal("1100"), 11),
            MonthlySpending(YearMonth.of(2024, 3), BigDecimal("1200"), 12)
        )
    }

    private fun createSampleCategoryTrend(
        category: Category,
        monthlyData: List<MonthlySpending>
    ): CategoryTrend {
        return CategoryTrend(
            category = category,
            monthlyData = monthlyData,
            averageMonthlySpending = BigDecimal("1100"),
            trendDirection = TrendDirection.INCREASING,
            volatility = 0.1f,
            seasonality = 0.05f
        )
    }
}