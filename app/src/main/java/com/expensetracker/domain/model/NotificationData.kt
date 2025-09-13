package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Data model for notifications to be sent to users
 */
data class NotificationData(
    val id: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val title: String,
    val message: String,
    val accountId: Long? = null,
    val transactionId: Long? = null,
    val amount: BigDecimal? = null,
    val category: Category? = null,
    val scheduledTime: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isDelivered: Boolean = false,
    val deliveredAt: LocalDateTime? = null,
    val actionData: Map<String, String> = emptyMap()
)

/**
 * Bill reminder notification data
 */
data class BillReminderData(
    val billName: String,
    val dueDate: LocalDateTime,
    val amount: BigDecimal?,
    val accountId: Long,
    val category: Category
)

/**
 * Spending limit alert data
 */
data class SpendingLimitAlertData(
    val accountId: Long,
    val accountName: String,
    val currentSpending: BigDecimal,
    val spendingLimit: BigDecimal,
    val period: String, // "monthly", "weekly", etc.
    val category: Category? = null
)

/**
 * Low balance warning data
 */
data class LowBalanceWarningData(
    val accountId: Long,
    val accountName: String,
    val currentBalance: BigDecimal,
    val threshold: BigDecimal
)

/**
 * Unusual spending alert data
 */
data class UnusualSpendingAlertData(
    val anomaly: SpendingAnomaly,
    val accountId: Long,
    val accountName: String,
    val description: String,
    val suggestedAction: String?
)