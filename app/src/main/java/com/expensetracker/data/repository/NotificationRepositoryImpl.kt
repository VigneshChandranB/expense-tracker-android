package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.NotificationDao
import com.expensetracker.data.mapper.CategoryMapper
import com.expensetracker.data.mapper.NotificationMapper
import com.expensetracker.domain.model.NotificationData
import com.expensetracker.domain.model.NotificationPreferences
import com.expensetracker.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val categoryDao: CategoryDao
) : NotificationRepository {
    
    override suspend fun getNotificationPreferences(): NotificationPreferences {
        val preferencesEntity = notificationDao.getNotificationPreferences()
        val accountSettings = notificationDao.getAllAccountNotificationSettings()
        
        return if (preferencesEntity != null) {
            NotificationMapper.toDomain(preferencesEntity, accountSettings)
        } else {
            // Return default preferences if none exist
            val defaultPreferences = NotificationPreferences()
            updateNotificationPreferences(defaultPreferences)
            defaultPreferences
        }
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        val preferencesEntity = NotificationMapper.toEntity(preferences)
        notificationDao.insertNotificationPreferences(preferencesEntity)
        
        // Update account-specific settings
        preferences.accountSpecificSettings.forEach { (accountId, settings) ->
            val settingsEntity = NotificationMapper.toEntity(settings)
            notificationDao.insertAccountNotificationSettings(settingsEntity)
        }
    }
    
    override suspend fun scheduleNotification(notification: NotificationData) {
        val entity = NotificationMapper.toEntity(notification)
        notificationDao.insertNotification(entity)
    }
    
    override suspend fun cancelNotification(notificationId: String) {
        notificationDao.deleteNotificationById(notificationId)
    }
    
    override suspend fun getPendingNotifications(): List<NotificationData> {
        val entities = notificationDao.getPendingNotifications()
        return entities.map { entity ->
            val category = entity.categoryId?.let { categoryId ->
                categoryDao.getCategoryById(categoryId)?.let { categoryEntity ->
                    CategoryMapper.toDomain(categoryEntity)
                }
            }
            NotificationMapper.toDomain(entity, category)
        }
    }
    
    override suspend fun markNotificationAsDelivered(notificationId: String) {
        notificationDao.markNotificationAsDelivered(notificationId, LocalDateTime.now())
    }
    
    override suspend fun getNotificationHistory(limit: Int): List<NotificationData> {
        val entities = notificationDao.getNotificationHistory(limit)
        return entities.map { entity ->
            val category = entity.categoryId?.let { categoryId ->
                categoryDao.getCategoryById(categoryId)?.let { categoryEntity ->
                    CategoryMapper.toDomain(categoryEntity)
                }
            }
            NotificationMapper.toDomain(entity, category)
        }
    }
    
    override suspend fun clearOldNotifications(olderThanDays: Int) {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
        notificationDao.deleteOldNotifications(cutoffDate)
    }
    
    override fun observeNotificationPreferences(): Flow<NotificationPreferences> {
        return notificationDao.observeNotificationPreferences().map { preferencesEntity ->
            if (preferencesEntity != null) {
                val accountSettings = notificationDao.getAllAccountNotificationSettings()
                NotificationMapper.toDomain(preferencesEntity, accountSettings)
            } else {
                NotificationPreferences()
            }
        }
    }
    
    /**
     * Get notifications due for delivery at the current time
     */
    suspend fun getNotificationsDueForDelivery(): List<NotificationData> {
        val entities = notificationDao.getNotificationsDueForDelivery(LocalDateTime.now())
        return entities.map { entity ->
            val category = entity.categoryId?.let { categoryId ->
                categoryDao.getCategoryById(categoryId)?.let { categoryEntity ->
                    CategoryMapper.toDomain(categoryEntity)
                }
            }
            NotificationMapper.toDomain(entity, category)
        }
    }
    
    /**
     * Check if a similar notification was recently sent to avoid spam
     */
    suspend fun hasSimilarRecentNotification(
        type: String,
        accountId: Long,
        withinMinutes: Int = 60
    ): Boolean {
        val since = LocalDateTime.now().minusMinutes(withinMinutes.toLong())
        val count = notificationDao.getNotificationCountByTypeSince(type, since)
        return count > 0
    }
    
    /**
     * Delete duplicate notifications to prevent spam
     */
    suspend fun deleteDuplicateNotifications(
        type: String,
        accountId: Long,
        withinMinutes: Int = 5
    ) {
        val since = LocalDateTime.now().minusMinutes(withinMinutes.toLong())
        notificationDao.deleteDuplicateNotifications(type, accountId, since)
    }
}