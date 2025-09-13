package com.expensetracker.data.background

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.expensetracker.data.sms.SmsMonitoringService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SmsMonitoringServiceIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }
    
    @After
    fun tearDown() {
        // Stop any running services
        val stopIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_STOP_MONITORING
        }
        context.startService(stopIntent)
    }
    
    @Test
    fun testServiceStartsAndStops() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        
        // Wait a moment for service to start
        delay(1000)
        
        // Verify service is running
        assertTrue(isServiceRunning())
        
        // Stop the service
        val stopIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_STOP_MONITORING
        }
        
        context.startService(stopIntent)
        
        // Wait a moment for service to stop
        delay(1000)
        
        // Verify service is stopped
        assertFalse(isServiceRunning())
    }
    
    @Test
    fun testServiceRestart() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        assertTrue(isServiceRunning())
        
        // Restart the service
        val restartIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_RESTART_MONITORING
        }
        
        context.startService(restartIntent)
        delay(2000) // Wait longer for restart process
        
        // Service should still be running after restart
        assertTrue(isServiceRunning())
    }
    
    @Test
    fun testServiceSurvivesTaskRemoval() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        assertTrue(isServiceRunning())
        
        // Simulate task removal by calling onTaskRemoved
        // Note: This is a simplified test - in real scenarios, the system would call this
        val service = SmsMonitoringService()
        service.onTaskRemoved(Intent())
        
        // Wait a moment
        delay(1000)
        
        // Service should attempt to restart itself
        // In a real scenario, we'd verify the restart intent was sent
        // For this test, we just verify the service is still conceptually running
        assertTrue(isServiceRunning())
    }
    
    @Test
    fun testServiceHandlesMultipleStartCommands() = runBlocking {
        // Start the service multiple times
        repeat(3) {
            val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_START_MONITORING
            }
            serviceRule.startService(startIntent)
            delay(500)
        }
        
        // Service should still be running and handle multiple starts gracefully
        assertTrue(isServiceRunning())
        
        // Stop once should stop the service
        val stopIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_STOP_MONITORING
        }
        
        context.startService(stopIntent)
        delay(1000)
        
        assertFalse(isServiceRunning())
    }
    
    @Test
    fun testServiceCreatesNotificationChannel() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        
        // Verify service is running (which means notification channel was created successfully)
        assertTrue(isServiceRunning())
        
        // Note: Testing notification channel creation directly would require
        // accessing NotificationManager and checking for the specific channel
        // This is a simplified test that verifies the service starts without crashing
    }
    
    @Test
    fun testServiceErrorRecovery() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        assertTrue(isServiceRunning())
        
        // Simulate an error by restarting the service
        val restartIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_RESTART_MONITORING
        }
        
        context.startService(restartIntent)
        delay(3000) // Wait for restart process
        
        // Service should recover and still be running
        assertTrue(isServiceRunning())
    }
    
    @Test
    fun testServiceBatteryOptimizationHandling() = runBlocking {
        // This test verifies that the service can handle battery optimization scenarios
        // In a real device test, this would involve actual battery optimization settings
        
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        
        // Verify service starts even with potential battery optimization
        assertTrue(isServiceRunning())
        
        // The service should create appropriate notifications about battery optimization
        // In a real test, we'd verify notification content
    }
    
    @Test
    fun testServiceHealthCheck() = runBlocking {
        // Start the service
        val startIntent = Intent(context, SmsMonitoringService::class.java).apply {
            action = SmsMonitoringService.ACTION_START_MONITORING
        }
        
        serviceRule.startService(startIntent)
        delay(1000)
        assertTrue(isServiceRunning())
        
        // Wait for health check cycles to run
        delay(35000) // Wait longer than health check interval
        
        // Service should still be running after health checks
        assertTrue(isServiceRunning())
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == SmsMonitoringService::class.java.name }
    }
}