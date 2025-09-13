package com.expensetracker.domain.usecase.notification

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.service.NotificationService
import com.expensetracker.domain.usecase.analytics.DetectSpendingAnomaliesUseCase
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

/**
 * Main use case for managing all notification types and triggers
 */
class NotificationManagerUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val detectSpendingAnomaliesUseCase: DetectSpendingAnomaliesUseCase,
    private val sendBillReminderUseCase: SendBillReminderUseCase,
    private val sendSpendingLimitAlertUseCase: SendSpendingLimitAlertUseCase,
    private val sendLowBalanceWarningUseCase: SendLowBalanceWarningUseCase,
    private val sendUnusualSpendingAlertUseCase: SendUnusualSpendingAlertUseCase
) {
    
    /**
     * Check and send all applicable notifications
     */
    suspend fun checkAndSendNotifications() {
        val preferences = notificationRepository.getNotificationPreferences()
        
        if (preferences.billRemindersEnabled) {
            checkBillReminders()
        }
        
        if (preferences.spendingLimitAlertsEnabled) {
            checkSpendingLimits()
        }
        
        if (preferences.lowBalanceWarningsEnabled) {
            checkLowBalances()
        }
        
        if (preferences.unusualSpendingAlertsEnabled) {
            checkUnusualSpending()
        }
    }
    
    /**
     * Process a new transaction and check for immediate notifications
     */
    suspend fun processTransactionNotifications(transaction: Transaction) {
        val preferences = notificationRepository.getNotificationPreferences()
        
        // Check for large transaction alerts
        if (preferences.largeTransactionAlertsEnabled && 
            transaction.amount >= preferences.largeTransactionThreshold) {
            sendLargeTransactionAlert(transaction)
        }
        
        // Check spending limits after new transaction
        if (preferences.spendingLimitAlertsEnabled) {
            checkSpendingLimitForAccount(transaction.accountId)
        }
        
        // Check low balance after expense transaction
        if (preferences.lowBalanceWarningsEnabled && 
            transaction.type == TransactionType.EXPENSE) {
            checkLowBalanceForAccount(transaction.accountId)
        }
        
        // Check for unusual spending patterns
        if (preferences.unusualSpendingAlertsEnabled) {
            checkUnusualSpendingForTransaction(transaction)
        }
    }
    
    private suspend fun checkBillReminders() {
        // This would typically check a bills/recurring transactions table
        // For now, we'll check for recurring transactions that might be bills
        val recurringTransactions = transactionRepository.getRecurringTransactions().first()
        
        recurringTransactions.forEach { transaction ->
            if (transaction.type == TransactionType.EXPENSE) {
                val nextDueDate = calculateNextDueDate(transaction)
                val billReminderData = BillReminderData(
                    billName = transaction.merchant,
                    dueDate = nextDueDate,
                    amount = transaction.amount,
                    accountId = transaction.accountId,
                    category = transaction.category
                )
                sendBillReminderUseCase(billReminderData)
            }
        }
    }
    
    private suspend fun checkSpendingLimits() {
        val accounts = accountRepository.getAllAccounts().first()
        accounts.forEach { account ->
            checkSpendingLimitForAccount(account.id)
        }
    }
    
    private suspend fun checkSpendingLimitForAccount(accountId: Long) {
        val preferences = notificationRepository.getNotificationPreferences()
        val accountSettings = preferences.accountSpecificSettings[accountId]
        val spendingLimit = accountSettings?.spendingLimit
        
        if (spendingLimit != null && accountSettings.spendingLimitEnabled) {
            val currentMonth = YearMonth.now()
            val monthlySpending = transactionRepository.getMonthlySpendingForAccount(accountId, currentMonth).first()
            
            if (monthlySpending >= spendingLimit.multiply(0.8.toBigDecimal())) { // 80% threshold
                val account = accountRepository.getAccountById(accountId).first()
                account?.let {
                    val alertData = SpendingLimitAlertData(
                        accountId = accountId,
                        accountName = it.nickname,
                        currentSpending = monthlySpending,
                        spendingLimit = spendingLimit,
                        period = "monthly"
                    )
                    sendSpendingLimitAlertUseCase(alertData)
                }
            }
        }
    }
    
    private suspend fun checkLowBalances() {
        val accounts = accountRepository.getAllAccounts().first()
        accounts.forEach { account ->
            checkLowBalanceForAccount(account.id)
        }
    }
    
    private suspend fun checkLowBalanceForAccount(accountId: Long) {
        val preferences = notificationRepository.getNotificationPreferences()
        val account = accountRepository.getAccountById(accountId).first()
        
        account?.let {
            val accountSettings = preferences.accountSpecificSettings[accountId]
            val threshold = accountSettings?.lowBalanceThreshold ?: preferences.lowBalanceThreshold
            
            if (it.currentBalance <= threshold) {
                val warningData = LowBalanceWarningData(
                    accountId = accountId,
                    accountName = it.nickname,
                    currentBalance = it.currentBalance,
                    threshold = threshold
                )
                sendLowBalanceWarningUseCase(warningData)
            }
        }
    }
    
    private suspend fun checkUnusualSpending() {
        val anomalies = detectSpendingAnomaliesUseCase()
        
        anomalies.forEach { anomaly ->
            anomaly.account?.let { account ->
                val alertData = UnusualSpendingAlertData(
                    anomaly = anomaly,
                    accountId = account.id,
                    accountName = account.nickname,
                    description = anomaly.description,
                    suggestedAction = anomaly.suggestedAction
                )
                sendUnusualSpendingAlertUseCase(alertData)
            }
        }
    }
    
    private suspend fun checkUnusualSpendingForTransaction(transaction: Transaction) {
        // Check for immediate anomalies related to this specific transaction
        val anomalies = detectSpendingAnomaliesUseCase()
        
        anomalies.filter { anomaly ->
            anomaly.relatedTransactions.any { it.id == transaction.id }
        }.forEach { anomaly ->
            val account = accountRepository.getAccountById(transaction.accountId).first()
            account?.let {
                val alertData = UnusualSpendingAlertData(
                    anomaly = anomaly,
                    accountId = transaction.accountId,
                    accountName = it.nickname,
                    description = anomaly.description,
                    suggestedAction = anomaly.suggestedAction
                )
                sendUnusualSpendingAlertUseCase(alertData)
            }
        }
    }
    
    private suspend fun sendLargeTransactionAlert(transaction: Transaction) {
        val account = accountRepository.getAccountById(transaction.accountId).first()
        account?.let {
            val notification = NotificationData(
                id = "large_transaction_${transaction.id}",
                type = NotificationType.LARGE_TRANSACTION_ALERT,
                priority = NotificationPriority.HIGH,
                title = "Large Transaction Alert",
                message = "Large ${transaction.type.name.lowercase()} of ₹${transaction.amount} at ${transaction.merchant} on ${it.nickname}",
                accountId = transaction.accountId,
                transactionId = transaction.id,
                amount = transaction.amount,
                category = transaction.category,
                scheduledTime = LocalDateTime.now(),
                actionData = mapOf(
                    "transactionId" to transaction.id.toString(),
                    "accountId" to transaction.accountId.toString(),
                    "merchant" to transaction.merchant,
                    "amount" to transaction.amount.toString()
                )
            )
            
            notificationService.sendNotification(notification)
            notificationRepository.markNotificationAsDelivered(notification.id)
        }
    }
    
    private fun calculateNextDueDate(transaction: Transaction): LocalDateTime {
        // Simple logic - assume monthly recurring transactions
        // In a real app, this would be more sophisticated
        return transaction.date.plusMonths(1)
    }
}