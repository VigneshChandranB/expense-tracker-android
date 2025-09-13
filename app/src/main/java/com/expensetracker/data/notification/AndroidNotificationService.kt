package com.expensetracker.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expensetracker.R
import com.expensetracker.domain.model.NotificationData
import com.expensetracker.domain.model.NotificationPriority
import com.expensetracker.domain.model.NotificationType
import com.expensetracker.domain.service.NotificationService
import com.expensetracker.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of NotificationService
 */
@Singleton
class AndroidNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationService {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        private const val CHANNEL_BILLS = "bills_channel"
        private const val CHANNEL_SPENDING = "spending_channel"
        private const val CHANNEL_BALANCE = "balance_channel"
        private const val CHANNEL_ALERTS = "alerts_channel"
        private const val CHANNEL_GENERAL = "general_channel"
        
        private const val REQUEST_CODE_MAIN = 1001
        private const val REQUEST_CODE_TRANSACTION = 1002
        private const val REQUEST_CODE_ACCOUNT = 1003
    }
    
    override suspend fun sendNotification(notification: NotificationData) {
        if (!areNotificationsEnabled()) {
            return
        }
        
        val channelId = getChannelIdForType(notification.type)
        val androidNotification = buildNotification(notification, channelId)
        
        try {
            notificationManager.notify(notification.id.hashCode(), androidNotification)
        } catch (e: SecurityException) {
            // Handle case where notification permission was revoked
        }
    }
    
    override suspend fun scheduleNotification(notification: NotificationData) {
        // For scheduled notifications, we would typically use AlarmManager or WorkManager
        // For now, we'll just send immediately if the time has passed
        if (notification.scheduledTime.isBefore(java.time.LocalDateTime.now())) {
            sendNotification(notification)
        }
        // In a real implementation, you would schedule with AlarmManager or WorkManager
    }
    
    override suspend fun cancelNotification(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }
    
    override suspend fun requestNotificationPermission(): Boolean {
        // This would typically be handled by the UI layer
        // Return current permission status
        return areNotificationsEnabled()
    }
    
    override suspend fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_BILLS,
                    "Bill Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for upcoming bill due dates"
                },
                NotificationChannel(
                    CHANNEL_SPENDING,
                    "Spending Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when spending limits are reached"
                },
                NotificationChannel(
                    CHANNEL_BALANCE,
                    "Balance Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Warnings for low account balances"
                },
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Unusual Activity",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for unusual spending patterns"
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General app notifications"
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun buildNotification(notification: NotificationData, channelId: String): android.app.Notification {
        val intent = createIntentForNotification(notification)
        val pendingIntent = PendingIntent.getActivity(
            context,
            getRequestCodeForType(notification.type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val priority = when (notification.priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getIconForType(notification.type))
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
        
        // Add amount if available
        notification.amount?.let { amount ->
            builder.setSubText("₹${amount}")
        }
        
        // Add action buttons based on notification type
        addActionButtons(builder, notification)
        
        return builder.build()
    }
    
    private fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.BILL_DUE_REMINDER -> CHANNEL_BILLS
            NotificationType.SPENDING_LIMIT_ALERT, NotificationType.BUDGET_EXCEEDED -> CHANNEL_SPENDING
            NotificationType.LOW_BALANCE_WARNING -> CHANNEL_BALANCE
            NotificationType.UNUSUAL_SPENDING_ALERT, NotificationType.LARGE_TRANSACTION_ALERT -> CHANNEL_ALERTS
        }
    }
    
    private fun getIconForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.BILL_DUE_REMINDER -> R.drawable.ic_receipt_24
            NotificationType.SPENDING_LIMIT_ALERT, NotificationType.BUDGET_EXCEEDED -> R.drawable.ic_trending_up_24
            NotificationType.LOW_BALANCE_WARNING -> R.drawable.ic_account_balance_wallet_24
            NotificationType.UNUSUAL_SPENDING_ALERT, NotificationType.LARGE_TRANSACTION_ALERT -> R.drawable.ic_warning_24
        }
    }
    
    private fun getRequestCodeForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.BILL_DUE_REMINDER -> REQUEST_CODE_MAIN
            NotificationType.SPENDING_LIMIT_ALERT, NotificationType.BUDGET_EXCEEDED -> REQUEST_CODE_ACCOUNT
            NotificationType.LOW_BALANCE_WARNING -> REQUEST_CODE_ACCOUNT
            NotificationType.UNUSUAL_SPENDING_ALERT, NotificationType.LARGE_TRANSACTION_ALERT -> REQUEST_CODE_TRANSACTION
        }
    }
    
    private fun createIntentForNotification(notification: NotificationData): Intent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Add extras based on notification type
        when (notification.type) {
            NotificationType.BILL_DUE_REMINDER -> {
                intent.putExtra("navigate_to", "bills")
                notification.accountId?.let { intent.putExtra("account_id", it) }
            }
            NotificationType.SPENDING_LIMIT_ALERT, NotificationType.BUDGET_EXCEEDED -> {
                intent.putExtra("navigate_to", "spending")
                notification.accountId?.let { intent.putExtra("account_id", it) }
            }
            NotificationType.LOW_BALANCE_WARNING -> {
                intent.putExtra("navigate_to", "accounts")
                notification.accountId?.let { intent.putExtra("account_id", it) }
            }
            NotificationType.UNUSUAL_SPENDING_ALERT, NotificationType.LARGE_TRANSACTION_ALERT -> {
                intent.putExtra("navigate_to", "transactions")
                notification.transactionId?.let { intent.putExtra("transaction_id", it) }
                notification.accountId?.let { intent.putExtra("account_id", it) }
            }
        }
        
        return intent
    }
    
    private fun addActionButtons(builder: NotificationCompat.Builder, notification: NotificationData) {
        when (notification.type) {
            NotificationType.BILL_DUE_REMINDER -> {
                // Add "Mark as Paid" action
                val markPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "MARK_BILL_PAID"
                    putExtra("notification_id", notification.id)
                    notification.accountId?.let { putExtra("account_id", it) }
                }
                val markPaidPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notification.id.hashCode(),
                    markPaidIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_check_24, "Mark as Paid", markPaidPendingIntent)
            }
            NotificationType.LARGE_TRANSACTION_ALERT -> {
                // Add "View Transaction" action
                val viewIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("navigate_to", "transaction_detail")
                    notification.transactionId?.let { putExtra("transaction_id", it) }
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context,
                    notification.id.hashCode(),
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_visibility_24, "View", viewPendingIntent)
            }
            else -> {
                // Default action - open app
            }
        }
    }
}