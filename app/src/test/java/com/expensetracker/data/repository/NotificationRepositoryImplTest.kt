package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.NotificationDao
import com.expensetracker.data.local.entity.NotificationEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class NotificationRepositoryImplTest {
    
    private val notificationDao = mockk<NotificationDao>()
    private val categoryDao = mockk<CategoryDao>()
    
    private lateinit var repository: NotificationRepositoryImpl
    
    @Before
    fun setup() {
        repository = NotificationRepositoryImpl(notificationDao, categoryDao)
    }
    
    @Test
    fun `should return default preferences when none exist`() = runTest {
        // Given
        coEvery { notificationDao.getNotificationPreferences() } returns null
        coEvery { notificationDao.getAllAccountNotificationSettings() } returns emptyList()
        coEvery { notificationDao.insertNotificationPreferences(any()) } just Runs
        
        // When
        val preferences = repository.getNotificationPreferences()
        
        // Then
        assertEquals(NotificationPreferences(), preferences)
        coVerify { notificationDao.insertNotificationPreferences(any()) }
    }
    
    @Test
    fun `should return existing preferences when available`() = runTest {
        // Given
        val preferencesEntity = NotificationPreferencesEntity(
            id = 1L,
            billRemindersEnabled = false,
            billReminderDaysBefore = 5
        )
        
        coEvery { notificationDao.getNotificationPreferences() } returns preferencesEntity
        coEvery { notificationDao.getAllAccountNotificationSettings() } returns emptyList()
        
        // When
        val preferences = repository.getNotificationPreferences()
        
        // Then
        assertEquals(false, preferences.billRemindersEnabled)
        assertEquals(5, preferences.billReminderDaysBefore)
    }
    
    @Test
    fun `should schedule notification successfully`() = runTest {
        // Given
        val notification = NotificationData(
            id = "test-notification",
            type = NotificationType.BILL_DUE_REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now()
        )
        
        coEvery { notificationDao.insertNotification(any()) } just Runs
        
        // When
        repository.scheduleNotification(notification)
        
        // Then
        coVerify { notificationDao.insertNotification(any()) }
    }
    
    @Test
    fun `should cancel notification successfully`() = runTest {
        // Given
        val notificationId = "test-notification"
        
        coEvery { notificationDao.deleteNotificationById(notificationId) } just Runs
        
        // When
        repository.cancelNotification(notificationId)
        
        // Then
        coVerify { notificationDao.deleteNotificationById(notificationId) }
    }
    
    @Test
    fun `should get pending notifications with categories`() = runTest {
        // Given
        val notificationEntity = NotificationEntity(
            id = "test-notification",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Test Notification",
            message = "Test message",
            categoryId = 1L,
            scheduledTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now()
        )
        
        coEvery { notificationDao.getPendingNotifications() } returns listOf(notificationEntity)
        coEvery { categoryDao.getCategoryById(1L) } returns null
        
        // When
        val notifications = repository.getPendingNotifications()
        
        // Then
        assertEquals(1, notifications.size)
        assertEquals("test-notification", notifications[0].id)
        assertEquals(NotificationType.BILL_DUE_REMINDER, notifications[0].type)
    }
    
    @Test
    fun `should mark notification as delivered`() = runTest {
        // Given
        val notificationId = "test-notification"
        
        coEvery { notificationDao.markNotificationAsDelivered(any(), any()) } just Runs
        
        // When
        repository.markNotificationAsDelivered(notificationId)
        
        // Then
        coVerify { notificationDao.markNotificationAsDelivered(eq(notificationId), any()) }
    }
    
    @Test
    fun `should clear old notifications`() = runTest {
        // Given
        val olderThanDays = 30
        
        coEvery { notificationDao.deleteOldNotifications(any()) } just Runs
        
        // When
        repository.clearOldNotifications(olderThanDays)
        
        // Then
        coVerify { notificationDao.deleteOldNotifications(any()) }
    }
    
    @Test
    fun `should observe notification preferences changes`() = runTest {
        // Given
        val preferencesEntity = NotificationPreferencesEntity()
        
        coEvery { notificationDao.observeNotificationPreferences() } returns flowOf(preferencesEntity)
        coEvery { notificationDao.getAllAccountNotificationSettings() } returns emptyList()
        
        // When
        val flow = repository.observeNotificationPreferences()
        
        // Then
        flow.collect { preferences ->
            assertNotNull(preferences)
        }
    }
}