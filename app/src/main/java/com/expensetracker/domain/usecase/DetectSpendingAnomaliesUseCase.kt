package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AnalyticsRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case for detecting unusual spending patterns and anomalies
 * Implements requirement 5.4: Spending alerts when exceeding previous month by 20%
 * Implements requirement 7.4: Unusual spending pattern detection alerts
 */
class DetectSpendingAnomaliesUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    /**
     * Detect spending anomalies for a specific period
     * 
     * @param dateRange The period to analyze for anomalies
     * @param config Configuration for anomaly detection thresholds
     * @return List of detected spending anomalies
     */
    suspend operator fun invoke(
        dateRange: DateRange,
        config: AnomalyDetectionConfig
    ): Result<List<SpendingAnomaly>> {
        return try {
            val anomalies = analyticsRepository.detectSpendingAnomalies(dateRange, config)
            Result.success(anomalies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Detect anomalies for the current month with default configuration
     */
    suspend fun detectCurrentMonthAnomalies(): Result<List<SpendingAnomaly>> {
        val config = createDefaultConfig()
        return invoke(DateRange.currentMonth(), config)
    }
    
    /**
     * Detect anomalies for the last 7 days
     */
    suspend fun detectRecentAnomalies(): Result<List<SpendingAnomaly>> {
        val config = createDefaultConfig()
        return invoke(DateRange.lastNDays(7), config)
    }
    
    /**
     * Check if current month spending exceeds previous month by threshold
     * Implements requirement 5.4: Alert when spending exceeds previous month by 20%
     */
    suspend fun checkMonthlySpendingAlert(threshold: Float = 0.20f): Result<SpendingAnomaly?> {
        return try {
            val currentMonth = DateRange.currentMonth()
            val previousMonth = DateRange.previousMonth()
            
            val currentSpending = analyticsRepository.getTotalSpending(currentMonth)
            val previousSpending = analyticsRepository.getTotalSpending(previousMonth)
            
            if (previousSpending > BigDecimal.ZERO) {
                val changePercentage = (currentSpending - previousSpending)
                    .divide(previousSpending, 4, java.math.RoundingMode.HALF_UP)
                    .toFloat()
                
                if (changePercentage > threshold) {
                    val anomaly = SpendingAnomaly(
                        id = "monthly_spending_alert_${System.currentTimeMillis()}",
                        type = AnomalyType.CATEGORY_SPENDING_SPIKE,
                        severity = if (changePercentage > 0.5f) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                        description = "Monthly spending increased by ${(changePercentage * 100).toInt()}% compared to last month",
                        detectedAt = java.time.LocalDateTime.now(),
                        relatedTransactions = emptyList(),
                        suggestedAction = "Review recent transactions and consider adjusting spending habits",
                        threshold = previousSpending.multiply(BigDecimal(1 + threshold)),
                        actualValue = currentSpending,
                        category = null,
                        account = null
                    )
                    Result.success(anomaly)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Filter anomalies by severity level
     */
    fun filterBySeverity(
        anomalies: List<SpendingAnomaly>,
        minSeverity: AnomalySeverity
    ): List<SpendingAnomaly> {
        val severityOrder = listOf(
            AnomalySeverity.LOW,
            AnomalySeverity.MEDIUM,
            AnomalySeverity.HIGH,
            AnomalySeverity.CRITICAL
        )
        
        val minIndex = severityOrder.indexOf(minSeverity)
        return anomalies.filter { 
            severityOrder.indexOf(it.severity) >= minIndex 
        }
    }
    
    /**
     * Create default anomaly detection configuration
     */
    private fun createDefaultConfig(): AnomalyDetectionConfig {
        return AnomalyDetectionConfig(
            largeTransactionThreshold = BigDecimal("10000"), // ₹10,000
            categorySpikeFactor = 2.0f,
            frequentTransactionCount = 10,
            frequentTransactionTimeWindow = 60,
            duplicateTransactionTimeWindow = 5,
            lateNightStartHour = 23,
            lateNightEndHour = 6
        )
    }
}