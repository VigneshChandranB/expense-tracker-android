package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entity.AccountNotificationSettingsEntity
import com.expensetracker.data.local.entity.NotificationEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO for notification-related database operations
 */
@Dao
interface NotificationDao {
    
    // Notification CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: String)
    
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: String): NotificationEntity?
    
    @Query("SELECT * FROM notifications WHERE isDelivered = 0 ORDER BY scheduledTime ASC")
    suspend fun getPendingNotifications(): List<NotificationEntity>
    
    @Query("SELECT * FROM notifications WHERE scheduledTime <= :currentTime AND isDelivered = 0 ORDER BY scheduledTime ASC")
    suspend fun getNotificationsDueForDelivery(currentTime: LocalDateTime): List<NotificationEntity>
    
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getNotificationHistory(limit: Int): List<NotificationEntity>
    
    @Query("UPDATE notifications SET isDelivered = 1, deliveredAt = :deliveredAt WHERE id = :notificationId")
    suspend fun markNotificationAsDelivered(notificationId: String, deliveredAt: LocalDateTime)
    
    @Query("DELETE FROM notifications WHERE createdAt < :cutoffDate")
    suspend fun deleteOldNotifications(cutoffDate: LocalDateTime)
    
    @Query("SELECT * FROM notifications WHERE accountId = :accountId ORDER BY createdAt DESC")
    suspend fun getNotificationsForAccount(accountId: Long): List<NotificationEntity>
    
    // Notification Preferences operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationPreferences(preferences: NotificationPreferencesEntity)
    
    @Update
    suspend fun updateNotificationPreferences(preferences: NotificationPreferencesEntity)
    
    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    suspend fun getNotificationPreferences(): NotificationPreferencesEntity?
    
    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    fun observeNotificationPreferences(): Flow<NotificationPreferencesEntity?>
    
    // Account-specific notification settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountNotificationSettings(settings: AccountNotificationSettingsEntity)
    
    @Update
    suspend fun updateAccountNotificationSettings(settings: AccountNotificationSettingsEntity)
    
    @Delete
    suspend fun deleteAccountNotificationSettings(settings: AccountNotificationSettingsEntity)
    
    @Query("SELECT * FROM account_notification_settings WHERE accountId = :accountId")
    suspend fun getAccountNotificationSettings(accountId: Long): AccountNotificationSettingsEntity?
    
    @Query("SELECT * FROM account_notification_settings")
    suspend fun getAllAccountNotificationSettings(): List<AccountNotificationSettingsEntity>
    
    @Query("DELETE FROM account_notification_settings WHERE accountId = :accountId")
    suspend fun deleteAccountNotificationSettingsById(accountId: Long)
    
    // Utility queries
    @Query("SELECT COUNT(*) FROM notifications WHERE isDelivered = 0")
    suspend fun getPendingNotificationCount(): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE type = :type AND createdAt >= :since")
    suspend fun getNotificationCountByTypeSince(type: String, since: LocalDateTime): Int
    
    @Query("DELETE FROM notifications WHERE type = :type AND accountId = :accountId AND createdAt >= :since")
    suspend fun deleteDuplicateNotifications(type: String, accountId: Long, since: LocalDateTime)
    
    // Data management methods
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}