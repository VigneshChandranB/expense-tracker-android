package com.expensetracker.domain.service

import com.expensetracker.domain.model.NotificationData

/**
 * Service interface for sending notifications to users
 */
interface NotificationService {
    
    /**
     * Send an immediate notification
     */
    suspend fun sendNotification(notification: NotificationData)
    
    /**
     * Schedule a notification for future delivery
     */
    suspend fun scheduleNotification(notification: NotificationData)
    
    /**
     * Cancel a scheduled notification
     */
    suspend fun cancelNotification(notificationId: String)
    
    /**
     * Check if notifications are enabled for the app
     */
    suspend fun areNotificationsEnabled(): Boolean
    
    /**
     * Request notification permission from user
     */
    suspend fun requestNotificationPermission(): Boolean
    
    /**
     * Create notification channels (required for Android 8.0+)
     */
    suspend fun createNotificationChannels()
}