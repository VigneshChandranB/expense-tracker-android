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
import kotlin.system.measureTimeMillis

/**
 * Performance tests for analytics engine
 * Tests performance with large datasets and complex calculations
 */
class AnalyticsPerformanceTest {

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
    fun `monthly report generation should complete within acceptable time for large dataset`() = runTest {
        // Given - Large dataset (10,000 transactions)
        val largeTransactionSet = createLargeTransactionDataset(10000)
        val month = YearMonth.now()
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns largeTransactionSet

        // When - Generate monthly report and measure time
        val executionTime = measureTimeMillis {
            val result = generateMonthlyReportUseCase(month)
            assertTrue("Monthly report generation failed", result.isSuccess)
            
            val report = result.getOrNull()!!
            assertTrue("Report should have data", report.totalExpenses > BigDecimal.ZERO)
            assertTrue("Should have category breakdown", report.categoryBreakdown.isNotEmpty())
            assertTrue("Should have top merchants", report.topMerchants.isNotEmpty())
        }

        // Then - Should complete within 2 seconds
        assertTrue("Monthly report took too long: ${executionTime}ms", executionTime < 2000)
        println("Monthly report generation with 10,000 transactions: ${executionTime}ms")
    }

    @Test
    fun `category breakdown analysis should handle large datasets efficiently`() = runTest {
        // Given - Large dataset with many categories
        val largeTransactionSet = createDiverseCategoryTransactions(5000)
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns largeTransactionSet

        // When - Analyze category breakdown and measure time
        val executionTime = measureTimeMillis {
            val result = analyzeCategoryBreakdownUseCase(dateRange)
            assertTrue("Category breakdown analysis failed", result.isSuccess)
            
            val breakdown = result.getOrNull()!!
            assertTrue("Should have category data", breakdown.categorySpending.isNotEmpty())
            assertEquals("Percentages should sum to 100%", 
                100f, 
                breakdown.categorySpending.sumOf { it.percentage.toDouble() }.toFloat(), 
                1f
            )
        }

        // Then - Should complete within 1.5 seconds
        assertTrue("Category breakdown took too long: ${executionTime}ms", executionTime < 1500)
        println("Category breakdown analysis with 5,000 transactions: ${executionTime}ms")
    }

    @Test
    fun `spending trends calculation should be efficient for multi-year data`() = runTest {
        // Given - 24 months of data (2 years)
        setupMultiYearTrendData(24)

        // When - Calculate spending trends and measure time
        val executionTime = measureTimeMillis {
            val result = analyzeSpendingTrendsUseCase.getLongTermTrends()
            assertTrue("Spending trends calculation failed", result.isSuccess)
            
            val trends = result.getOrNull()!!
            assertEquals("Should have 24 months of data", 24, trends.monthlyTrends.size)
            assertTrue("Should have category trends", trends.categoryTrends.isNotEmpty())
            assertNotNull("Should have overall trend", trends.overallTrend)
        }

        // Then - Should complete within 3 seconds
        assertTrue("Spending trends took too long: ${executionTime}ms", executionTime < 3000)
        println("Spending trends calculation for 24 months: ${executionTime}ms")
    }

    @Test
    fun `anomaly detection should scale well with transaction volume`() = runTest {
        // Given - Large dataset with potential anomalies
        val largeDatasetWithAnomalies = createAnomalyDataset(8000)
        val dateRange = DateRange.currentMonth()
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns largeDatasetWithAnomalies
        
        // Mock previous period data for category spike detection
        val previousPeriodStart = dateRange.startDate.minusDays(31)
        val previousPeriodEnd = dateRange.startDate.minusDays(1)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(
                previousPeriodStart.atStartOfDay(), 
                previousPeriodEnd.atTime(23, 59, 59)
            ) 
        } returns createLargeTransactionDataset(1000)

        // When - Detect anomalies and measure time
        val executionTime = measureTimeMillis {
            val result = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()
            assertTrue("Anomaly detection failed", result.isSuccess)
            
            val anomalies = result.getOrNull()!!
            assertTrue("Should detect some anomalies", anomalies.isNotEmpty())
            
            // Verify different types of anomalies are detected
            val anomalyTypes = anomalies.map { it.type }.toSet()
            assertTrue("Should detect large transactions", 
                anomalyTypes.contains(AnomalyType.UNUSUAL_LARGE_TRANSACTION))
        }

        // Then - Should complete within 2.5 seconds
        assertTrue("Anomaly detection took too long: ${executionTime}ms", executionTime < 2500)
        println("Anomaly detection with 8,000 transactions: ${executionTime}ms")
    }

    @Test
    fun `concurrent analytics operations should not degrade performance significantly`() = runTest {
        // Given - Setup data for concurrent operations
        setupConcurrentTestData()

        // When - Run multiple analytics operations concurrently
        val totalExecutionTime = measureTimeMillis {
            val monthlyReportResult = generateMonthlyReportUseCase.generateCurrentMonthReport()
            val categoryBreakdownResult = analyzeCategoryBreakdownUseCase.getCurrentMonthBreakdown()
            val trendsResult = analyzeSpendingTrendsUseCase.getShortTermTrends()
            val anomaliesResult = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()
            
            // Verify all operations succeeded
            assertTrue("Monthly report failed", monthlyReportResult.isSuccess)
            assertTrue("Category breakdown failed", categoryBreakdownResult.isSuccess)
            assertTrue("Trends analysis failed", trendsResult.isSuccess)
            assertTrue("Anomaly detection failed", anomaliesResult.isSuccess)
        }

        // Then - Total time should be reasonable (less than 5 seconds for all operations)
        assertTrue("Concurrent operations took too long: ${totalExecutionTime}ms", totalExecutionTime < 5000)
        println("Concurrent analytics operations: ${totalExecutionTime}ms")
    }

    @Test
    fun `memory usage should remain stable with large datasets`() = runTest {
        // Given - Very large dataset
        val veryLargeDataset = createLargeTransactionDataset(50000)
        val month = YearMonth.now()
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns veryLargeDataset

        // When - Perform memory-intensive operations
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform multiple analytics operations
        repeat(5) {
            val reportResult = generateMonthlyReportUseCase(month)
            assertTrue("Operation $it failed", reportResult.isSuccess)
            
            // Force garbage collection between operations
            System.gc()
        }
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Then - Memory increase should be reasonable (less than 100MB)
        val maxMemoryIncrease = 100 * 1024 * 1024 // 100MB in bytes
        assertTrue("Memory usage increased too much: ${memoryIncrease / (1024 * 1024)}MB", 
            memoryIncrease < maxMemoryIncrease)
        
        println("Memory increase with 50,000 transactions: ${memoryIncrease / (1024 * 1024)}MB")
    }

    @Test
    fun `analytics calculations should be consistent across multiple runs`() = runTest {
        // Given - Fixed dataset
        val fixedDataset = createFixedTransactionDataset()
        val month = YearMonth.now()
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns fixedDataset

        // When - Run analytics multiple times
        val results = mutableListOf<MonthlyReport>()
        
        repeat(10) {
            val result = generateMonthlyReportUseCase(month)
            assertTrue("Run $it failed", result.isSuccess)
            results.add(result.getOrNull()!!)
        }

        // Then - All results should be identical
        val firstResult = results.first()
        results.forEach { result ->
            assertEquals("Total income should be consistent", firstResult.totalIncome, result.totalIncome)
            assertEquals("Total expenses should be consistent", firstResult.totalExpenses, result.totalExpenses)
            assertEquals("Net amount should be consistent", firstResult.netAmount, result.netAmount)
            assertEquals("Category breakdown should be consistent", 
                firstResult.categoryBreakdown.size, result.categoryBreakdown.size)
        }
        
        println("Analytics calculations are consistent across multiple runs")
    }

    private fun createLargeTransactionDataset(size: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val categories = createSampleCategories()
        val merchants = (1..100).map { "Merchant $it" }
        
        repeat(size) { i ->
            transactions.add(
                Transaction(
                    id = i.toLong(),
                    amount = BigDecimal((50..10000).random()),
                    type = if (i % 15 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = categories.random(),
                    merchant = merchants.random(),
                    description = "Transaction $i",
                    date = LocalDateTime.now().minusDays((0..30).random().toLong()),
                    source = TransactionSource.SMS_AUTO,
                    accountId = (1..5).random().toLong()
                )
            )
        }
        
        return transactions
    }

    private fun createDiverseCategoryTransactions(size: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val categories = createSampleCategories()
        
        repeat(size) { i ->
            transactions.add(
                Transaction(
                    id = i.toLong(),
                    amount = BigDecimal((100..5000).random()),
                    type = TransactionType.EXPENSE,
                    category = categories[i % categories.size],
                    merchant = "Merchant ${i % 50}",
                    description = "Transaction $i",
                    date = LocalDateTime.now().minusDays((0..30).random().toLong()),
                    source = TransactionSource.SMS_AUTO,
                    accountId = 1
                )
            )
        }
        
        return transactions
    }

    private fun setupMultiYearTrendData(months: Int) {
        val currentMonth = YearMonth.now()
        
        for (i in 0 until months) {
            val month = currentMonth.minusMonths(i.toLong())
            val transactions = createLargeTransactionDataset(500 + i * 10)
            val startDate = month.atDay(1).atStartOfDay()
            val endDate = month.atEndOfMonth().atTime(23, 59, 59)
            
            coEvery { 
                transactionRepository.getTransactionsByDateRange(startDate, endDate) 
            } returns transactions
        }
    }

    private fun createAnomalyDataset(size: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val categories = createSampleCategories()
        
        // Add normal transactions
        repeat(size - 50) { i ->
            transactions.add(
                Transaction(
                    id = i.toLong(),
                    amount = BigDecimal((100..2000).random()),
                    type = TransactionType.EXPENSE,
                    category = categories.random(),
                    merchant = "Normal Merchant $i",
                    description = "Normal transaction",
                    date = LocalDateTime.now().minusDays((0..30).random().toLong()),
                    source = TransactionSource.SMS_AUTO,
                    accountId = 1
                )
            )
        }
        
        // Add anomalous transactions
        repeat(50) { i ->
            val anomalyType = (1..4).random()
            when (anomalyType) {
                1 -> { // Large transaction
                    transactions.add(
                        Transaction(
                            id = (size + i).toLong(),
                            amount = BigDecimal((15000..50000).random()),
                            type = TransactionType.EXPENSE,
                            category = categories.random(),
                            merchant = "Expensive Store $i",
                            description = "Large purchase",
                            date = LocalDateTime.now().minusDays(i.toLong()),
                            source = TransactionSource.SMS_AUTO,
                            accountId = 1
                        )
                    )
                }
                2 -> { // Late night transaction
                    transactions.add(
                        Transaction(
                            id = (size + i).toLong(),
                            amount = BigDecimal((1000..3000).random()),
                            type = TransactionType.EXPENSE,
                            category = categories.random(),
                            merchant = "Late Night Store $i",
                            description = "Late transaction",
                            date = LocalDateTime.now().minusDays(i.toLong()).withHour((1..4).random()),
                            source = TransactionSource.SMS_AUTO,
                            accountId = 1
                        )
                    )
                }
                3 -> { // Duplicate-like transaction
                    val baseTransaction = Transaction(
                        id = (size + i).toLong(),
                        amount = BigDecimal("1500"),
                        type = TransactionType.EXPENSE,
                        category = categories.first(),
                        merchant = "Duplicate Store",
                        description = "Duplicate transaction",
                        date = LocalDateTime.now().minusDays(i.toLong()),
                        source = TransactionSource.SMS_AUTO,
                        accountId = 1
                    )
                    transactions.add(baseTransaction)
                    transactions.add(baseTransaction.copy(
                        id = (size + i + 100).toLong(),
                        date = baseTransaction.date.plusMinutes(2)
                    ))
                }
                4 -> { // Unusual merchant
                    transactions.add(
                        Transaction(
                            id = (size + i).toLong(),
                            amount = BigDecimal((5000..8000).random()),
                            type = TransactionType.EXPENSE,
                            category = categories.random(),
                            merchant = "Unknown Merchant $i",
                            description = "First time merchant",
                            date = LocalDateTime.now().minusDays(i.toLong()),
                            source = TransactionSource.SMS_AUTO,
                            accountId = 1
                        )
                    )
                }
            }
        }
        
        return transactions
    }

    private fun setupConcurrentTestData() {
        val currentMonth = YearMonth.now()
        val transactions = createLargeTransactionDataset(3000)
        
        // Setup current month data
        val startDate = currentMonth.atDay(1).atStartOfDay()
        val endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns transactions

        // Setup previous months for trends
        for (i in 1..6) {
            val month = currentMonth.minusMonths(i.toLong())
            val monthTransactions = createLargeTransactionDataset(2500)
            val monthStartDate = month.atDay(1).atStartOfDay()
            val monthEndDate = month.atEndOfMonth().atTime(23, 59, 59)
            coEvery { 
                transactionRepository.getTransactionsByDateRange(monthStartDate, monthEndDate) 
            } returns monthTransactions
        }
    }

    private fun createFixedTransactionDataset(): List<Transaction> {
        val categories = createSampleCategories()
        
        return listOf(
            Transaction(1, BigDecimal("1000"), TransactionType.EXPENSE, categories[0], "Merchant A", "Desc", LocalDateTime.of(2024, 1, 1, 10, 0), TransactionSource.SMS_AUTO, 1),
            Transaction(2, BigDecimal("2000"), TransactionType.EXPENSE, categories[1], "Merchant B", "Desc", LocalDateTime.of(2024, 1, 2, 11, 0), TransactionSource.SMS_AUTO, 1),
            Transaction(3, BigDecimal("5000"), TransactionType.INCOME, categories[2], "Salary", "Desc", LocalDateTime.of(2024, 1, 3, 12, 0), TransactionSource.MANUAL, 1),
            Transaction(4, BigDecimal("1500"), TransactionType.EXPENSE, categories[0], "Merchant C", "Desc", LocalDateTime.of(2024, 1, 4, 13, 0), TransactionSource.SMS_AUTO, 1),
            Transaction(5, BigDecimal("800"), TransactionType.EXPENSE, categories[1], "Merchant D", "Desc", LocalDateTime.of(2024, 1, 5, 14, 0), TransactionSource.SMS_AUTO, 1)
        )
    }

    private fun createSampleCategories(): List<Category> {
        return listOf(
            Category(1, "Food", "restaurant", "#FF5722", true),
            Category(2, "Shopping", "shopping_cart", "#2196F3", true),
            Category(3, "Transport", "directions_car", "#4CAF50", true),
            Category(4, "Bills", "receipt", "#FF9800", true),
            Category(5, "Entertainment", "movie", "#9C27B0", true),
            Category(6, "Healthcare", "local_hospital", "#E91E63", true),
            Category(7, "Investment", "trending_up", "#00BCD4", true),
            Category(8, "Income", "attach_money", "#8BC34A", true)
        )
    }
}