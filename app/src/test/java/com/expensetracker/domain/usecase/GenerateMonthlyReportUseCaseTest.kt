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
 * Unit tests for GenerateMonthlyReportUseCase
 * Tests monthly report generation functionality
 */
class GenerateMonthlyReportUseCaseTest {

    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var generateMonthlyReportUseCase: GenerateMonthlyReportUseCase

    @Before
    fun setup() {
        analyticsRepository = mockk()
        generateMonthlyReportUseCase = GenerateMonthlyReportUseCase(analyticsRepository)
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val expectedReport = createSampleMonthlyReport(month)
        coEvery { analyticsRepository.generateMonthlyReport(month) } returns expectedReport

        // When
        val result = generateMonthlyReportUseCase(month)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedReport, result.getOrNull())
        coVerify { analyticsRepository.generateMonthlyReport(month) }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val exception = RuntimeException("Database error")
        coEvery { analyticsRepository.generateMonthlyReport(month) } throws exception

        // When
        val result = generateMonthlyReportUseCase(month)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `generateCurrentMonthReport should use current month`() = runTest {
        // Given
        val currentMonth = YearMonth.now()
        val expectedReport = createSampleMonthlyReport(currentMonth)
        coEvery { analyticsRepository.generateMonthlyReport(currentMonth) } returns expectedReport

        // When
        val result = generateMonthlyReportUseCase.generateCurrentMonthReport()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.generateMonthlyReport(currentMonth) }
    }

    @Test
    fun `generatePreviousMonthReport should use previous month`() = runTest {
        // Given
        val previousMonth = YearMonth.now().minusMonths(1)
        val expectedReport = createSampleMonthlyReport(previousMonth)
        coEvery { analyticsRepository.generateMonthlyReport(previousMonth) } returns expectedReport

        // When
        val result = generateMonthlyReportUseCase.generatePreviousMonthReport()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.generateMonthlyReport(previousMonth) }
    }

    @Test
    fun `should handle zero income and expenses correctly`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val reportWithZeros = createSampleMonthlyReport(month).copy(
            totalIncome = BigDecimal.ZERO,
            totalExpenses = BigDecimal.ZERO,
            netAmount = BigDecimal.ZERO
        )
        coEvery { analyticsRepository.generateMonthlyReport(month) } returns reportWithZeros

        // When
        val result = generateMonthlyReportUseCase(month)

        // Then
        assertTrue(result.isSuccess)
        val report = result.getOrNull()!!
        assertEquals(BigDecimal.ZERO, report.totalIncome)
        assertEquals(BigDecimal.ZERO, report.totalExpenses)
        assertEquals(BigDecimal.ZERO, report.netAmount)
    }

    @Test
    fun `should handle negative net amount correctly`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val reportWithLoss = createSampleMonthlyReport(month).copy(
            totalIncome = BigDecimal("5000"),
            totalExpenses = BigDecimal("7000"),
            netAmount = BigDecimal("-2000")
        )
        coEvery { analyticsRepository.generateMonthlyReport(month) } returns reportWithLoss

        // When
        val result = generateMonthlyReportUseCase(month)

        // Then
        assertTrue(result.isSuccess)
        val report = result.getOrNull()!!
        assertEquals(BigDecimal("5000"), report.totalIncome)
        assertEquals(BigDecimal("7000"), report.totalExpenses)
        assertEquals(BigDecimal("-2000"), report.netAmount)
    }

    private fun createSampleMonthlyReport(month: YearMonth): MonthlyReport {
        val category = Category(1, "Food", "restaurant", "#FF5722", true)
        val account = Account(
            id = 1,
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234",
            nickname = "Main Account",
            currentBalance = BigDecimal("10000"),
            createdAt = java.time.LocalDateTime.now()
        )

        return MonthlyReport(
            month = month,
            totalIncome = BigDecimal("50000"),
            totalExpenses = BigDecimal("30000"),
            netAmount = BigDecimal("20000"),
            categoryBreakdown = mapOf(category to BigDecimal("15000")),
            topMerchants = listOf(
                MerchantSpending(
                    merchant = "Amazon",
                    totalAmount = BigDecimal("5000"),
                    transactionCount = 10,
                    category = category,
                    averageAmount = BigDecimal("500")
                )
            ),
            comparisonToPreviousMonth = MonthComparison(
                incomeChange = BigDecimal("5000"),
                expenseChange = BigDecimal("2000"),
                incomeChangePercentage = 10f,
                expenseChangePercentage = 7f,
                significantChanges = emptyList()
            ),
            accountBreakdown = mapOf(
                account to AccountSummary(
                    account = account,
                    totalIncome = BigDecimal("50000"),
                    totalExpenses = BigDecimal("30000"),
                    transactionCount = 25,
                    balanceChange = BigDecimal("20000")
                )
            )
        )
    }
}