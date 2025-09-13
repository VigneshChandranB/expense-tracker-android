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
 * Integration tests for the complete analytics engine
 * Tests the interaction between use cases and repository
 */
class AnalyticsEngineIntegrationTest {

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
        
        // Setup common mocks
        coEvery { transactionRepository.observeTransactions() } returns flowOf(emptyList())
        coEvery { categoryRepository.getAllCategories() } returns createSampleCategories()
    }

    @Test
    fun `complete analytics workflow should work end-to-end`() = runTest {
        // Given - Setup realistic transaction data for multiple months
        setupMultiMonthTransactionData()

        // When - Generate monthly report
        val monthlyReportResult = generateMonthlyReportUseCase.generateCurrentMonthReport()
        
        // Then - Verify monthly report
        assertTrue(monthlyReportResult.isSuccess)
        val monthlyReport = monthlyReportResult.getOrNull()!!
        assertTrue(monthlyReport.totalExpenses > BigDecimal.ZERO)
        assertTrue(monthlyReport.categoryBreakdown.isNotEmpty())
        assertTrue(monthlyReport.topMerchants.isNotEmpty())

        // When - Analyze category breakdown
        val categoryBreakdownResult = analyzeCategoryBreakdownUseCase.getCurrentMonthBreakdown()
        
        // Then - Verify category breakdown
        assertTrue(categoryBreakdownResult.isSuccess)
        val categoryBreakdown = categoryBreakdownResult.getOrNull()!!
        assertTrue(categoryBreakdown.categorySpending.isNotEmpty())
        assertEquals(100f, categoryBreakdown.categorySpending.sumOf { it.percentage.toDouble() }.toFloat(), 1f)

        // When - Analyze spending trends
        val trendsResult = analyzeSpendingTrendsUseCase.getShortTermTrends()
        
        // Then - Verify trends analysis
        assertTrue(trendsResult.isSuccess)
        val trends = trendsResult.getOrNull()!!
        assertTrue(trends.monthlyTrends.isNotEmpty())
        assertNotNull(trends.overallTrend)

        // When - Detect anomalies
        val anomaliesResult = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()
        
        // Then - Verify anomaly detection
        assertTrue(anomaliesResult.isSuccess)
        val anomalies = anomaliesResult.getOrNull()!!
        // Should detect the large transaction anomaly
        assertTrue(anomalies.any { it.type == AnomalyType.UNUSUAL_LARGE_TRANSACTION })
    }

    @Test
    fun `month-over-month comparison should detect spending increases`() = runTest {
        // Given - Setup data with spending increase
        setupSpendingIncreaseData()

        // When - Compare month over month
        val comparisonResult = analyzeSpendingTrendsUseCase.compareMonthOverMonth()
        
        // Then - Should detect increase
        assertTrue(comparisonResult.isSuccess)
        val comparison = comparisonResult.getOrNull()!!
        assertTrue(comparison.expenseChangePercentage > 0)
        assertTrue(comparison.expenseChange > BigDecimal.ZERO)

        // When - Check for spending alert
        val alertResult = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(0.20f)
        
        // Then - Should trigger alert for 20%+ increase
        assertTrue(alertResult.isSuccess)
        val alert = alertResult.getOrNull()
        if (comparison.expenseChangePercentage > 20f) {
            assertNotNull(alert)
            assertEquals(AnomalyType.CATEGORY_SPENDING_SPIKE, alert!!.type)
        }
    }

    @Test
    fun `category trend analysis should identify spending patterns`() = runTest {
        // Given - Setup trending category data
        setupCategoryTrendData()

        // When - Analyze food category trend
        val foodCategory = createFoodCategory()
        val trendResult = analyzeSpendingTrendsUseCase.analyzeCategoryTrend(foodCategory, 6)
        
        // Then - Should identify trend direction
        assertTrue(trendResult.isSuccess)
        val trend = trendResult.getOrNull()!!
        assertEquals(foodCategory, trend.category)
        assertTrue(trend.monthlyData.isNotEmpty())
        assertNotNull(trend.trendDirection)
        assertTrue(trend.averageMonthlySpending >= BigDecimal.ZERO)
    }

    @Test
    fun `anomaly detection should identify multiple anomaly types`() = runTest {
        // Given - Setup data with various anomalies
        setupAnomalyData()

        // When - Detect anomalies
        val anomaliesResult = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()
        
        // Then - Should detect multiple types
        assertTrue(anomaliesResult.isSuccess)
        val anomalies = anomaliesResult.getOrNull()!!
        
        val anomalyTypes = anomalies.map { it.type }.toSet()
        assertTrue(anomalyTypes.contains(AnomalyType.UNUSUAL_LARGE_TRANSACTION))
        
        // Verify severity levels are assigned correctly
        val highSeverityAnomalies = anomalies.filter { it.severity == AnomalySeverity.HIGH }
        val mediumSeverityAnomalies = anomalies.filter { it.severity == AnomalySeverity.MEDIUM }
        
        assertTrue(highSeverityAnomalies.isNotEmpty() || mediumSeverityAnomalies.isNotEmpty())
    }

    @Test
    fun `analytics should handle edge cases gracefully`() = runTest {
        // Given - Setup edge case data (empty, single transaction, etc.)
        setupEdgeCaseData()

        // When & Then - All operations should succeed without errors
        
        // Empty data case
        val emptyReportResult = generateMonthlyReportUseCase(YearMonth.of(2023, 1))
        assertTrue(emptyReportResult.isSuccess)
        val emptyReport = emptyReportResult.getOrNull()!!
        assertEquals(BigDecimal.ZERO, emptyReport.totalExpenses)

        // Single transaction case
        val singleTransactionResult = generateMonthlyReportUseCase(YearMonth.of(2024, 2))
        assertTrue(singleTransactionResult.isSuccess)
        
        // Category breakdown with no expenses
        val breakdownResult = analyzeCategoryBreakdownUseCase(
            DateRange(
                java.time.LocalDate.of(2023, 1, 1),
                java.time.LocalDate.of(2023, 1, 31)
            )
        )
        assertTrue(breakdownResult.isSuccess)
        val breakdown = breakdownResult.getOrNull()!!
        assertEquals(BigDecimal.ZERO, breakdown.totalAmount)
    }

    @Test
    fun `performance should be acceptable for large datasets`() = runTest {
        // Given - Setup large dataset
        setupLargeDataset()

        // When - Measure performance of analytics operations
        val startTime = System.currentTimeMillis()
        
        val reportResult = generateMonthlyReportUseCase.generateCurrentMonthReport()
        val breakdownResult = analyzeCategoryBreakdownUseCase.getCurrentMonthBreakdown()
        val trendsResult = analyzeSpendingTrendsUseCase.getShortTermTrends()
        val anomaliesResult = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Then - All operations should complete successfully
        assertTrue(reportResult.isSuccess)
        assertTrue(breakdownResult.isSuccess)
        assertTrue(trendsResult.isSuccess)
        assertTrue(anomaliesResult.isSuccess)
        
        // Performance should be reasonable (less than 5 seconds for all operations)
        assertTrue("Analytics operations took too long: ${totalTime}ms", totalTime < 5000)
    }

    private fun setupMultiMonthTransactionData() {
        val currentMonth = YearMonth.now()
        
        // Current month transactions
        val currentMonthTransactions = createRealisticTransactions(currentMonth, 50)
        val currentStartDate = currentMonth.atDay(1).atStartOfDay()
        val currentEndDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(currentStartDate, currentEndDate) 
        } returns currentMonthTransactions

        // Previous months for trend analysis
        for (i in 1..6) {
            val month = currentMonth.minusMonths(i.toLong())
            val transactions = createRealisticTransactions(month, 40 + i * 2)
            val startDate = month.atDay(1).atStartOfDay()
            val endDate = month.atEndOfMonth().atTime(23, 59, 59)
            coEvery { 
                transactionRepository.getTransactionsByDateRange(startDate, endDate) 
            } returns transactions
        }
    }

    private fun setupSpendingIncreaseData() {
        val currentMonth = YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)
        
        // Current month: Higher spending
        val currentTransactions = createTransactionsWithTotalSpending(BigDecimal("15000"))
        val currentStartDate = currentMonth.atDay(1).atStartOfDay()
        val currentEndDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(currentStartDate, currentEndDate) 
        } returns currentTransactions

        // Previous month: Lower spending
        val previousTransactions = createTransactionsWithTotalSpending(BigDecimal("10000"))
        val previousStartDate = previousMonth.atDay(1).atStartOfDay()
        val previousEndDate = previousMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(previousStartDate, previousEndDate) 
        } returns previousTransactions
    }

    private fun setupCategoryTrendData() {
        val currentMonth = YearMonth.now()
        
        // Setup 6 months of data with increasing food spending
        for (i in 0..5) {
            val month = currentMonth.minusMonths(i.toLong())
            val baseAmount = BigDecimal("2000").add(BigDecimal(i * 200)) // Increasing trend
            val transactions = createCategorySpecificTransactions(createFoodCategory(), baseAmount)
            
            val startDate = month.atDay(1).atStartOfDay()
            val endDate = month.atEndOfMonth().atTime(23, 59, 59)
            coEvery { 
                transactionRepository.getTransactionsByDateRange(startDate, endDate) 
            } returns transactions
        }
    }

    private fun setupAnomalyData() {
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.atDay(1).atStartOfDay()
        val endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        
        val anomalousTransactions = listOf(
            // Large transaction anomaly
            Transaction(
                id = 1,
                amount = BigDecimal("25000"), // Very large amount
                type = TransactionType.EXPENSE,
                category = createShoppingCategory(),
                merchant = "Expensive Store",
                description = "Large purchase",
                date = LocalDateTime.now(),
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            // Late night transaction
            Transaction(
                id = 2,
                amount = BigDecimal("2000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Late Night Restaurant",
                description = "Late dinner",
                date = LocalDateTime.now().withHour(2), // 2 AM
                source = TransactionSource.SMS_AUTO,
                accountId = 1
            ),
            // Normal transactions
            *createRealisticTransactions(currentMonth, 20).toTypedArray()
        )
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns anomalousTransactions
    }

    private fun setupEdgeCaseData() {
        val currentMonth = YearMonth.now()
        val emptyMonth = YearMonth.of(2023, 1)
        val singleTransactionMonth = YearMonth.of(2024, 2)
        
        // Empty month
        val emptyStartDate = emptyMonth.atDay(1).atStartOfDay()
        val emptyEndDate = emptyMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(emptyStartDate, emptyEndDate) 
        } returns emptyList()

        // Single transaction month
        val singleStartDate = singleTransactionMonth.atDay(1).atStartOfDay()
        val singleEndDate = singleTransactionMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(singleStartDate, singleEndDate) 
        } returns listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000"),
                type = TransactionType.EXPENSE,
                category = createFoodCategory(),
                merchant = "Single Restaurant",
                description = "Only transaction",
                date = LocalDateTime.of(2024, 2, 15, 12, 0),
                source = TransactionSource.MANUAL,
                accountId = 1
            )
        )

        // Current month with normal data
        val currentStartDate = currentMonth.atDay(1).atStartOfDay()
        val currentEndDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(currentStartDate, currentEndDate) 
        } returns createRealisticTransactions(currentMonth, 30)
    }

    private fun setupLargeDataset() {
        val currentMonth = YearMonth.now()
        
        // Create large dataset (1000 transactions)
        val largeTransactionSet = createRealisticTransactions(currentMonth, 1000)
        val startDate = currentMonth.atDay(1).atStartOfDay()
        val endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns largeTransactionSet

        // Setup data for multiple months for trend analysis
        for (i in 1..12) {
            val month = currentMonth.minusMonths(i.toLong())
            val transactions = createRealisticTransactions(month, 800)
            val monthStartDate = month.atDay(1).atStartOfDay()
            val monthEndDate = month.atEndOfMonth().atTime(23, 59, 59)
            coEvery { 
                transactionRepository.getTransactionsByDateRange(monthStartDate, monthEndDate) 
            } returns transactions
        }
    }

    private fun createRealisticTransactions(month: YearMonth, count: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val categories = createSampleCategories()
        val merchants = listOf("Restaurant A", "Amazon", "Grocery Store", "Gas Station", "Coffee Shop")
        
        repeat(count) { i ->
            val category = categories.random()
            val merchant = merchants.random()
            val amount = BigDecimal((100..5000).random())
            val day = (1..month.lengthOfMonth()).random()
            val hour = (8..22).random()
            
            transactions.add(
                Transaction(
                    id = i.toLong(),
                    amount = amount,
                    type = if (i % 10 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = category,
                    merchant = merchant,
                    description = "Transaction $i",
                    date = LocalDateTime.of(month.year, month.month, day, hour, 0),
                    source = TransactionSource.SMS_AUTO,
                    accountId = 1
                )
            )
        }
        
        return transactions
    }

    private fun createTransactionsWithTotalSpending(totalAmount: BigDecimal): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val categories = createSampleCategories()
        val transactionCount = 10
        val amountPerTransaction = totalAmount.divide(BigDecimal(transactionCount))
        
        repeat(transactionCount) { i ->
            transactions.add(
                Transaction(
                    id = i.toLong(),
                    amount = amountPerTransaction,
                    type = TransactionType.EXPENSE,
                    category = categories[i % categories.size],
                    merchant = "Merchant $i",
                    description = "Transaction $i",
                    date = LocalDateTime.now().minusDays(i.toLong()),
                    source = TransactionSource.SMS_AUTO,
                    accountId = 1
                )
            )
        }
        
        return transactions
    }

    private fun createCategorySpecificTransactions(category: Category, totalAmount: BigDecimal): List<Transaction> {
        return listOf(
            Transaction(
                id = 1,
                amount = totalAmount,
                type = TransactionType.EXPENSE,
                category = category,
                merchant = "Category Merchant",
                description = "Category transaction",
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
            Category(3, "Transport", "directions_car", "#4CAF50", true),
            Category(4, "Bills", "receipt", "#FF9800", true),
            Category(5, "Entertainment", "movie", "#9C27B0", true)
        )
    }

    private fun createFoodCategory(): Category {
        return Category(1, "Food", "restaurant", "#FF5722", true)
    }

    private fun createShoppingCategory(): Category {
        return Category(2, "Shopping", "shopping_cart", "#2196F3", true)
    }
}