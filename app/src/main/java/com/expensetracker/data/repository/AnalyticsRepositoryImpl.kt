package com.expensetracker.data.repository

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AnalyticsRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsRepository
 * Provides analytics and insights functionality using transaction data
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : AnalyticsRepository {

    override suspend fun generateMonthlyReport(month: YearMonth): MonthlyReport {
        val startDate = month.atDay(1).atStartOfDay()
        val endDate = month.atEndOfMonth().atTime(23, 59, 59)
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
        
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val netAmount = totalIncome - totalExpenses
        
        // Category breakdown
        val categoryBreakdown = expenseTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
        
        // Top merchants
        val topMerchants = calculateTopMerchants(expenseTransactions, 10)
        
        // Previous month comparison
        val previousMonth = month.minusMonths(1)
        val comparisonToPreviousMonth = calculateMonthComparison(month, previousMonth)
        
        // Account breakdown
        val accountBreakdown = calculateAccountBreakdown(transactions)
        
        return MonthlyReport(
            month = month,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netAmount = netAmount,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            comparisonToPreviousMonth = comparisonToPreviousMonth,
            accountBreakdown = accountBreakdown
        )
    }

    override suspend fun getCategoryBreakdown(dateRange: DateRange): CategoryBreakdown {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
        
        val totalAmount = transactions.sumOf { it.amount }
        
        val categorySpending = transactions
            .groupBy { it.category }
            .map { (category, categoryTransactions) ->
                val amount = categoryTransactions.sumOf { it.amount }
                val percentage = if (totalAmount > BigDecimal.ZERO) {
                    (amount.divide(totalAmount, 4, RoundingMode.HALF_UP).toFloat() * 100)
                } else 0f
                
                val transactionCount = categoryTransactions.size
                val averageTransactionAmount = if (transactionCount > 0) {
                    amount.divide(BigDecimal(transactionCount), 2, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO
                
                val trend = calculateCategoryTrend(category, dateRange)
                
                CategorySpending(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    transactionCount = transactionCount,
                    averageTransactionAmount = averageTransactionAmount,
                    trend = trend
                )
            }
            .sortedByDescending { it.amount }
        
        val topCategories = categorySpending.take(5)
        
        // Get all categories and find unused ones
        val allCategories = categoryRepository.getAllCategories()
        val usedCategories = categorySpending.map { it.category }.toSet()
        val unusedCategories = allCategories.filter { it !in usedCategories }
        
        return CategoryBreakdown(
            period = dateRange,
            totalAmount = totalAmount,
            categorySpending = categorySpending,
            topCategories = topCategories,
            unusedCategories = unusedCategories
        )
    }

    override suspend fun calculateSpendingTrends(months: Int): SpendingTrends {
        val monthlyTrends = getMonthlySpendingTrends(months)
        val categoryTrends = calculateCategoryTrends(months)
        val overallTrend = calculateOverallTrend(monthlyTrends)
        val seasonalPatterns = calculateSeasonalPatterns(monthlyTrends)
        val predictions = calculateSpendingPredictions(monthlyTrends)
        
        return SpendingTrends(
            monthlyTrends = monthlyTrends,
            categoryTrends = categoryTrends,
            overallTrend = overallTrend,
            seasonalPatterns = seasonalPatterns,
            predictions = predictions
        )
    }

    override suspend fun detectSpendingAnomalies(
        dateRange: DateRange,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly> {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
        val anomalies = mutableListOf<SpendingAnomaly>()
        
        // Detect large transactions
        anomalies.addAll(detectLargeTransactions(transactions, config))
        
        // Detect category spending spikes
        anomalies.addAll(detectCategorySpikes(transactions, config, dateRange))
        
        // Detect frequent small transactions
        anomalies.addAll(detectFrequentTransactions(transactions, config))
        
        // Detect unusual merchants
        anomalies.addAll(detectUnusualMerchants(transactions))
        
        // Detect late night transactions
        anomalies.addAll(detectLateNightTransactions(transactions, config))
        
        // Detect duplicate transactions
        anomalies.addAll(detectDuplicateTransactions(transactions, config))
        
        return anomalies.sortedByDescending { it.severity }
    }

    override suspend fun getTotalSpending(dateRange: DateRange): BigDecimal {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    override suspend fun getTotalIncome(dateRange: DateRange): BigDecimal {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    override suspend fun getSpendingByCategory(dateRange: DateRange): Map<Category, BigDecimal> {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    override suspend fun getSpendingByAccount(dateRange: DateRange): Map<Account, BigDecimal> {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
        
        // Group by account ID and create Account objects
        return transactions
            .groupBy { it.accountId }
            .mapNotNull { (accountId, accountTransactions) ->
                // Get account details - this would need to be implemented in AccountRepository
                // For now, create a placeholder account
                val account = Account(
                    id = accountId,
                    bankName = "Unknown",
                    accountType = AccountType.CHECKING,
                    accountNumber = "****",
                    nickname = "Account $accountId",
                    currentBalance = BigDecimal.ZERO,
                    createdAt = LocalDateTime.now()
                )
                account to accountTransactions.sumOf { it.amount }
            }
            .toMap()
    }

    override suspend fun getTopMerchants(dateRange: DateRange, limit: Int): List<MerchantSpending> {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
        
        return calculateTopMerchants(transactions, limit)
    }

    override suspend fun getMonthlySpendingTotals(months: Int): List<MonthlyTrend> {
        return getMonthlySpendingTrends(months)
    }

    override suspend fun getCategorySpendingTrends(
        category: Category,
        months: Int
    ): List<MonthlySpending> {
        val trends = mutableListOf<MonthlySpending>()
        val currentMonth = YearMonth.now()
        
        for (i in 0 until months) {
            val month = currentMonth.minusMonths(i.toLong())
            val startDate = month.atDay(1).atStartOfDay()
            val endDate = month.atEndOfMonth().atTime(23, 59, 59)
            
            val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .filter { it.type == TransactionType.EXPENSE && it.category == category }
            
            val amount = transactions.sumOf { it.amount }
            val transactionCount = transactions.size
            
            trends.add(
                MonthlySpending(
                    month = month,
                    amount = amount,
                    transactionCount = transactionCount
                )
            )
        }
        
        return trends.reversed() // Return in chronological order
    }

    override suspend fun getAverageDailySpending(dateRange: DateRange): BigDecimal {
        val totalSpending = getTotalSpending(dateRange)
        val days = ChronoUnit.DAYS.between(dateRange.startDate, dateRange.endDate) + 1
        
        return if (days > 0) {
            totalSpending.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    override suspend fun getTransactionCount(dateRange: DateRange): Int {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate).size
    }

    override suspend fun getLargestTransactions(
        dateRange: DateRange,
        limit: Int
    ): List<Transaction> {
        val startDate = dateRange.startDate.atStartOfDay()
        val endDate = dateRange.endDate.atTime(23, 59, 59)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    override suspend fun calculateSavingsRate(dateRange: DateRange): Float {
        val totalIncome = getTotalIncome(dateRange)
        val totalExpenses = getTotalSpending(dateRange)
        
        return if (totalIncome > BigDecimal.ZERO) {
            val savings = totalIncome - totalExpenses
            savings.divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
        } else {
            0f
        }
    }

    override fun observeAnalyticsUpdates(): Flow<Unit> {
        return transactionRepository.observeTransactions().map { }
    }

    // Private helper methods

    private suspend fun calculateTopMerchants(
        transactions: List<Transaction>,
        limit: Int
    ): List<MerchantSpending> {
        return transactions
            .groupBy { it.merchant }
            .map { (merchant, merchantTransactions) ->
                val totalAmount = merchantTransactions.sumOf { it.amount }
                val transactionCount = merchantTransactions.size
                val averageAmount = totalAmount.divide(
                    BigDecimal(transactionCount), 
                    2, 
                    RoundingMode.HALF_UP
                )
                val category = merchantTransactions.first().category
                
                MerchantSpending(
                    merchant = merchant,
                    totalAmount = totalAmount,
                    transactionCount = transactionCount,
                    category = category,
                    averageAmount = averageAmount
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(limit)
    }

    private suspend fun calculateMonthComparison(
        currentMonth: YearMonth,
        previousMonth: YearMonth
    ): MonthComparison {
        val currentReport = generateMonthlyReport(currentMonth)
        val previousReport = generateMonthlyReport(previousMonth)
        
        val incomeChange = currentReport.totalIncome - previousReport.totalIncome
        val expenseChange = currentReport.totalExpenses - previousReport.totalExpenses
        
        val incomeChangePercentage = if (previousReport.totalIncome > BigDecimal.ZERO) {
            incomeChange.divide(previousReport.totalIncome, 4, RoundingMode.HALF_UP).toFloat() * 100
        } else 0f
        
        val expenseChangePercentage = if (previousReport.totalExpenses > BigDecimal.ZERO) {
            expenseChange.divide(previousReport.totalExpenses, 4, RoundingMode.HALF_UP).toFloat() * 100
        } else 0f
        
        val significantChanges = calculateCategoryChanges(
            currentReport.categoryBreakdown,
            previousReport.categoryBreakdown
        )
        
        return MonthComparison(
            incomeChange = incomeChange,
            expenseChange = expenseChange,
            incomeChangePercentage = incomeChangePercentage,
            expenseChangePercentage = expenseChangePercentage,
            significantChanges = significantChanges
        )
    }

    private fun calculateAccountBreakdown(transactions: List<Transaction>): Map<Account, AccountSummary> {
        return transactions
            .groupBy { it.accountId }
            .mapNotNull { (accountId, accountTransactions) ->
                // Create placeholder account - this would need AccountRepository integration
                val account = Account(
                    id = accountId,
                    bankName = "Unknown",
                    accountType = AccountType.CHECKING,
                    accountNumber = "****",
                    nickname = "Account $accountId",
                    currentBalance = BigDecimal.ZERO,
                    createdAt = LocalDateTime.now()
                )
                
                val income = accountTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                
                val expenses = accountTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                
                val balanceChange = income - expenses
                
                val summary = AccountSummary(
                    account = account,
                    totalIncome = income,
                    totalExpenses = expenses,
                    transactionCount = accountTransactions.size,
                    balanceChange = balanceChange
                )
                
                account to summary
            }
            .toMap()
    }

    private suspend fun calculateCategoryTrend(category: Category, dateRange: DateRange): SpendingTrend {
        // Compare with previous period of same length
        val periodLength = ChronoUnit.DAYS.between(dateRange.startDate, dateRange.endDate)
        val previousPeriodStart = dateRange.startDate.minusDays(periodLength + 1)
        val previousPeriodEnd = dateRange.startDate.minusDays(1)
        val previousPeriod = DateRange(previousPeriodStart, previousPeriodEnd)
        
        val currentSpending = getSpendingByCategory(dateRange)[category] ?: BigDecimal.ZERO
        val previousSpending = getSpendingByCategory(previousPeriod)[category] ?: BigDecimal.ZERO
        
        val changePercentage = if (previousSpending > BigDecimal.ZERO) {
            (currentSpending - previousSpending)
                .divide(previousSpending, 4, RoundingMode.HALF_UP)
                .toFloat() * 100
        } else if (currentSpending > BigDecimal.ZERO) {
            100f
        } else {
            0f
        }
        
        val direction = when {
            changePercentage > 10f -> TrendDirection.INCREASING
            changePercentage < -10f -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        val isSignificant = kotlin.math.abs(changePercentage) > 20f
        
        return SpendingTrend(
            direction = direction,
            changePercentage = changePercentage,
            isSignificant = isSignificant
        )
    }

    private suspend fun getMonthlySpendingTrends(months: Int): List<MonthlyTrend> {
        val trends = mutableListOf<MonthlyTrend>()
        val currentMonth = YearMonth.now()
        
        for (i in 0 until months) {
            val month = currentMonth.minusMonths(i.toLong())
            val startDate = month.atDay(1).atStartOfDay()
            val endDate = month.atEndOfMonth().atTime(23, 59, 59)
            
            val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
            
            val totalSpending = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            val totalIncome = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            
            val savingsRate = if (totalIncome > BigDecimal.ZERO) {
                (totalIncome - totalSpending).divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
            } else 0f
            
            val changeFromPreviousMonth = if (i < months - 1) {
                val previousMonth = currentMonth.minusMonths((i + 1).toLong())
                val previousSpending = getTotalSpending(
                    DateRange(
                        previousMonth.atDay(1),
                        previousMonth.atEndOfMonth()
                    )
                )
                
                if (previousSpending > BigDecimal.ZERO) {
                    (totalSpending - previousSpending)
                        .divide(previousSpending, 4, RoundingMode.HALF_UP)
                        .toFloat() * 100
                } else 0f
            } else 0f
            
            val topCategories = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .map { (category, categoryTransactions) ->
                    val amount = categoryTransactions.sumOf { it.amount }
                    val percentage = if (totalSpending > BigDecimal.ZERO) {
                        amount.divide(totalSpending, 4, RoundingMode.HALF_UP).toFloat() * 100
                    } else 0f
                    
                    CategorySpending(
                        category = category,
                        amount = amount,
                        percentage = percentage,
                        transactionCount = categoryTransactions.size,
                        averageTransactionAmount = amount.divide(
                            BigDecimal(categoryTransactions.size),
                            2,
                            RoundingMode.HALF_UP
                        ),
                        trend = SpendingTrend(TrendDirection.STABLE, 0f, false)
                    )
                }
                .sortedByDescending { it.amount }
                .take(5)
            
            trends.add(
                MonthlyTrend(
                    month = month,
                    totalSpending = totalSpending,
                    totalIncome = totalIncome,
                    savingsRate = savingsRate,
                    changeFromPreviousMonth = changeFromPreviousMonth,
                    topCategories = topCategories
                )
            )
        }
        
        return trends.reversed() // Return in chronological order
    }

    private suspend fun calculateCategoryTrends(months: Int): Map<Category, CategoryTrend> {
        val allCategories = categoryRepository.getAllCategories()
        val trends = mutableMapOf<Category, CategoryTrend>()
        
        for (category in allCategories) {
            val monthlyData = getCategorySpendingTrends(category, months)
            
            val averageMonthlySpending = if (monthlyData.isNotEmpty()) {
                monthlyData.map { it.amount }.reduce { acc, amount -> acc + amount }
                    .divide(BigDecimal(monthlyData.size), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            
            val trendDirection = calculateTrendDirection(monthlyData)
            val volatility = calculateVolatility(monthlyData)
            val seasonality = calculateSeasonality(monthlyData)
            
            trends[category] = CategoryTrend(
                category = category,
                monthlyData = monthlyData,
                averageMonthlySpending = averageMonthlySpending,
                trendDirection = trendDirection,
                volatility = volatility,
                seasonality = seasonality
            )
        }
        
        return trends
    }

    private fun calculateTrendDirection(monthlyData: List<MonthlySpending>): TrendDirection {
        if (monthlyData.size < 3) return TrendDirection.STABLE
        
        val recentMonths = monthlyData.takeLast(3)
        val earlierMonths = monthlyData.dropLast(3).takeLast(3)
        
        if (recentMonths.isEmpty() || earlierMonths.isEmpty()) return TrendDirection.STABLE
        
        val recentAverage = recentMonths.map { it.amount }.reduce { acc, amount -> acc + amount }
            .divide(BigDecimal(recentMonths.size), 2, RoundingMode.HALF_UP)
        
        val earlierAverage = earlierMonths.map { it.amount }.reduce { acc, amount -> acc + amount }
            .divide(BigDecimal(earlierMonths.size), 2, RoundingMode.HALF_UP)
        
        val changePercentage = if (earlierAverage > BigDecimal.ZERO) {
            (recentAverage - earlierAverage).divide(earlierAverage, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f
        
        return when {
            changePercentage > 0.1f -> TrendDirection.INCREASING
            changePercentage < -0.1f -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun calculateVolatility(monthlyData: List<MonthlySpending>): Float {
        if (monthlyData.size < 2) return 0f
        
        val amounts = monthlyData.map { it.amount.toFloat() }
        val mean = amounts.average().toFloat()
        val variance = amounts.map { (it - mean) * (it - mean) }.average()
        
        return kotlin.math.sqrt(variance).toFloat()
    }

    private fun calculateSeasonality(monthlyData: List<MonthlySpending>): Float {
        if (monthlyData.size < 12) return 0f
        
        val monthlyAverages = monthlyData.groupBy { it.month.monthValue }
            .mapValues { (_, values) ->
                values.map { it.amount.toFloat() }.average().toFloat()
            }
        
        if (monthlyAverages.size < 4) return 0f
        
        val overallMean = monthlyAverages.values.average().toFloat()
        val seasonalVariance = monthlyAverages.values.map { (it - overallMean) * (it - overallMean) }.average()
        
        return kotlin.math.sqrt(seasonalVariance).toFloat()
    }

    private fun calculateOverallTrend(monthlyTrends: List<MonthlyTrend>): OverallTrend {
        if (monthlyTrends.size < 2) {
            return OverallTrend(
                direction = TrendDirection.STABLE,
                averageMonthlyChange = 0f,
                consistency = 0f,
                volatility = 0f,
                savingsRate = monthlyTrends.firstOrNull()?.savingsRate ?: 0f,
                savingsRateTrend = TrendDirection.STABLE
            )
        }
        
        val changes = monthlyTrends.zipWithNext { current, next ->
            next.changeFromPreviousMonth
        }
        
        val averageMonthlyChange = changes.average().toFloat()
        val consistency = calculateConsistency(changes)
        val volatility = calculateVolatility(monthlyTrends.map { 
            MonthlySpending(it.month, it.totalSpending, 0) 
        })
        
        val direction = when {
            averageMonthlyChange > 5f -> TrendDirection.INCREASING
            averageMonthlyChange < -5f -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        val currentSavingsRate = monthlyTrends.lastOrNull()?.savingsRate ?: 0f
        val previousSavingsRate = monthlyTrends.dropLast(1).lastOrNull()?.savingsRate ?: 0f
        
        val savingsRateTrend = when {
            currentSavingsRate > previousSavingsRate + 0.05f -> TrendDirection.INCREASING
            currentSavingsRate < previousSavingsRate - 0.05f -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return OverallTrend(
            direction = direction,
            averageMonthlyChange = averageMonthlyChange,
            consistency = consistency,
            volatility = volatility,
            savingsRate = currentSavingsRate,
            savingsRateTrend = savingsRateTrend
        )
    }

    private fun calculateConsistency(changes: List<Float>): Float {
        if (changes.isEmpty()) return 0f
        
        val mean = changes.average().toFloat()
        val variance = changes.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance).toFloat()
        
        // Consistency is inverse of coefficient of variation, normalized to 0-1
        return if (kotlin.math.abs(mean) > 0.01f) {
            kotlin.math.max(0f, 1f - (standardDeviation / kotlin.math.abs(mean)))
        } else {
            if (standardDeviation < 5f) 1f else 0f
        }
    }

    private fun calculateSeasonalPatterns(monthlyTrends: List<MonthlyTrend>): List<SeasonalPattern> {
        if (monthlyTrends.size < 12) return emptyList()
        
        val seasonalData = monthlyTrends.groupBy { getSeason(it.month.monthValue) }
        val overallAverage = monthlyTrends.map { it.totalSpending }.average()
        
        return seasonalData.map { (season, trends) ->
            val averageSpending = trends.map { it.totalSpending }.reduce { acc, amount -> acc + amount }
                .divide(BigDecimal(trends.size), 2, RoundingMode.HALF_UP)
            
            val topCategories = trends
                .flatMap { it.topCategories }
                .groupBy { it.category }
                .mapValues { (_, categorySpending) ->
                    categorySpending.map { it.amount }.reduce { acc, amount -> acc + amount }
                }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
            
            val spendingIncrease = ((averageSpending.toDouble() - overallAverage) / overallAverage * 100).toFloat()
            
            SeasonalPattern(
                season = season,
                averageSpending = averageSpending,
                topCategories = topCategories,
                spendingIncrease = spendingIncrease
            )
        }
    }

    private fun getSeason(month: Int): Season {
        return when (month) {
            3, 4, 5 -> Season.SPRING
            6, 7, 8 -> Season.SUMMER
            9, 10, 11 -> Season.FALL
            else -> Season.WINTER
        }
    }

    private fun calculateSpendingPredictions(monthlyTrends: List<MonthlyTrend>): SpendingPrediction? {
        if (monthlyTrends.size < 6) return null
        
        val recentTrends = monthlyTrends.takeLast(6)
        val averageSpending = recentTrends.map { it.totalSpending }.reduce { acc, amount -> acc + amount }
            .divide(BigDecimal(recentTrends.size), 2, RoundingMode.HALF_UP)
        
        val trendSlope = calculateTrendSlope(recentTrends)
        val nextMonthPrediction = averageSpending + (averageSpending * BigDecimal(trendSlope))
        
        val confidence = calculatePredictionConfidence(recentTrends)
        
        val categoryPredictions = recentTrends
            .flatMap { it.topCategories }
            .groupBy { it.category }
            .mapValues { (_, categorySpending) ->
                val avgCategorySpending = categorySpending.map { it.amount }.average()
                BigDecimal(avgCategorySpending).setScale(2, RoundingMode.HALF_UP)
            }
        
        val factors = listOf(
            "Based on last 6 months average",
            "Trend analysis included",
            if (trendSlope > 0.05) "Increasing spending trend detected" else "Stable spending pattern"
        )
        
        return SpendingPrediction(
            nextMonthPrediction = nextMonthPrediction,
            confidence = confidence,
            categoryPredictions = categoryPredictions,
            factors = factors
        )
    }

    private fun calculateTrendSlope(trends: List<MonthlyTrend>): Float {
        if (trends.size < 2) return 0f
        
        val changes = trends.zipWithNext { current, next ->
            next.changeFromPreviousMonth / 100f
        }
        
        return changes.average().toFloat()
    }

    private fun calculatePredictionConfidence(trends: List<MonthlyTrend>): Float {
        val volatility = calculateVolatility(trends.map { 
            MonthlySpending(it.month, it.totalSpending, 0) 
        })
        
        // Higher volatility = lower confidence
        val normalizedVolatility = kotlin.math.min(volatility / 10000f, 1f)
        return kotlin.math.max(0.3f, 1f - normalizedVolatility)
    }

    private fun calculateCategoryChanges(
        currentSpending: Map<Category, BigDecimal>,
        previousSpending: Map<Category, BigDecimal>
    ): List<CategoryChange> {
        val changes = mutableListOf<CategoryChange>()
        val allCategories = (currentSpending.keys + previousSpending.keys).distinct()
        
        for (category in allCategories) {
            val currentAmount = currentSpending[category] ?: BigDecimal.ZERO
            val previousAmount = previousSpending[category] ?: BigDecimal.ZERO
            val changeAmount = currentAmount - previousAmount
            
            val changePercentage = if (previousAmount > BigDecimal.ZERO) {
                changeAmount.divide(previousAmount, 4, RoundingMode.HALF_UP).toFloat() * 100
            } else if (currentAmount > BigDecimal.ZERO) {
                100f
            } else {
                0f
            }
            
            if (kotlin.math.abs(changePercentage) > 20f || 
                changeAmount.abs() > BigDecimal("1000")) {
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

    // Anomaly detection helper methods

    private fun detectLargeTransactions(
        transactions: List<Transaction>,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly> {
        return transactions
            .filter { it.type == TransactionType.EXPENSE && it.amount >= config.largeTransactionThreshold }
            .map { transaction ->
                SpendingAnomaly(
                    id = "large_transaction_${transaction.id}",
                    type = AnomalyType.UNUSUAL_LARGE_TRANSACTION,
                    severity = when {
                        transaction.amount >= config.largeTransactionThreshold * BigDecimal("5") -> AnomalySeverity.CRITICAL
                        transaction.amount >= config.largeTransactionThreshold * BigDecimal("2") -> AnomalySeverity.HIGH
                        else -> AnomalySeverity.MEDIUM
                    },
                    description = "Large transaction of ₹${transaction.amount} at ${transaction.merchant}",
                    detectedAt = LocalDateTime.now(),
                    relatedTransactions = listOf(transaction),
                    suggestedAction = "Verify this transaction is legitimate",
                    threshold = config.largeTransactionThreshold,
                    actualValue = transaction.amount,
                    category = transaction.category,
                    account = null
                )
            }
    }

    private suspend fun detectCategorySpikes(
        transactions: List<Transaction>,
        config: AnomalyDetectionConfig,
        dateRange: DateRange
    ): List<SpendingAnomaly> {
        val anomalies = mutableListOf<SpendingAnomaly>()
        
        // Get previous period for comparison
        val periodLength = ChronoUnit.DAYS.between(dateRange.startDate, dateRange.endDate)
        val previousPeriodStart = dateRange.startDate.minusDays(periodLength + 1)
        val previousPeriodEnd = dateRange.startDate.minusDays(1)
        val previousPeriod = DateRange(previousPeriodStart, previousPeriodEnd)
        
        val currentSpending = getSpendingByCategory(dateRange)
        val previousSpending = getSpendingByCategory(previousPeriod)
        
        for ((category, currentAmount) in currentSpending) {
            val previousAmount = previousSpending[category] ?: BigDecimal.ZERO
            
            if (previousAmount > BigDecimal.ZERO) {
                val spikeFactor = currentAmount.divide(previousAmount, 2, RoundingMode.HALF_UP).toFloat()
                
                if (spikeFactor >= config.categorySpikeFactor) {
                    val relatedTransactions = transactions.filter { 
                        it.category == category && it.type == TransactionType.EXPENSE 
                    }
                    
                    anomalies.add(
                        SpendingAnomaly(
                            id = "category_spike_${category.id}_${System.currentTimeMillis()}",
                            type = AnomalyType.CATEGORY_SPENDING_SPIKE,
                            severity = when {
                                spikeFactor >= 5f -> AnomalySeverity.HIGH
                                spikeFactor >= 3f -> AnomalySeverity.MEDIUM
                                else -> AnomalySeverity.LOW
                            },
                            description = "Spending in ${category.name} increased by ${((spikeFactor - 1) * 100).toInt()}%",
                            detectedAt = LocalDateTime.now(),
                            relatedTransactions = relatedTransactions,
                            suggestedAction = "Review ${category.name} transactions for unusual activity",
                            threshold = previousAmount * BigDecimal(config.categorySpikeFactor),
                            actualValue = currentAmount,
                            category = category,
                            account = null
                        )
                    )
                }
            }
        }
        
        return anomalies
    }

    private fun detectFrequentTransactions(
        transactions: List<Transaction>,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly> {
        val anomalies = mutableListOf<SpendingAnomaly>()
        
        // Group transactions by time windows
        val timeWindows = transactions
            .sortedBy { it.date }
            .windowed(config.frequentTransactionCount, 1, true)
            .filter { window ->
                if (window.size < config.frequentTransactionCount) return@filter false
                
                val timeSpan = ChronoUnit.MINUTES.between(
                    window.first().date,
                    window.last().date
                )
                
                timeSpan <= config.frequentTransactionTimeWindow
            }
        
        for (window in timeWindows) {
            anomalies.add(
                SpendingAnomaly(
                    id = "frequent_transactions_${System.currentTimeMillis()}",
                    type = AnomalyType.FREQUENT_SMALL_TRANSACTIONS,
                    severity = AnomalySeverity.MEDIUM,
                    description = "${window.size} transactions within ${config.frequentTransactionTimeWindow} minutes",
                    detectedAt = LocalDateTime.now(),
                    relatedTransactions = window,
                    suggestedAction = "Check for potential fraudulent activity",
                    threshold = BigDecimal(config.frequentTransactionCount),
                    actualValue = BigDecimal(window.size),
                    category = null,
                    account = null
                )
            )
        }
        
        return anomalies
    }

    private fun detectUnusualMerchants(transactions: List<Transaction>): List<SpendingAnomaly> {
        // This is a simplified implementation
        // In a real app, you'd maintain a list of known merchants
        val merchantFrequency = transactions
            .groupBy { it.merchant }
            .mapValues { it.value.size }
        
        val unusualMerchants = merchantFrequency
            .filter { it.value == 1 } // First time merchants
            .keys
        
        return transactions
            .filter { it.merchant in unusualMerchants && it.amount >= BigDecimal("5000") }
            .map { transaction ->
                SpendingAnomaly(
                    id = "unusual_merchant_${transaction.id}",
                    type = AnomalyType.UNUSUAL_MERCHANT,
                    severity = AnomalySeverity.LOW,
                    description = "First transaction with ${transaction.merchant}",
                    detectedAt = LocalDateTime.now(),
                    relatedTransactions = listOf(transaction),
                    suggestedAction = "Verify merchant legitimacy",
                    threshold = null,
                    actualValue = transaction.amount,
                    category = transaction.category,
                    account = null
                )
            }
    }

    private fun detectLateNightTransactions(
        transactions: List<Transaction>,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly> {
        return transactions
            .filter { transaction ->
                val hour = transaction.date.hour
                hour >= config.lateNightStartHour || hour <= config.lateNightEndHour
            }
            .filter { it.amount >= BigDecimal("1000") } // Only flag significant amounts
            .map { transaction ->
                SpendingAnomaly(
                    id = "late_night_${transaction.id}",
                    type = AnomalyType.LATE_NIGHT_TRANSACTIONS,
                    severity = AnomalySeverity.LOW,
                    description = "Late night transaction at ${transaction.date.hour}:${transaction.date.minute}",
                    detectedAt = LocalDateTime.now(),
                    relatedTransactions = listOf(transaction),
                    suggestedAction = "Verify transaction timing",
                    threshold = null,
                    actualValue = transaction.amount,
                    category = transaction.category,
                    account = null
                )
            }
    }

    private fun detectDuplicateTransactions(
        transactions: List<Transaction>,
        config: AnomalyDetectionConfig
    ): List<SpendingAnomaly> {
        val anomalies = mutableListOf<SpendingAnomaly>()
        
        val sortedTransactions = transactions.sortedBy { it.date }
        
        for (i in 0 until sortedTransactions.size - 1) {
            val current = sortedTransactions[i]
            val next = sortedTransactions[i + 1]
            
            val timeDifference = ChronoUnit.MINUTES.between(current.date, next.date)
            
            if (timeDifference <= config.duplicateTransactionTimeWindow &&
                current.amount == next.amount &&
                current.merchant == next.merchant) {
                
                anomalies.add(
                    SpendingAnomaly(
                        id = "duplicate_${current.id}_${next.id}",
                        type = AnomalyType.DUPLICATE_TRANSACTIONS,
                        severity = AnomalySeverity.MEDIUM,
                        description = "Potential duplicate transactions of ₹${current.amount}",
                        detectedAt = LocalDateTime.now(),
                        relatedTransactions = listOf(current, next),
                        suggestedAction = "Check for duplicate charges",
                        threshold = null,
                        actualValue = current.amount,
                        category = current.category,
                        account = null
                    )
                )
            }
        }
        
        return anomalies
    }
}