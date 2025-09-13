package com.expensetracker.data.repository

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.TransactionRepository
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
 * Unit tests for AnalyticsRepositoryImpl
 * Tests analytics calculations and data aggregation
 */
class AnalyticsRepositoryTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var analyticsRepository: AnalyticsRepositoryImpl

    @Before
    fun setup() {
        transactionRepository = mockk()
        categoryRepository = mockk()
        analyticsRepository = AnalyticsRepositoryImpl(transactionRepository, categoryRepository)
    }

    @Test
    fun `generateMonthlyReport should calculate totals correctly`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        val transactions = createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val report = analyticsRepository.generateMonthlyReport(month)

        // Then
        assertEquals(month, report.month)
        assertEquals(BigDecimal("15000"), report.totalIncome)
        assertEquals(BigDecimal("8000"), report.totalExpenses)
        assertEquals(BigDecimal("7000"), report.netAmount)
        assertFalse(report.categoryBreakdown.isEmpty())
        assertFalse(report.topMerchants.isEmpty())
    }

    @Test
    fun `generateMonthlyReport should handle empty transactions`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns emptyList()

        // When
        val report = analyticsRepository.generateMonthlyReport(month)

        // Then
        assertEquals(BigDecimal.ZERO, report.totalIncome)
        assertEquals(BigDecimal.ZERO, report.totalExpenses)
        assertEquals(BigDecimal.ZERO, report.netAmount)
        assertTrue(report.categoryBreakdown.isEmpty())
        assertTrue(report.topMerchants.isEmpty())
    }

    @Test
    fun `getCategoryBreakdown should calculate percentages correctly`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions
        coEvery { categoryRepository.getAllCategories() } returns createSampleCategories()

        // When
        val breakdown = analyticsRepository.getCategoryBreakdown(dateRange)

        // Then
        assertEquals(BigDecimal("8000"), breakdown.totalAmount)
        assertEquals(2, breakdown.categorySpending.size)
        
        val foodSpending = breakdown.categorySpending.find { it.category.name == "Food" }
        assertNotNull(foodSpending)
        assertEquals(62.5f, foodSpending!!.percentage, 0.01f)
        
        val shoppingSpending = breakdown.categorySpending.find { it.category.name == "Shopping" }
        assertNotNull(shoppingSpending)
        assertEquals(37.5f, shoppingSpending!!.percentage, 0.01f)
    }

    @Test
    fun `getCategoryBreakdown should handle zero total amount`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns emptyList()
        coEvery { categoryRepository.getAllCategories() } returns createSampleCategories()

        // When
        val breakdown = analyticsRepository.getCategoryBreakdown(dateRange)

        // Then
        assertEquals(BigDecimal.ZERO, breakdown.totalAmount)
        assertTrue(breakdown.categorySpending.isEmpty())
        assertEquals(2, breakdown.unusedCategories.size)
    }

    @Test
    fun `getTotalSpending should sum expense transactions only`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val totalSpending = analyticsRepository.getTotalSpending(dateRange)

        // Then
        assertEquals(BigDecimal("8000"), totalSpending)
    }

    @Test
    fun `getTotalIncome should sum income transactions only`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val totalIncome = analyticsRepository.getTotalIncome(dateRange)

        // Then
        assertEquals(BigDecimal("15000"), totalIncome)
    }

    @Test
    fun `getSpendingByCategory should group expenses by category`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val spendingByCategory = analyticsRepository.getSpendingByCategory(dateRange)

        // Then
        assertEquals(2, spendingByCategory.size)
        assertEquals(BigDecimal("5000"), spendingByCategory[createFoodCategory()])
        assertEquals(BigDecimal("3000"), spendingByCategory[createShoppingCategory()])
    }

    @Test
    fun `getTopMerchants should return merchants sorted by spending`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val topMerchants = analyticsRepository.getTopMerchants(dateRange, 5)

        // Then
        assertEquals(2, topMerchants.size)
        assertEquals("Restaurant A", topMerchants[0].merchant)
        assertEquals(BigDecimal("5000"), topMerchants[0].totalAmount)
        assertEquals("Amazon", topMerchants[1].merchant)
        assertEquals(BigDecimal("3000"), topMerchants[1].totalAmount)
    }

    @Test
    fun `calculateSavingsRate should calculate correctly`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val savingsRate = analyticsRepository.calculateSavingsRate(dateRange)

        // Then
        // (15000 - 8000) / 15000 = 0.4667
        assertEquals(0.4667f, savingsRate, 0.001f)
    }

    @Test
    fun `calculateSavingsRate should return zero when no income`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val expenseOnlyTransactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns expenseOnlyTransactions

        // When
        val savingsRate = analyticsRepository.calculateSavingsRate(dateRange)

        // Then
        assertEquals(0f, savingsRate, 0.001f)
    }

    @Test
    fun `getAverageDailySpending should calculate correctly`() = runTest {
        // Given
        val dateRange = DateRange(
            startDate = java.time.LocalDate.of(2024, 1, 1),
            endDate = java.time.LocalDate.of(2024, 1, 10) // 10 days
        )
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions() // Total: 8000
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val averageDailySpending = analyticsRepository.getAverageDailySpending(dateRange)

        // Then
        // 8000 / 10 days = 800
        assertEquals(BigDecimal("800.00"), averageDailySpending)
    }

    @Test
    fun `getTransactionCount should return correct count`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val count = analyticsRepository.getTransactionCount(dateRange)

        // Then
        assertEquals(4, count)
    }

    @Test
    fun `getLargestTransactions should return sorted transactions`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val largestTransactions = analyticsRepository.getLargestTransactions(dateRange, 2)

        // Then
        assertEquals(2, largestTransactions.size)
        assertEquals(BigDecimal("5000"), largestTransactions[0].amount)
        assertEquals(BigDecimal("3000"), largestTransactions[1].amount)
    }

    @Test
    fun `observeAnalyticsUpdates should return flow`() = runTest {
        // Given
        coEvery { transactionRepository.observeTransactions() } returns flowOf(emptyList())

        // When
        val flow = analyticsRepository.observeAnalyticsUpdates()

        // Then
        assertNotNull(flow)
    }

    @Test
    fun `detectSpendingAnomalies should detect large transactions`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val config = AnomalyDetectionConfig(
            largeTransactionThreshold = BigDecimal("4000")
        )
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = createSampleExpenseTransactions()
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions
        coEvery { analyticsRepository.getSpendingByCategory(any()) } returns emptyMap()

        // When
        val anomalies = analyticsRepository.detectSpendingAnomalies(dateRange, config)

        // Then
        val largeTransactionAnomalies = anomalies.filter { 
            it.type == AnomalyType.UNUSUAL_LARGE_TRANSACTION 
        }
        assertEquals(1, largeTransactionAnomalies.size)
        assertEquals(BigDecimal("5000"), largeTransactionAnomalies[0].actualValue)
    }

    private fun createSampleTransactions(): List<Transaction> {
        return listOf(
            // Income transactions
            Transaction(
                id = 1,
                amount = BigDecimal("10000"),
                type = TransactionType.INCOME,
                category = createSalaryCategory(),
                merchant = "Company Salary",
                description = "Monthly salary",
                date = LocalDateTime.of(2024, 1, 1, 10, 0),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("5000"),
                type = TransactionType.INCOME,
                category = createSalaryCategory(),
                merchant = "Freelance Work",
                description = "Project payment",
                date = LocalDateTime.of(2024, 1, 15, 14, 0),
                source = TransactionSource.MANUAL,
                accountId = 1
            ),
            // Expense transactions
            Transaction(
                id = 3,
                amount = BigDecimal("5000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant A",
                description = "Dinner",
                date = LocalDateTime.of(2024, 1, 10, 19, 30),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 4,
                amount = BigDecimal("3000"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Amazon",
                description = "Online shopping",
                date = LocalDateTime.of(2024, 1, 20, 16, 45),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
    }

    private fun createSampleExpenseTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = 3,
                amount = BigDecimal("5000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Restaurant A",
                description = "Dinner",
                date = LocalDateTime.of(2024, 1, 10, 19, 30),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            Transaction(
                id = 4,
                amount = BigDecimal("3000"),
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Amazon",
                description = "Online shopping",
                date = LocalDateTime.of(2024, 1, 20, 16, 45),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            )
        )
    }

    private fun createSampleCategories(): List<Category> {
        return listOf(
            createFoodCategory(),
            createShoppingCategory()
        )
    }

    private fun createFoodCategory(): Category {
        return Category(
            id = 1,
            name = "Food",
            icon = "restaurant",
            color = "#FF5722",
            isDefault = true
        )
    }

    private fun createShoppingCategory(): Category {
        return Category(
            id = 2,
            name = "Shopping",
            icon = "shopping_cart",
            color = "#2196F3",
            isDefault = true
        )
    }

    private fun createSalaryCategory(): Category {
        return Category(
            id = 8,
            name = "Salary",
            icon = "attach_money",
            color = "#4CAF50",
            isDefault = true
        )
    }
}