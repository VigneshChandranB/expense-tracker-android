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

/**
 * Unit tests for AnalyzeCategoryBreakdownUseCase
 * Tests category breakdown analysis functionality
 */
class AnalyzeCategoryBreakdownUseCaseTest {

    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var analyzeCategoryBreakdownUseCase: AnalyzeCategoryBreakdownUseCase

    @Before
    fun setup() {
        analyticsRepository = mockk()
        analyzeCategoryBreakdownUseCase = AnalyzeCategoryBreakdownUseCase(analyticsRepository)
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val expectedBreakdown = createSampleCategoryBreakdown(dateRange)
        coEvery { analyticsRepository.getCategoryBreakdown(dateRange) } returns expectedBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase(dateRange)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedBreakdown, result.getOrNull())
        coVerify { analyticsRepository.getCategoryBreakdown(dateRange) }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val exception = RuntimeException("Database error")
        coEvery { analyticsRepository.getCategoryBreakdown(dateRange) } throws exception

        // When
        val result = analyzeCategoryBreakdownUseCase(dateRange)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getCurrentMonthBreakdown should use current month date range`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val expectedBreakdown = createSampleCategoryBreakdown(currentMonth)
        coEvery { analyticsRepository.getCategoryBreakdown(currentMonth) } returns expectedBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase.getCurrentMonthBreakdown()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.getCategoryBreakdown(currentMonth) }
    }

    @Test
    fun `getPreviousMonthBreakdown should use previous month date range`() = runTest {
        // Given
        val previousMonth = DateRange.previousMonth()
        val expectedBreakdown = createSampleCategoryBreakdown(previousMonth)
        coEvery { analyticsRepository.getCategoryBreakdown(previousMonth) } returns expectedBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase.getPreviousMonthBreakdown()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.getCategoryBreakdown(previousMonth) }
    }

    @Test
    fun `getLast30DaysBreakdown should use last 30 days date range`() = runTest {
        // Given
        val last30Days = DateRange.lastNDays(30)
        val expectedBreakdown = createSampleCategoryBreakdown(last30Days)
        coEvery { analyticsRepository.getCategoryBreakdown(last30Days) } returns expectedBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase.getLast30DaysBreakdown()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.getCategoryBreakdown(last30Days) }
    }

    @Test
    fun `getYearToDateBreakdown should use year to date range`() = runTest {
        // Given
        val yearToDate = DateRange.yearToDate()
        val expectedBreakdown = createSampleCategoryBreakdown(yearToDate)
        coEvery { analyticsRepository.getCategoryBreakdown(yearToDate) } returns expectedBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase.getYearToDateBreakdown()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.getCategoryBreakdown(yearToDate) }
    }

    @Test
    fun `compareBreakdowns should return both breakdowns when successful`() = runTest {
        // Given
        val currentPeriod = DateRange.currentMonth()
        val previousPeriod = DateRange.previousMonth()
        val currentBreakdown = createSampleCategoryBreakdown(currentPeriod)
        val previousBreakdown = createSampleCategoryBreakdown(previousPeriod)
        
        coEvery { analyticsRepository.getCategoryBreakdown(currentPeriod) } returns currentBreakdown
        coEvery { analyticsRepository.getCategoryBreakdown(previousPeriod) } returns previousBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase.compareBreakdowns(currentPeriod, previousPeriod)

        // Then
        assertTrue(result.isSuccess)
        val (current, previous) = result.getOrNull()!!
        assertEquals(currentBreakdown, current)
        assertEquals(previousBreakdown, previous)
        coVerify { analyticsRepository.getCategoryBreakdown(currentPeriod) }
        coVerify { analyticsRepository.getCategoryBreakdown(previousPeriod) }
    }

    @Test
    fun `compareBreakdowns should return failure when first repository call fails`() = runTest {
        // Given
        val currentPeriod = DateRange.currentMonth()
        val previousPeriod = DateRange.previousMonth()
        val exception = RuntimeException("Database error")
        
        coEvery { analyticsRepository.getCategoryBreakdown(currentPeriod) } throws exception

        // When
        val result = analyzeCategoryBreakdownUseCase.compareBreakdowns(currentPeriod, previousPeriod)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `should handle empty category breakdown correctly`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val emptyBreakdown = CategoryBreakdown(
            period = dateRange,
            totalAmount = BigDecimal.ZERO,
            categorySpending = emptyList(),
            topCategories = emptyList(),
            unusedCategories = emptyList()
        )
        coEvery { analyticsRepository.getCategoryBreakdown(dateRange) } returns emptyBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase(dateRange)

        // Then
        assertTrue(result.isSuccess)
        val breakdown = result.getOrNull()!!
        assertEquals(BigDecimal.ZERO, breakdown.totalAmount)
        assertTrue(breakdown.categorySpending.isEmpty())
        assertTrue(breakdown.topCategories.isEmpty())
    }

    @Test
    fun `should handle single category breakdown correctly`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val singleCategoryBreakdown = CategoryBreakdown(
            period = dateRange,
            totalAmount = BigDecimal("1000"),
            categorySpending = listOf(
                CategorySpending(
                    category = category,
                    amount = BigDecimal("1000"),
                    percentage = 100f,
                    transactionCount = 5,
                    averageTransactionAmount = BigDecimal("200"),
                    trend = SpendingTrend(TrendDirection.STABLE, 0f, false)
                )
            ),
            topCategories = listOf(
                CategorySpending(
                    category = category,
                    amount = BigDecimal("1000"),
                    percentage = 100f,
                    transactionCount = 5,
                    averageTransactionAmount = BigDecimal("200"),
                    trend = SpendingTrend(TrendDirection.STABLE, 0f, false)
                )
            ),
            unusedCategories = emptyList()
        )
        coEvery { analyticsRepository.getCategoryBreakdown(dateRange) } returns singleCategoryBreakdown

        // When
        val result = analyzeCategoryBreakdownUseCase(dateRange)

        // Then
        assertTrue(result.isSuccess)
        val breakdown = result.getOrNull()!!
        assertEquals(1, breakdown.categorySpending.size)
        assertEquals(100f, breakdown.categorySpending.first().percentage, 0.01f)
        assertEquals(BigDecimal("1000"), breakdown.totalAmount)
    }

    private fun createSampleCategoryBreakdown(dateRange: DateRange): CategoryBreakdown {
        val foodCategory = Category(1, "Food", "restaurant", "#FF5722", true)
        val shoppingCategory = Category(2, "Shopping", "shopping_cart", "#2196F3", true)
        val transportCategory = Category(3, "Transport", "directions_car", "#4CAF50", true)

        return CategoryBreakdown(
            period = dateRange,
            totalAmount = BigDecimal("10000"),
            categorySpending = listOf(
                CategorySpending(
                    category = foodCategory,
                    amount = BigDecimal("5000"),
                    percentage = 50f,
                    transactionCount = 20,
                    averageTransactionAmount = BigDecimal("250"),
                    trend = SpendingTrend(TrendDirection.INCREASING, 15f, true)
                ),
                CategorySpending(
                    category = shoppingCategory,
                    amount = BigDecimal("3000"),
                    percentage = 30f,
                    transactionCount = 10,
                    averageTransactionAmount = BigDecimal("300"),
                    trend = SpendingTrend(TrendDirection.STABLE, 2f, false)
                ),
                CategorySpending(
                    category = transportCategory,
                    amount = BigDecimal("2000"),
                    percentage = 20f,
                    transactionCount = 15,
                    averageTransactionAmount = BigDecimal("133.33"),
                    trend = SpendingTrend(TrendDirection.DECREASING, -10f, true)
                )
            ),
            topCategories = listOf(
                CategorySpending(
                    category = foodCategory,
                    amount = BigDecimal("5000"),
                    percentage = 50f,
                    transactionCount = 20,
                    averageTransactionAmount = BigDecimal("250"),
                    trend = SpendingTrend(TrendDirection.INCREASING, 15f, true)
                ),
                CategorySpending(
                    category = shoppingCategory,
                    amount = BigDecimal("3000"),
                    percentage = 30f,
                    transactionCount = 10,
                    averageTransactionAmount = BigDecimal("300"),
                    trend = SpendingTrend(TrendDirection.STABLE, 2f, false)
                )
            ),
            unusedCategories = emptyList()
        )
    }
}