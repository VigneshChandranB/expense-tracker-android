package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class SendBillReminderUseCaseTest {
    
    private val notificationRepository = mockk<NotificationRepository>()
    private val notificationService = mockk<NotificationService>()
    
    private lateinit var useCase: SendBillReminderUseCase
    
    @Before
    fun setup() {
        useCase = SendBillReminderUseCase(notificationRepository, notificationService)
    }
    
    @Test
    fun `should schedule bill reminder when enabled`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            billRemindersEnabled = true,
            billReminderDaysBefore = 3
        )
        val billData = BillReminderData(
            billName = "Electricity Bill",
            dueDate = LocalDateTime.now().plusDays(5),
            amount = BigDecimal("1500.00"),
            accountId = 1L,
            category = Category(1, "Bills", "receipt", androidx.compose.ui.graphics.Color.Red, true)
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { notificationRepository.scheduleNotification(any()) } just Runs
        
        // When
        useCase(billData)
        
        // Then
        coVerify { notificationRepository.scheduleNotification(any()) }
    }
    
    @Test
    fun `should not schedule reminder when disabled`() = runTest {
        // Given
        val preferences = NotificationPreferences(billRemindersEnabled = false)
        val billData = BillReminderData(
            billName = "Electricity Bill",
            dueDate = LocalDateTime.now().plusDays(5),
            amount = BigDecimal("1500.00"),
            accountId = 1L,
            category = Category(1, "Bills", "receipt", androidx.compose.ui.graphics.Color.Red, true)
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // When
        useCase(billData)
        
        // Then
        coVerify(exactly = 0) { notificationRepository.scheduleNotification(any()) }
    }
    
    @Test
    fun `should not schedule reminder for past due dates`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            billRemindersEnabled = true,
            billReminderDaysBefore = 3
        )
        val billData = BillReminderData(
            billName = "Electricity Bill",
            dueDate = LocalDateTime.now().minusDays(1), // Past date
            amount = BigDecimal("1500.00"),
            accountId = 1L,
            category = Category(1, "Bills", "receipt", androidx.compose.ui.graphics.Color.Red, true)
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // When
        useCase(billData)
        
        // Then
        coVerify(exactly = 0) { notificationRepository.scheduleNotification(any()) }
    }
    
    @Test
    fun `should adjust time for quiet hours`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            billRemindersEnabled = true,
            billReminderDaysBefore = 1,
            quietHoursEnabled = true,
            quietHoursStart = 22,
            quietHoursEnd = 8
        )
        val billData = BillReminderData(
            billName = "Electricity Bill",
            dueDate = LocalDateTime.now().plusDays(2).withHour(23), // During quiet hours
            amount = BigDecimal("1500.00"),
            accountId = 1L,
            category = Category(1, "Bills", "receipt", androidx.compose.ui.graphics.Color.Red, true)
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { notificationRepository.scheduleNotification(any()) } just Runs
        
        // When
        useCase(billData)
        
        // Then
        coVerify { 
            notificationRepository.scheduleNotification(
                match { notification ->
                    notification.scheduledTime.hour == 8 // Adjusted to end of quiet hours
                }
            )
        }
    }
}