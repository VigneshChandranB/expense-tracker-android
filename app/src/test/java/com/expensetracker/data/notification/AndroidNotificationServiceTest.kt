package com.expensetracker.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.expensetracker.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class AndroidNotificationServiceTest {
    
    private val context = mockk<Context>()
    private val notificationManager = mockk<NotificationManagerCompat>()
    
    private lateinit var notificationService: AndroidNotificationService
    
    @Before
    fun setup() {
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notificationManager
        
        notificationService = AndroidNotificationService(context)
    }
    
    @Test
    fun `should send notification when permissions are granted`() = runTest {
        // Given
        val notification = NotificationData(
            id = "test-notification",
            type = NotificationType.BILL_DUE_REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now()
        )
        
        every { notificationManager.areNotificationsEnabled() } returns true
        every { notificationManager.notify(any(), any()) } just Runs
        
        // When
        notificationService.sendNotification(notification)
        
        // Then
        verify { notificationManager.notify(any(), any()) }
    }
    
    @Test
    fun `should not send notification when permissions are denied`() = runTest {
        // Given
        val notification = NotificationData(
            id = "test-notification",
            type = NotificationType.BILL_DUE_REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now()
        )
        
        every { notificationManager.areNotificationsEnabled() } returns false
        
        // When
        notificationService.sendNotification(notification)
        
        // Then
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }
    
    @Test
    fun `should cancel notification by id`() = runTest {
        // Given
        val notificationId = "test-notification"
        
        every { notificationManager.cancel(any()) } just Runs
        
        // When
        notificationService.cancelNotification(notificationId)
        
        // Then
        verify { notificationManager.cancel(notificationId.hashCode()) }
    }
    
    @Test
    fun `should create notification channels on initialization`() = runTest {
        // Given
        val systemNotificationManager = mockk<android.app.NotificationManager>()
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns systemNotificationManager
        every { systemNotificationManager.createNotificationChannel(any()) } just Runs
        
        // When
        notificationService.createNotificationChannels()
        
        // Then
        verify(atLeast = 1) { systemNotificationManager.createNotificationChannel(any()) }
    }
    
    @Test
    fun `should handle security exception gracefully`() = runTest {
        // Given
        val notification = NotificationData(
            id = "test-notification",
            type = NotificationType.BILL_DUE_REMINDER,
            priority = NotificationPriority.NORMAL,
            title = "Test Notification",
            message = "Test message",
            scheduledTime = LocalDateTime.now()
        )
        
        every { notificationManager.areNotificationsEnabled() } returns true
        every { notificationManager.notify(any(), any()) } throws SecurityException("Permission denied")
        
        // When & Then - should not throw exception
        notificationService.sendNotification(notification)
    }
    
    @Test
    fun `should set correct priority for urgent notifications`() = runTest {
        // Given
        val notification = NotificationData(
            id = "urgent-notification",
            type = NotificationType.LOW_BALANCE_WARNING,
            priority = NotificationPriority.URGENT,
            title = "Urgent Alert",
            message = "Critical balance warning",
            scheduledTime = LocalDateTime.now(),
            amount = BigDecimal("100.00")
        )
        
        every { notificationManager.areNotificationsEnabled() } returns true
        every { notificationManager.notify(any(), any()) } just Runs
        
        // When
        notificationService.sendNotification(notification)
        
        // Then
        verify { 
            notificationManager.notify(
                any(), 
                match<android.app.Notification> { 
                    it.priority == android.app.Notification.PRIORITY_MAX 
                }
            ) 
        }
    }
}