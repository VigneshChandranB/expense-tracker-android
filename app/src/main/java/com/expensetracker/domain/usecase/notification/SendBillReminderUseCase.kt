package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * Use case for sending bill due date reminder notifications
 */
class SendBillReminderUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService
) {
    
    suspend operator fun invoke(billReminderData: BillReminderData) {
        val preferences = notificationRepository.getNotificationPreferences()
        
        if (!preferences.billRemindersEnabled) {
            return
        }
        
        val reminderTime = billReminderData.dueDate.minusDays(preferences.billReminderDaysBefore.toLong())
        
        // Don't schedule reminders for past dates
        if (reminderTime.isBefore(LocalDateTime.now())) {
            return
        }
        
        val notification = NotificationData(
            id = "bill_reminder_${UUID.randomUUID()}",
            type = NotificationType.BILL_DUE_REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Bill Due Reminder",
            message = buildBillReminderMessage(billReminderData, preferences.billReminderDaysBefore),
            accountId = billReminderData.accountId,
            amount = billReminderData.amount,
            category = billReminderData.category,
            scheduledTime = reminderTime,
            actionData = mapOf(
                "billName" to billReminderData.billName,
                "dueDate" to billReminderData.dueDate.toString(),
                "accountId" to billReminderData.accountId.toString()
            )
        )
        
        if (isWithinQuietHours(reminderTime, preferences)) {
            // Reschedule to after quiet hours
            val adjustedTime = adjustForQuietHours(reminderTime, preferences)
            notificationRepository.scheduleNotification(notification.copy(scheduledTime = adjustedTime))
        } else {
            notificationRepository.scheduleNotification(notification)
        }
    }
    
    private fun buildBillReminderMessage(data: BillReminderData, daysBefore: Int): String {
        val amountText = data.amount?.let { " of ₹${it}" } ?: ""
        return "Your ${data.billName} bill${amountText} is due in $daysBefore days"
    }
    
    private fun isWithinQuietHours(time: LocalDateTime, preferences: NotificationPreferences): Boolean {
        if (!preferences.quietHoursEnabled) return false
        
        val hour = time.hour
        return if (preferences.quietHoursStart < preferences.quietHoursEnd) {
            hour >= preferences.quietHoursStart && hour < preferences.quietHoursEnd
        } else {
            hour >= preferences.quietHoursStart || hour < preferences.quietHoursEnd
        }
    }
    
    private fun adjustForQuietHours(time: LocalDateTime, preferences: NotificationPreferences): LocalDateTime {
        return time.withHour(preferences.quietHoursEnd).withMinute(0).withSecond(0)
    }
}