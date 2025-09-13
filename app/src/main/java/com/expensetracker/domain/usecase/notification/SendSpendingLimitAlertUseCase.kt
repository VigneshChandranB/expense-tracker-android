package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * Use case for sending spending limit alert notifications per account
 */
class SendSpendingLimitAlertUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService
) {
    
    suspend operator fun invoke(alertData: SpendingLimitAlertData) {
        val preferences = notificationRepository.getNotificationPreferences()
        
        if (!preferences.spendingLimitAlertsEnabled) {
            return
        }
        
        // Check account-specific settings
        val accountSettings = preferences.accountSpecificSettings[alertData.accountId]
        if (accountSettings?.spendingLimitEnabled == false) {
            return
        }
        
        val percentageUsed = (alertData.currentSpending.divide(alertData.spendingLimit) * 100.toBigDecimal()).toInt()
        val priority = when {
            percentageUsed >= 100 -> NotificationPriority.URGENT
            percentageUsed >= 90 -> NotificationPriority.HIGH
            percentageUsed >= 80 -> NotificationPriority.NORMAL
            else -> NotificationPriority.LOW
        }
        
        val notification = NotificationData(
            id = "spending_limit_${alertData.accountId}_${UUID.randomUUID()}",
            type = NotificationType.SPENDING_LIMIT_ALERT,
            priority = priority,
            title = "Spending Limit Alert",
            message = buildSpendingLimitMessage(alertData, percentageUsed),
            accountId = alertData.accountId,
            amount = alertData.currentSpending,
            category = alertData.category,
            scheduledTime = LocalDateTime.now(),
            actionData = mapOf(
                "accountId" to alertData.accountId.toString(),
                "accountName" to alertData.accountName,
                "currentSpending" to alertData.currentSpending.toString(),
                "spendingLimit" to alertData.spendingLimit.toString(),
                "percentageUsed" to percentageUsed.toString(),
                "period" to alertData.period
            )
        )
        
        notificationService.sendNotification(notification)
        notificationRepository.markNotificationAsDelivered(notification.id)
    }
    
    private fun buildSpendingLimitMessage(data: SpendingLimitAlertData, percentageUsed: Int): String {
        val categoryText = data.category?.let { " in ${it.name}" } ?: ""
        
        return when {
            percentageUsed >= 100 -> "You've exceeded your ${data.period} spending limit${categoryText} for ${data.accountName}. Current: ₹${data.currentSpending}, Limit: ₹${data.spendingLimit}"
            percentageUsed >= 90 -> "You've used ${percentageUsed}% of your ${data.period} spending limit${categoryText} for ${data.accountName}"
            else -> "Spending alert: ${percentageUsed}% of your ${data.period} limit used${categoryText} for ${data.accountName}"
        }
    }
}