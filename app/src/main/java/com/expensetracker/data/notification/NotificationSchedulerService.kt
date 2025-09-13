package com.expensetracker.data.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.expensetracker.data.repository.NotificationRepositoryImpl
import com.expensetracker.domain.service.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Background service for processing scheduled notifications
 */
@AndroidEntryPoint
class NotificationSchedulerService : Service() {
    
    @Inject
    lateinit var notificationRepository: NotificationRepositoryImpl
    
    @Inject
    lateinit var notificationService: NotificationService
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var schedulerJob: Job? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNotificationScheduler()
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        schedulerJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun startNotificationScheduler() {
        schedulerJob = serviceScope.launch {
            while (isActive) {
                try {
                    processScheduledNotifications()
                    delay(60_000) // Check every minute
                } catch (e: Exception) {
                    // Log error and continue
                    delay(60_000)
                }
            }
        }
    }
    
    private suspend fun processScheduledNotifications() {
        val dueNotifications = notificationRepository.getNotificationsDueForDelivery()
        
        dueNotifications.forEach { notification ->
            try {
                notificationService.sendNotification(notification)
                notificationRepository.markNotificationAsDelivered(notification.id)
            } catch (e: Exception) {
                // Log error but continue with other notifications
            }
        }
        
        // Clean up old notifications
        if (LocalDateTime.now().hour == 2) { // Run cleanup at 2 AM
            notificationRepository.clearOldNotifications(30)
        }
    }
}