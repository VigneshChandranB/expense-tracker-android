package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SendSpendingLimitAlertUseCaseTest {
    
    private val notificationRepository = mockk<NotificationRepository>()
    private val notificationService = mockk<NotificationService>()
    
    private lateinit var useCase: SendSpendingLimitAlertUseCase
    
    @Before
    fun setup() {
        useCase = SendSpendingLimitAlertUseCase(notificationRepository, notificationService)
    }
    
    @Test
    fun `should send urgent alert when spending exceeds limit`() = runTest {
        // Given
        val preferences = NotificationPreferences(spendingLimitAlertsEnabled = true)
        val alertData = SpendingLimitAlertData(
            accountId = 1L,
            accountName = "Main Account",
            currentSpending = BigDecimal("11000.00"),
            spendingLimit = BigDecimal("10000.00"),
            period = "monthly"
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { notificationService.sendNotification(any()) } just Runs
        coEvery { notificationRepository.markNotificationAsDelivered(any()) } just Runs
        
        // When
        useCase(alertData)
        
        // Then
        coVerify { 
            notificationService.sendNotification(
                match { notification ->
                    notification.priority == NotificationPriority.URGENT &&
                    notification.type == NotificationType.SPENDING_LIMIT_ALERT
                }
            )
        }
    }
    
    @Test
    fun `should send high priority alert when spending is 90% of limit`() = runTest {
        // Given
        val preferences = NotificationPreferences(spendingLimitAlertsEnabled = true)
        val alertData = SpendingLimitAlertData(
            accountId = 1L,
            accountName = "Main Account",
            currentSpending = BigDecimal("9500.00"),
            spendingLimit = BigDecimal("10000.00"),
            period = "monthly"
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { notificationService.sendNotification(any()) } just Runs
        coEvery { notificationRepository.markNotificationAsDelivered(any()) } just Runs
        
        // When
        useCase(alertData)
        
        // Then
        coVerify { 
            notificationService.sendNotification(
                match { notification ->
                    notification.priority == NotificationPriority.HIGH
                }
            )
        }
    }
    
    @Test
    fun `should not send alert when disabled globally`() = runTest {
        // Given
        val preferences = NotificationPreferences(spendingLimitAlertsEnabled = false)
        val alertData = SpendingLimitAlertData(
            accountId = 1L,
            accountName = "Main Account",
            currentSpending = BigDecimal("11000.00"),
            spendingLimit = BigDecimal("10000.00"),
            period = "monthly"
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // When
        useCase(alertData)
        
        // Then
        coVerify(exactly = 0) { notificationService.sendNotification(any()) }
    }
    
    @Test
    fun `should not send alert when disabled for specific account`() = runTest {
        // Given
        val accountSettings = mapOf(
            1L to AccountNotificationSettings(
                accountId = 1L,
                spendingLimitEnabled = false
            )
        )
        val preferences = NotificationPreferences(
            spendingLimitAlertsEnabled = true,
            accountSpecificSettings = accountSettings
        )
        val alertData = SpendingLimitAlertData(
            accountId = 1L,
            accountName = "Main Account",
            currentSpending = BigDecimal("11000.00"),
            spendingLimit = BigDecimal("10000.00"),
            period = "monthly"
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // When
        useCase(alertData)
        
        // Then
        coVerify(exactly = 0) { notificationService.sendNotification(any()) }
    }
}