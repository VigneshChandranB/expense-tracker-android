package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * Use case for sending unusual spending pattern detection alerts across accounts
 */
class SendUnusualSpendingAlertUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService
) {
    
    suspend operator fun invoke(alertData: UnusualSpendingAlertData) {
        val preferences = notificationRepository.getNotificationPreferences()
        
        if (!preferences.unusualSpendingAlertsEnabled) {
            return
        }
        
        // Check account-specific settings
        val accountSettings = preferences.accountSpecificSettings[alertData.accountId]
        if (accountSettings?.unusualSpendingEnabled == false) {
            return
        }
        
        val priority = when (alertData.anomaly.severity) {
            AnomalySeverity.CRITICAL -> NotificationPriority.URGENT
            AnomalySeverity.HIGH -> NotificationPriority.HIGH
            AnomalySeverity.MEDIUM -> NotificationPriority.NORMAL
            AnomalySeverity.LOW -> NotificationPriority.LOW
        }
        
        val notification = NotificationData(
            id = "unusual_spending_${alertData.anomaly.id}",
            type = NotificationType.UNUSUAL_SPENDING_ALERT,
            priority = priority,
            title = "Unusual Spending Detected",
            message = buildUnusualSpendingMessage(alertData),
            accountId = alertData.accountId,
            amount = alertData.anomaly.actualValue,
            category = alertData.anomaly.category,
            scheduledTime = LocalDateTime.now(),
            actionData = mapOf(
                "anomalyId" to alertData.anomaly.id,
                "anomalyType" to alertData.anomaly.type.name,
                "severity" to alertData.anomaly.severity.name,
                "accountId" to alertData.accountId.toString(),
                "accountName" to alertData.accountName,
                "description" to alertData.description,
                "suggestedAction" to (alertData.suggestedAction ?: "")
            )
        )
        
        notificationService.sendNotification(notification)
        notificationRepository.markNotificationAsDelivered(notification.id)
    }
    
    private fun buildUnusualSpendingMessage(data: UnusualSpendingAlertData): String {
        val accountText = if (data.accountName.isNotBlank()) " on ${data.accountName}" else ""
        val actionText = data.suggestedAction?.let { ". $it" } ?: ""
        
        return "${data.description}${accountText}${actionText}"
    }
}