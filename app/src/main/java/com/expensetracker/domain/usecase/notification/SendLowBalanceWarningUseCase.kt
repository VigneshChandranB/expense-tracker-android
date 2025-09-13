package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * Use case for sending low balance warning notifications for individual accounts
 */
class SendLowBalanceWarningUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService
) {
    
    suspend operator fun invoke(warningData: LowBalanceWarningData) {
        val preferences = notificationRepository.getNotificationPreferences()
        
        if (!preferences.lowBalanceWarningsEnabled) {
            return
        }
        
        // Check account-specific settings
        val accountSettings = preferences.accountSpecificSettings[warningData.accountId]
        if (accountSettings?.lowBalanceEnabled == false) {
            return
        }
        
        // Use account-specific threshold if available, otherwise use global threshold
        val effectiveThreshold = accountSettings?.lowBalanceThreshold ?: preferences.lowBalanceThreshold
        
        if (warningData.currentBalance > effectiveThreshold) {
            return // Balance is above threshold, no warning needed
        }
        
        val priority = when {
            warningData.currentBalance <= effectiveThreshold.multiply(0.5.toBigDecimal()) -> NotificationPriority.URGENT
            warningData.currentBalance <= effectiveThreshold.multiply(0.75.toBigDecimal()) -> NotificationPriority.HIGH
            else -> NotificationPriority.NORMAL
        }
        
        val notification = NotificationData(
            id = "low_balance_${warningData.accountId}_${UUID.randomUUID()}",
            type = NotificationType.LOW_BALANCE_WARNING,
            priority = priority,
            title = "Low Balance Warning",
            message = buildLowBalanceMessage(warningData),
            accountId = warningData.accountId,
            amount = warningData.currentBalance,
            scheduledTime = LocalDateTime.now(),
            actionData = mapOf(
                "accountId" to warningData.accountId.toString(),
                "accountName" to warningData.accountName,
                "currentBalance" to warningData.currentBalance.toString(),
                "threshold" to warningData.threshold.toString()
            )
        )
        
        notificationService.sendNotification(notification)
        notificationRepository.markNotificationAsDelivered(notification.id)
    }
    
    private fun buildLowBalanceMessage(data: LowBalanceWarningData): String {
        return "Low balance alert for ${data.accountName}: ₹${data.currentBalance}. Consider adding funds to avoid overdraft fees."
    }
}