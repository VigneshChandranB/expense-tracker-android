package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model for spending anomaly detection
 * Represents unusual spending patterns that require user attention
 */
data class SpendingAnomaly(
    val id: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val description: String,
    val detectedAt: LocalDateTime,
    val relatedTransactions: List<Transaction>,
    val suggestedAction: String?,
    val threshold: BigDecimal?,
    val actualValue: BigDecimal,
    val category: Category?,
    val account: Account?
)

/**
 * Types of spending anomalies that can be detected
 */
enum class AnomalyType {
    UNUSUAL_LARGE_TRANSACTION,
    CATEGORY_SPENDING_SPIKE,
    FREQUENT_SMALL_TRANSACTIONS,
    UNUSUAL_MERCHANT,
    WEEKEND_SPENDING_PATTERN,
    LATE_NIGHT_TRANSACTIONS,
    DUPLICATE_TRANSACTIONS,
    BUDGET_EXCEEDED,
    ACCOUNT_BALANCE_LOW
}

/**
 * Severity levels for anomalies
 */
enum class AnomalySeverity {
    LOW,      // Minor deviation, informational
    MEDIUM,   // Moderate deviation, worth attention
    HIGH,     // Significant deviation, requires action
    CRITICAL  // Severe deviation, immediate attention needed
}

/**
 * Anomaly detection configuration
 */
data class AnomalyDetectionConfig(
    val largeTransactionThreshold: BigDecimal,
    val categorySpikeFactor: Float = 2.0f,
    val frequentTransactionCount: Int = 10,
    val frequentTransactionTimeWindow: Int = 60, // minutes
    val duplicateTransactionTimeWindow: Int = 5, // minutes
    val lateNightStartHour: Int = 23,
    val lateNightEndHour: Int = 6
)