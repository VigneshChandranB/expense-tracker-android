package com.expensetracker.analytics

import com.expensetracker.data.repository.AnalyticsRepositoryImpl
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.usecase.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Edge case tests for analytics engine
 * Tests handling of unusual data scenarios and boundary conditions
 */
class AnalyticsEdgeCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var analyticsRepository: AnalyticsRepositoryImpl
    
    private lateinit var generateMonthlyReportUseCase: GenerateMonthlyReportUseCase
    private lateinit var analyzeCategoryBreakdownUseCase: AnalyzeCategoryBreakdownUseCase
    private lateinit var analyzeSpendingTrendsUseCase: AnalyzeSpendingTrendsUseCase
    private lateinit var detectSpendingAnomaliesUseCase: DetectSpendingAnomaliesUseCase

    @Before
    fun setup() {
        transactionRepository = mockk()
        categoryRepository = mockk()
        analyticsRepository = AnalyticsRepositoryImpl(transactionRepository, categoryRepository)
        
        generateMonthlyReportUseCase = GenerateMonthlyReportUseCase(analyticsRepository)
        analyzeCategoryBreakdownUseCase = AnalyzeCategoryBreakdownUseCase(analyticsRepository)
        analyzeSpendingTrendsUseCase = AnalyzeSpendingTrendsUseCase(analyticsRepository)
        detectSpendingAnomaliesUseCase = DetectSpendingAnomaliesUseCase(analyticsRepository)
        
        coEvery { transactionRepository.observeTransactions() } returns flowOf(emptyList())
        coEvery { categoryRepository.getAllCategories() } returns createSampleCategories()
    }

    @Test
    fun `should handle completely empty transaction data`() = runTest {
        // Given - No transactions at all
        val month = YearMonth.of(2024, 1)
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns emptyList()

        // When - Generate monthly report
        val reportResult = generateMonthlyReportUseCase(month)

        // Then - Should handle gracefully
        assertTrue("Should succeed with empty data", reportResult.isSuccess)
        val report = reportResult.getOrNull()!!
        
        assertEquals("Total income should be zero", BigDecimal.ZERO, report.totalIncome)
        assertEquals("Total expenses should be zero", BigDecimal.ZERO, report.totalExpenses)
        assertEquals("Net amount should be zero", BigDecimal.ZERO, report.netAmount)
        assertTrue("Category breakdown should be empty", report.categoryBreakdown.isEmpty())
        assertTrue("Top merchants should be empty", report.topMerchants.isEmpty())
    }

    @Test
    fun `should handle single transaction scenarios`() = runTest {
        // Given - Only one transaction
        val singleTransaction = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Single Restaurant",
                description = "Only transaction",
                date = LocalDateTime.of(2024, 1, 15, 12, 0),
                source = TransactionSource.MANUAL,
                accountId = 1
            )
        )
        
        val month = YearMonth.of(2024, 1)
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns singleTransaction

        // When - Analyze category breakdown
        val dateRange = DateRange(month.atDay(1), month.atEndOfMonth())
        val breakdownResult = analyzeCategoryBreakdownUseCase(dateRange)

        // Then - Should handle single transaction correctly
        assertTrue("Should succeed with single transaction", breakdownResult.isSuccess)
        val breakdown = breakdownResult.getOrNull()!!
        
        assertEquals("Total should equal single transaction", BigDecimal("1000"), breakdown.totalAmount)
        assertEquals("Should have one category", 1, breakdown.categorySpending.size)
        assertEquals("Percentage should be 100%", 100f, breakdown.categorySpending.first().percentage, 0.01f)
        assertEquals("Transaction count should be 1", 1, breakdown.categorySpending.first().transactionCount)
    }

    @Test
    fun `should handle zero amount transactions`() = runTest {
        // Given - Transactions with zero amounts
        val zeroAmountTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal.ZERO,
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Free Sample",
                description = "Zero cost transaction",
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Regular Store",
                description = "Normal transaction",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
        
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns zeroAmountTransactions

        // When - Get total spending
        val totalSpending = analyticsRepository.getTotalSpending(dateRange)

        // Then - Should only count non-zero transactions
        assertEquals("Should exclude zero amount transactions", BigDecimal("1000"), totalSpending)
    }

    @Test
    fun `should handle very large transaction amounts`() = runTest {
        // Given - Extremely large transaction amounts
        val largeAmountTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("999999999.99"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Expensive Store",
                description = "Very expensive purchase",
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("1000000000.00"),
                type = TransactionType.INCOME,
                category = createIncomeCategory(),
                merchant = "Lottery Win",
                description = "Jackpot",
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1
            )
        )
        
        val month = YearMonth.now()
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns largeAmountTransactions

        // When - Generate monthly report
        val reportResult = generateMonthlyReportUseCase(month)

        // Then - Should handle large amounts without overflow
        assertTrue("Should handle large amounts", reportResult.isSuccess)
        val report = reportResult.getOrNull()!!
        
        assertEquals("Income should be correct", BigDecimal("1000000000.00"), report.totalIncome)
        assertEquals("Expenses should be correct", BigDecimal("999999999.99"), report.totalExpenses)
        assertTrue("Net amount should be positive", report.netAmount > BigDecimal.ZERO)
    }

    @Test
    fun `should handle transactions with very small decimal amounts`() = runTest {
        // Given - Transactions with very small amounts
        val smallAmountTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("0.01"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Penny Store",
                description = "Very small purchase",
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("0.001"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Micro Store",
                description = "Micro transaction",
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1
            )
        )
        
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns smallAmountTransactions

        // When - Calculate category breakdown
        val breakdownResult = analyzeCategoryBreakdownUseCase(dateRange)

        // Then - Should handle small amounts with proper precision
        assertTrue("Should handle small amounts", breakdownResult.isSuccess)
        val breakdown = breakdownResult.getOrNull()!!
        
        assertEquals("Total should be sum of small amounts", BigDecimal("0.011"), breakdown.totalAmount)
        assertTrue("Should have category data", breakdown.categorySpending.isNotEmpty())
        
        // Verify percentages are calculated correctly for small amounts
        val totalPercentage = breakdown.categorySpending.sumOf { it.percentage.toDouble() }.toFloat()
        assertEquals("Percentages should sum to 100%", 100f, totalPercentage, 1f)
    }

    @Test
    fun `should handle transactions spanning leap year boundaries`() = runTest {
        // Given - Transactions around leap year (February 29, 2024)
        val leapYearTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Feb 28 Restaurant",
                description = "Day before leap day",
                date = LocalDateTime.of(2024, 2, 28, 12, 0),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("1500"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Feb 29 Store",
                description = "Leap day transaction",
                date = LocalDateTime.of(2024, 2, 29, 15, 0),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 3,
                amount = BigDecimal("2000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Mar 1 Restaurant",
                description = "Day after leap day",
                date = LocalDateTime.of(2024, 3, 1, 18, 0),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
        
        // February 2024 (leap year)
        val febMonth = YearMonth.of(2024, 2)
        val febStartDate = febMonth.atDay(1).atStartOfDay()
        val febEndDate = febMonth.atEndOfMonth().atTime(23, 59, 59) // Should be Feb 29
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(febStartDate, febEndDate) 
        } returns leapYearTransactions.filter { it.date.monthValue == 2 }

        // When - Generate February report
        val reportResult = generateMonthlyReportUseCase(febMonth)

        // Then - Should correctly handle leap year
        assertTrue("Should handle leap year", reportResult.isSuccess)
        val report = reportResult.getOrNull()!!
        
        assertEquals("Should include leap day transactions", BigDecimal("2500"), report.totalExpenses)
        assertEquals("Should be February", 2, report.month.monthValue)
        assertEquals("Should be 2024", 2024, report.month.year)
    }

    @Test
    fun `should handle transactions with identical timestamps`() = runTest {
        // Given - Multiple transactions at exact same time
        val sameTimestamp = LocalDateTime.of(2024, 1, 15, 12, 30, 45)
        val identicalTimestampTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant A",
                description = "First transaction",
                date = sameTimestamp,
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant A",
                description = "Duplicate transaction",
                date = sameTimestamp,
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 3,
                amount = BigDecimal("500"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Store B",
                description = "Different merchant same time",
                date = sameTimestamp,
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
        
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns identicalTimestampTransactions

        // When - Detect anomalies (should detect potential duplicates)
        val config = AnomalyDetectionConfig(
            largeTransactionThreshold = BigDecimal("5000"),
            duplicateTransactionTimeWindow = 5
        )
        val anomaliesResult = detectSpendingAnomaliesUseCase(dateRange, config)

        // Then - Should detect duplicate transactions
        assertTrue("Should detect anomalies", anomaliesResult.isSuccess)
        val anomalies = anomaliesResult.getOrNull()!!
        
        val duplicateAnomalies = anomalies.filter { it.type == AnomalyType.DUPLICATE_TRANSACTIONS }
        assertTrue("Should detect duplicate transactions", duplicateAnomalies.isNotEmpty())
    }

    @Test
    fun `should handle missing or null category data gracefully`() = runTest {
        // Given - Empty categories list
        coEvery { categoryRepository.getAllCategories() } returns emptyList()
        
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant",
                description = "Transaction with category",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
        
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When - Analyze category breakdown
        val breakdownResult = analyzeCategoryBreakdownUseCase(dateRange)

        // Then - Should handle missing categories gracefully
        assertTrue("Should handle missing categories", breakdownResult.isSuccess)
        val breakdown = breakdownResult.getOrNull()!!
        
        assertTrue("Should have transaction data", breakdown.categorySpending.isNotEmpty())
        assertTrue("All categories should be unused when none defined", breakdown.unusedCategories.isEmpty())
    }

    @Test
    fun `should handle extreme date ranges`() = runTest {
        // Given - Very wide date range (10 years)
        val extremeDateRange = DateRange(
            startDate = java.time.LocalDate.of(2014, 1, 1),
            endDate = java.time.LocalDate.of(2024, 12, 31)
        )
        
        val transactions = createSampleTransactions()
        val startDate = extremeDateRange.startDate.atStartOfDay()
        val endDate = extremeDateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When - Calculate average daily spending
        val averageDailySpending = analyticsRepository.getAverageDailySpending(extremeDateRange)

        // Then - Should handle large date range
        assertTrue("Average should be calculated", averageDailySpending >= BigDecimal.ZERO)
        
        // Verify calculation is reasonable (total spending / days)
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(
            extremeDateRange.startDate, 
            extremeDateRange.endDate
        ) + 1
        val expectedAverage = BigDecimal("3000").divide(
            BigDecimal(totalDays), 
            2, 
            java.math.RoundingMode.HALF_UP
        )
        assertEquals("Average should be correct", expectedAverage, averageDailySpending)
    }

    @Test
    fun `should handle future date transactions`() = runTest {
        // Given - Transactions with future dates
        val futureTransactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Future Restaurant",
                description = "Future transaction",
                date = LocalDateTime.now().plusDays(30),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("500"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Present Store",
                description = "Current transaction",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
        
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns futureTransactions

        // When - Calculate totals
        val totalSpending = analyticsRepository.getTotalSpending(dateRange)

        // Then - Should include all transactions in range (even future ones if in range)
        assertTrue("Should handle future dates", totalSpending >= BigDecimal.ZERO)
    }

    @Test
    fun `should handle division by zero scenarios in calculations`() = runTest {
        // Given - Scenario that could cause division by zero
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        
        // Current month has spending, previous month has zero
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("5000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal.ZERO
        coEvery { analyticsRepository.getTotalIncome(currentMonth) } returns BigDecimal("8000")
        coEvery { analyticsRepository.getTotalIncome(previousMonth) } returns BigDecimal.ZERO
        coEvery { analyticsRepository.getSpendingByCategory(any()) } returns emptyMap()

        // When - Compare month over month (potential division by zero)
        val comparisonResult = analyzeSpendingTrendsUseCase.compareMonthOverMonth()

        // Then - Should handle division by zero gracefully
        assertTrue("Should handle division by zero", comparisonResult.isSuccess)
        val comparison = comparisonResult.getOrNull()!!
        
        assertEquals("Income change percentage should be 0 when previous is 0", 0f, comparison.incomeChangePercentage, 0.01f)
        assertEquals("Expense change percentage should be 0 when previous is 0", 0f, comparison.expenseChangePercentage, 0.01f)
    }

    private fun createSampleTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("2000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant",
                description = "Dinner",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Store",
                description = "Shopping",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
    }

    private fun createSampleCategories(): List<Category> {
        return listOf(
            createFoodCategory(),
            createShoppingCategory(),
            createIncomeCategory()
        )
    }

    private fun createFoodCategory(): Category {
        return Category(1, "Food", "restaurant", "#FF5722", true)
    }

    private fun createShoppingCategory(): Category {
        return Category(2, "Shopping", "shopping_cart", "#2196F3", true)
    }

    private fun createIncomeCategory(): Category {
        return Category(8, "Income", "attach_money", "#4CAF50", true)
    }
}