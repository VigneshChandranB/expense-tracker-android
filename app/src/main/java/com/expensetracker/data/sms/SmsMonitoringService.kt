package com.expensetracker.data.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.expensetracker.R
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Enhanced foreground service for continuous SMS monitoring with error recovery and battery optimization
 */
@AndroidEntryPoint
class SmsMonitoringService : Service() {
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var smsReader: SmsReader
    
    @Inject
    lateinit var smsProcessor: SmsProcessor
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var permissionMonitoringJob: Job? = null
    private var healthCheckJob: Job? = null
    private var smsReceiver: SmsReceiver? = null
    
    // Service state management
    private val isMonitoring = AtomicBoolean(false)
    private val errorCount = AtomicInteger(0)
    private val lastHealthCheck = AtomicInteger(0)
    
    // Battery optimization handling
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeBatteryOptimization()
        startServiceHealthCheck()
        startPermissionMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_RESTART_MONITORING -> restartMonitoring()
        }
        
        // Return START_STICKY for automatic restart on system kill
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        cleanupService()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep service running even when app is removed from recent tasks
        if (isMonitoring.get()) {
            val restartIntent = Intent(this, SmsMonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            startForegroundService(restartIntent)
        }
    }
    
    private fun startMonitoring() {
        if (isMonitoring.get()) return
        
        if (permissionManager.hasSmsPermissions()) {
            try {
                registerSmsReceiver()
                acquireWakeLock()
                isMonitoring.set(true)
                errorCount.set(0)
                
                val notification = createNotification(
                    title = "SMS Monitoring Active",
                    content = "Automatically tracking your transactions",
                    isError = false
                )
                startForeground(NOTIFICATION_ID, notification)
                
            } catch (e: Exception) {
                handleServiceError("Failed to start monitoring", e)
            }
        } else {
            val notification = createNotification(
                title = "SMS Permission Required",
                content = "Grant SMS permission to enable automatic tracking",
                isError = true
            )
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun stopMonitoring() {
        isMonitoring.set(false)
        unregisterSmsReceiver()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun restartMonitoring() {
        serviceScope.launch {
            try {
                stopMonitoring()
                delay(1000) // Brief delay before restart
                startMonitoring()
            } catch (e: Exception) {
                handleServiceError("Failed to restart monitoring", e)
            }
        }
    }
    
    private fun registerSmsReceiver() {
        if (smsReceiver == null) {
            smsReceiver = SmsReceiver()
            val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                priority = 1000
            }
            registerReceiver(smsReceiver, intentFilter)
        }
    }
    
    private fun unregisterSmsReceiver() {
        smsReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, ignore
            }
            smsReceiver = null
        }
    }
    
    private fun initializeBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Create a partial wake lock for SMS processing
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExpenseTracker:SmsMonitoring"
        ).apply {
            setReferenceCounted(false)
        }
    }
    
    private fun acquireWakeLock() {
        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire(10 * 60 * 1000L) // 10 minutes timeout
            }
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
    }
    
    private fun startServiceHealthCheck() {
        healthCheckJob = serviceScope.launch {
            while (true) {
                delay(HEALTH_CHECK_INTERVAL)
                performHealthCheck()
            }
        }
    }
    
    private fun performHealthCheck() {
        try {
            lastHealthCheck.set((System.currentTimeMillis() / 1000).toInt())
            
            // Check if service is supposed to be monitoring but receiver is not registered
            if (isMonitoring.get() && smsReceiver == null && permissionManager.hasSmsPermissions()) {
                handleServiceError("SMS receiver not registered during monitoring", null)
                restartMonitoring()
            }
            
            // Reset error count if service has been stable
            if (errorCount.get() > 0 && System.currentTimeMillis() - lastHealthCheck.get() * 1000L > ERROR_RESET_INTERVAL) {
                errorCount.set(0)
            }
            
        } catch (e: Exception) {
            handleServiceError("Health check failed", e)
        }
    }
    
    private fun handleServiceError(message: String, exception: Exception?) {
        val currentErrors = errorCount.incrementAndGet()
        
        // Log error for debugging
        exception?.printStackTrace()
        
        // Update notification to show error state
        val notification = createNotification(
            title = "SMS Monitoring Error",
            content = "Service encountered an error. Attempting recovery... (Attempt $currentErrors/$MAX_ERROR_COUNT)",
            isError = true
        )
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Implement exponential backoff for error recovery
        if (currentErrors < MAX_ERROR_COUNT) {
            serviceScope.launch {
                val backoffDelay = minOf(
                    INITIAL_BACKOFF_DELAY * (1L shl (currentErrors - 1)),
                    MAX_BACKOFF_DELAY
                )
                delay(backoffDelay)
                
                if (isMonitoring.get()) {
                    try {
                        // Attempt to recover by restarting monitoring
                        restartMonitoring()
                        
                        // If restart succeeds, update notification
                        val recoveryNotification = createNotification(
                            title = "SMS Monitoring Recovered",
                            content = "Service recovered successfully after error",
                            isError = false
                        )
                        notificationManager.notify(NOTIFICATION_ID, recoveryNotification)
                        
                    } catch (e: Exception) {
                        handleServiceError("Recovery attempt failed", e)
                    }
                }
            }
        } else {
            // Too many errors, stop service and notify user
            val errorNotification = createNotification(
                title = "SMS Monitoring Failed",
                content = "Service stopped after $MAX_ERROR_COUNT failed attempts. Check app settings.",
                isError = true
            )
            
            notificationManager.notify(NOTIFICATION_ID, errorNotification)
            
            // Reset monitoring state and stop service
            isMonitoring.set(false)
            stopMonitoring()
        }
    }
    
    private fun startPermissionMonitoring() {
        permissionMonitoringJob = permissionManager.smsPermissionState
            .onEach { permissionStatus ->
                updateNotificationBasedOnPermission(permissionStatus)
                
                // Restart monitoring if permission was granted
                if (permissionStatus == com.expensetracker.domain.permission.PermissionStatus.GRANTED && 
                    !isMonitoring.get()) {
                    startMonitoring()
                } else if (permissionStatus != com.expensetracker.domain.permission.PermissionStatus.GRANTED && 
                          isMonitoring.get()) {
                    // Stop monitoring if permission was revoked
                    isMonitoring.set(false)
                    unregisterSmsReceiver()
                }
            }
            .launchIn(serviceScope)
    }
    
    private fun updateNotificationBasedOnPermission(permissionStatus: com.expensetracker.domain.permission.PermissionStatus) {
        val notification = when (permissionStatus) {
            com.expensetracker.domain.permission.PermissionStatus.GRANTED -> {
                createNotification(
                    title = "SMS Monitoring Active",
                    content = "Automatically tracking your transactions",
                    isError = false
                )
            }
            com.expensetracker.domain.permission.PermissionStatus.DENIED -> {
                createNotification(
                    title = "SMS Permission Required",
                    content = "Grant SMS permission to enable automatic tracking",
                    isError = true
                )
            }
            com.expensetracker.domain.permission.PermissionStatus.PERMANENTLY_DENIED -> {
                createNotification(
                    title = "SMS Permission Denied",
                    content = "Enable SMS permission in app settings for automatic tracking",
                    isError = true
                )
            }
        }
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors SMS messages for transaction tracking"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, content: String, isError: Boolean = false): Notification {
        // Create intent to open the app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (isError) R.drawable.ic_error else R.drawable.ic_notification)
            .setOngoing(!isError)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun cleanupService() {
        isMonitoring.set(false)
        permissionMonitoringJob?.cancel()
        healthCheckJob?.cancel()
        unregisterSmsReceiver()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    companion object {
        const val ACTION_START_MONITORING = "com.expensetracker.START_SMS_MONITORING"
        const val ACTION_STOP_MONITORING = "com.expensetracker.STOP_SMS_MONITORING"
        const val ACTION_RESTART_MONITORING = "com.expensetracker.RESTART_SMS_MONITORING"
        
        private const val CHANNEL_ID = "sms_monitoring_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Health check and error recovery constants
        private const val HEALTH_CHECK_INTERVAL = 30_000L // 30 seconds
        private const val ERROR_RESET_INTERVAL = 300_000L // 5 minutes
        private const val MAX_ERROR_COUNT = 5
        private const val INITIAL_BACKOFF_DELAY = 1_000L // 1 second
        private const val MAX_BACKOFF_DELAY = 60_000L // 1 minute
    }
}