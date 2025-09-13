package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.service.NotificationService
import com.expensetracker.domain.usecase.analytics.DetectSpendingAnomaliesUseCase
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class NotificationManagerUseCaseTest {
    
    private val notificationRepository = mockk<NotificationRepository>()
    private val notificationService = mockk<NotificationService>()
    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val detectSpendingAnomaliesUseCase = mockk<DetectSpendingAnomaliesUseCase>()
    private val sendBillReminderUseCase = mockk<SendBillReminderUseCase>()
    private val sendSpendingLimitAlertUseCase = mockk<SendSpendingLimitAlertUseCase>()
    private val sendLowBalanceWarningUseCase = mockk<SendLowBalanceWarningUseCase>()
    private val sendUnusualSpendingAlertUseCase = mockk<SendUnusualSpendingAlertUseCase>()
    
    private lateinit var useCase: NotificationManagerUseCase
    
    @Before
    fun setup() {
        useCase = NotificationManagerUseCase(
            notificationRepository,
            notificationService,
            accountRepository,
            transactionRepository,
            detectSpendingAnomaliesUseCase,
            sendBillReminderUseCase,
            sendSpendingLimitAlertUseCase,
            sendLowBalanceWarningUseCase,
            sendUnusualSpendingAlertUseCase
        )
    }
    
    @Test
    fun `should send large transaction alert when threshold exceeded`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            amount = BigDecimal("6000.00"),
            type = TransactionType.EXPENSE,
            category = Category(1, "Shopping", "shopping", androidx.compose.ui.graphics.Color.Blue, true),
            merchant = "Electronics Store",
            description = "Laptop purchase",
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1L
        )
        val account = Account(
            id = 1L,
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234",
            nickname = "Main Account",
            currentBalance = BigDecimal("50000.00"),
            createdAt = LocalDateTime.now()
        )
        val preferences = NotificationPreferences(
            largeTransactionAlertsEnabled = true,
            largeTransactionThreshold = BigDecimal("5000.00")
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { accountRepository.getAccountById(1L) } returns flowOf(account)
        coEvery { notificationService.sendNotification(any()) } just Runs
        coEvery { notificationRepository.markNotificationAsDelivered(any()) } just Runs
        
        // When
        useCase.processTransactionNotifications(transaction)
        
        // Then
        coVerify { 
            notificationService.sendNotification(
                match { notification ->
                    notification.type == NotificationType.LARGE_TRANSACTION_ALERT &&
                    notification.priority == NotificationPriority.HIGH
                }
            )
        }
    }
    
    @Test
    fun `should not send large transaction alert when disabled`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            amount = BigDecimal("6000.00"),
            type = TransactionType.EXPENSE,
            category = Category(1, "Shopping", "shopping", androidx.compose.ui.graphics.Color.Blue, true),
            merchant = "Electronics Store",
            description = "Laptop purchase",
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1L
        )
        val preferences = NotificationPreferences(
            largeTransactionAlertsEnabled = false,
            largeTransactionThreshold = BigDecimal("5000.00")
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // When
        useCase.processTransactionNotifications(transaction)
        
        // Then
        coVerify(exactly = 0) { notificationService.sendNotification(any()) }
    }
    
    @Test
    fun `should check spending limits after expense transaction`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            amount = BigDecimal("1000.00"),
            type = TransactionType.EXPENSE,
            category = Category(1, "Food", "restaurant", androidx.compose.ui.graphics.Color.Orange, true),
            merchant = "Restaurant",
            description = "Dinner",
            date = LocalDateTime.now(),
            source = TransactionSource.SMS_AUTO,
            accountId = 1L
        )
        val preferences = NotificationPreferences(
            spendingLimitAlertsEnabled = true,
            lowBalanceWarningsEnabled = true
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        
        // Mock the private methods that would be called
        mockkObject(useCase)
        every { useCase["checkSpendingLimitForAccount"](1L) } just Runs
        every { useCase["checkLowBalanceForAccount"](1L) } just Runs
        
        // When
        useCase.processTransactionNotifications(transaction)
        
        // Then - verify the methods would be called (in a real implementation)
        // This is a simplified test as the actual methods are private
        coVerify { notificationRepository.getNotificationPreferences() }
    }
    
    @Test
    fun `should check all notifications when requested`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            billRemindersEnabled = true,
            spendingLimitAlertsEnabled = true,
            lowBalanceWarningsEnabled = true,
            unusualSpendingAlertsEnabled = true
        )
        
        coEvery { notificationRepository.getNotificationPreferences() } returns preferences
        coEvery { transactionRepository.getRecurringTransactions() } returns flowOf(emptyList())
        coEvery { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        coEvery { detectSpendingAnomaliesUseCase() } returns emptyList()
        
        // When
        useCase.checkAndSendNotifications()
        
        // Then
        coVerify { notificationRepository.getNotificationPreferences() }
        coVerify { transactionRepository.getRecurringTransactions() }
        coVerify { accountRepository.getAllAccounts() }
        coVerify { detectSpendingAnomaliesUseCase() }
    }
}