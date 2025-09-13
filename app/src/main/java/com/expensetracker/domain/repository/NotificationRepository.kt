package com.expensetracker.domain.repository

import com.expensetracker.domain.model.NotificationData
import com.expensetracker.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification management
 */
interface NotificationRepository {
    
    /**
     * Get user notification preferences
     */
    suspend fun getNotificationPreferences(): NotificationPreferences
    
    /**
     * Update user notification preferences
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Schedule a notification
     */
    suspend fun scheduleNotification(notification: NotificationData)
    
    /**
     * Cancel a scheduled notification
     */
    suspend fun cancelNotification(notificationId: String)
    
    /**
     * Get all pending notifications
     */
    suspend fun getPendingNotifications(): List<NotificationData>
    
    /**
     * Mark notification as delivered
     */
    suspend fun markNotificationAsDelivered(notificationId: String)
    
    /**
     * Get notification history
     */
    suspend fun getNotificationHistory(limit: Int = 50): List<NotificationData>
    
    /**
     * Clear old notifications
     */
    suspend fun clearOldNotifications(olderThanDays: Int = 30)
    
    /**
     * Observe notification preferences changes
     */
    fun observeNotificationPreferences(): Flow<NotificationPreferences>
}