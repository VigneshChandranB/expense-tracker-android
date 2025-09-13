package com.expensetracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entity.NotificationEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.data.local.entity.AccountNotificationSettingsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var notificationDao: NotificationDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        notificationDao = database.notificationDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveNotification() = runTest {
        // Given
        val notification = NotificationEntity(
            id = "test-notification",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now()
        )
        
        // When
        notificationDao.insertNotification(notification)
        val retrieved = notificationDao.getNotificationById("test-notification")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("test-notification", retrieved?.id)
        assertEquals("BILL_DUE_REMINDER", retrieved?.type)
    }
    
    @Test
    fun getPendingNotifications() = runTest {
        // Given
        val notification1 = NotificationEntity(
            id = "notification-1",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Notification 1",
            message = "Message 1",
            scheduledTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            isDelivered = false
        )
        val notification2 = NotificationEntity(
            id = "notification-2",
            type = "SPENDING_LIMIT_ALERT",
            priority = "HIGH",
            title = "Notification 2",
            message = "Message 2",
            scheduledTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            isDelivered = true
        )
        
        // When
        notificationDao.insertNotification(notification1)
        notificationDao.insertNotification(notification2)
        val pending = notificationDao.getPendingNotifications()
        
        // Then
        assertEquals(1, pending.size)
        assertEquals("notification-1", pending[0].id)
        assertFalse(pending[0].isDelivered)
    }
    
    @Test
    fun markNotificationAsDelivered() = runTest {
        // Given
        val notification = NotificationEntity(
            id = "test-notification",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            isDelivered = false
        )
        
        // When
        notificationDao.insertNotification(notification)
        notificationDao.markNotificationAsDelivered("test-notification", LocalDateTime.now())
        val updated = notificationDao.getNotificationById("test-notification")
        
        // Then
        assertNotNull(updated)
        assertTrue(updated?.isDelivered ?: false)
        assertNotNull(updated?.deliveredAt)
    }
    
    @Test
    fun insertAndRetrieveNotificationPreferences() = runTest {
        // Given
        val preferences = NotificationPreferencesEntity(
            id = 1L,
            billRemindersEnabled = false,
            billReminderDaysBefore = 5,
            lowBalanceThreshold = BigDecimal("2000.00")
        )
        
        // When
        notificationDao.insertNotificationPreferences(preferences)
        val retrieved = notificationDao.getNotificationPreferences()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(false, retrieved?.billRemindersEnabled)
        assertEquals(5, retrieved?.billReminderDaysBefore)
        assertEquals(BigDecimal("2000.00"), retrieved?.lowBalanceThreshold)
    }
    
    @Test
    fun observeNotificationPreferences() = runTest {
        // Given
        val preferences = NotificationPreferencesEntity(
            id = 1L,
            billRemindersEnabled = true
        )
        
        // When
        notificationDao.insertNotificationPreferences(preferences)
        val observed = notificationDao.observeNotificationPreferences().first()
        
        // Then
        assertNotNull(observed)
        assertEquals(true, observed?.billRemindersEnabled)
    }
    
    @Test
    fun insertAndRetrieveAccountNotificationSettings() = runTest {
        // Given
        val settings = AccountNotificationSettingsEntity(
            accountId = 1L,
            spendingLimitEnabled = true,
            spendingLimit = BigDecimal("5000.00"),
            lowBalanceEnabled = true,
            lowBalanceThreshold = BigDecimal("1000.00")
        )
        
        // When
        notificationDao.insertAccountNotificationSettings(settings)
        val retrieved = notificationDao.getAccountNotificationSettings(1L)
        
        // Then
        assertNotNull(retrieved)
        assertEquals(1L, retrieved?.accountId)
        assertEquals(true, retrieved?.spendingLimitEnabled)
        assertEquals(BigDecimal("5000.00"), retrieved?.spendingLimit)
    }
    
    @Test
    fun deleteOldNotifications() = runTest {
        // Given
        val oldNotification = NotificationEntity(
            id = "old-notification",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Old Notification",
            message = "Old message",
            scheduledTime = LocalDateTime.now().minusDays(40),
            createdAt = LocalDateTime.now().minusDays(40)
        )
        val recentNotification = NotificationEntity(
            id = "recent-notification",
            type = "SPENDING_LIMIT_ALERT",
            priority = "HIGH",
            title = "Recent Notification",
            message = "Recent message",
            scheduledTime = LocalDateTime.now().minusDays(10),
            createdAt = LocalDateTime.now().minusDays(10)
        )
        
        // When
        notificationDao.insertNotification(oldNotification)
        notificationDao.insertNotification(recentNotification)
        notificationDao.deleteOldNotifications(LocalDateTime.now().minusDays(30))
        
        val remaining = notificationDao.getNotificationHistory(10)
        
        // Then
        assertEquals(1, remaining.size)
        assertEquals("recent-notification", remaining[0].id)
    }
    
    @Test
    fun getNotificationsDueForDelivery() = runTest {
        // Given
        val currentTime = LocalDateTime.now()
        val dueNotification = NotificationEntity(
            id = "due-notification",
            type = "BILL_DUE_REMINDER",
            priority = "NORMAL",
            title = "Due Notification",
            message = "Due message",
            scheduledTime = currentTime.minusMinutes(5),
            createdAt = currentTime.minusHours(1),
            isDelivered = false
        )
        val futureNotification = NotificationEntity(
            id = "future-notification",
            type = "SPENDING_LIMIT_ALERT",
            priority = "HIGH",
            title = "Future Notification",
            message = "Future message",
            scheduledTime = currentTime.plusHours(1),
            createdAt = currentTime,
            isDelivered = false
        )
        
        // When
        notificationDao.insertNotification(dueNotification)
        notificationDao.insertNotification(futureNotification)
        val dueNotifications = notificationDao.getNotificationsDueForDelivery(currentTime)
        
        // Then
        assertEquals(1, dueNotifications.size)
        assertEquals("due-notification", dueNotifications[0].id)
    }
}