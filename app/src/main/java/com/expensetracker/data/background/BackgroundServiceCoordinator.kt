package com.expensetracker.data.background

import android.content.Context
import android.content.Intent
import android.os.Build
import com.expensetracker.data.sms.SmsMonitoringService
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.sms.SmsServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator for managing all background services and their lifecycle
 */
@Singleton
class BackgroundServiceCoordinator @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val smsServiceManager: SmsServiceManager
) {
    
    private val coordinatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var healthMonitoringJob: kotlinx.coroutines.Job? = null
    private val lastHealthCheck = java.util.concurrent.atomic.AtomicLong(0)
    
    init {
        setupPermissionMonitoring()
        setupServiceStateMonitoring()
        startHealthMonitoring()
    }
    
    /**
     * Initialize all background services based on current permissions and settings
     */
    fun initializeBackgroundServices() {
        coordinatorScope.launch {
            try {
                // Start SMS monitoring if permissions are available
                if (permissionManager.hasSmsPermissions()) {
                    startSmsMonitoring()
                }
                
                // Initialize other background services here as needed
                
            } catch (e: Exception) {
                handleCoordinatorError("Failed to initialize background services", e)
            }
        }
    }
    
    /**
     * Start SMS monitoring service
     */
    fun startSmsMonitoring() {
        try {
            smsServiceManager.startSmsMonitoring()
        } catch (e: Exception) {
            handleCoordinatorError("Failed to start SMS monitoring", e)
        }
    }
    
    /**
     * Stop SMS monitoring service
     */
    fun stopSmsMonitoring() {
        try {
            smsServiceManager.stopSmsMonitoring()
        } catch (e: Exception) {
            handleCoordinatorError("Failed to stop SMS monitoring", e)
        }
    }
    
    /**
     * Restart all background services (useful for error recovery)
     */
    fun restartBackgroundServices() {
        coordinatorScope.launch {
            try {
                // Stop all services first
                stopAllServices()
                
                // Wait a moment before restarting
                kotlinx.coroutines.delay(2000)
                
                // Restart services based on current permissions
                initializeBackgroundServices()
                
            } catch (e: Exception) {
                handleCoordinatorError("Failed to restart background services", e)
            }
        }
    }
    
    /**
     * Stop all background services
     */
    fun stopAllServices() {
        try {
            smsServiceManager.stopSmsMonitoring()
            // Stop other services here as needed
            
        } catch (e: Exception) {
            handleCoordinatorError("Failed to stop all services", e)
        }
    }
    
    /**
     * Check if critical background services are running and healthy
     */
    fun areServicesHealthy(): Boolean {
        return try {
            // Check if SMS service should be running and is running
            val shouldRunSms = smsServiceManager.shouldMonitorSms()
            val isSmsRunning = smsServiceManager.isServiceRunning()
            
            // Additional health checks
            val hasPermissions = permissionManager.hasSmsPermissions()
            val batteryOptimized = smsServiceManager.isBatteryOptimizationEnabled()
            
            // Service is healthy if:
            // 1. It's running when it should be, or not running when it shouldn't be
            // 2. Permissions are properly aligned with service state
            // 3. Battery optimization isn't causing issues
            val basicHealth = (shouldRunSms && isSmsRunning) || (!shouldRunSms && !isSmsRunning)
            val permissionHealth = hasPermissions == shouldRunSms
            val batteryHealth = !batteryOptimized || !shouldRunSms
            
            basicHealth && permissionHealth && batteryHealth
            
        } catch (e: Exception) {
            handleCoordinatorError("Health check failed", e)
            false
        }
    }
    
    /**
     * Get comprehensive status of all background services
     */
    fun getServiceStatus(): BackgroundServiceStatus {
        return try {
            BackgroundServiceStatus(
                smsMonitoringEnabled = permissionManager.hasSmsPermissions(),
                smsServiceRunning = smsServiceManager.isServiceRunning(),
                batteryOptimizationEnabled = smsServiceManager.isBatteryOptimizationEnabled(),
                overallHealth = areServicesHealthy(),
                lastHealthCheck = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            BackgroundServiceStatus(
                smsMonitoringEnabled = false,
                smsServiceRunning = false,
                batteryOptimizationEnabled = true,
                overallHealth = false,
                lastHealthCheck = System.currentTimeMillis(),
                error = e.message
            )
        }
    }
    
    private fun setupPermissionMonitoring() {
        permissionManager.smsPermissionState
            .onEach { permissionStatus ->
                handlePermissionChange(permissionStatus)
            }
            .launchIn(coordinatorScope)
    }
    
    private fun setupServiceStateMonitoring() {
        smsServiceManager.serviceState
            .onEach { serviceState ->
                handleServiceStateChange(serviceState)
            }
            .launchIn(coordinatorScope)
    }
    
    private fun handlePermissionChange(permissionStatus: com.expensetracker.domain.permission.PermissionStatus) {
        coordinatorScope.launch {
            try {
                when (permissionStatus) {
                    com.expensetracker.domain.permission.PermissionStatus.GRANTED -> {
                        // Permission granted, start SMS monitoring
                        if (!smsServiceManager.isServiceRunning()) {
                            startSmsMonitoring()
                        }
                    }
                    com.expensetracker.domain.permission.PermissionStatus.DENIED,
                    com.expensetracker.domain.permission.PermissionStatus.PERMANENTLY_DENIED -> {
                        // Permission denied, stop SMS monitoring
                        if (smsServiceManager.isServiceRunning()) {
                            stopSmsMonitoring()
                        }
                    }
                }
            } catch (e: Exception) {
                handleCoordinatorError("Failed to handle permission change", e)
            }
        }
    }
    
    private fun handleServiceStateChange(serviceState: SmsServiceManager.ServiceState) {
        // React to service state changes if needed
        when (serviceState) {
            SmsServiceManager.ServiceState.ERROR -> {
                // Service encountered an error, could implement recovery logic here
                coordinatorScope.launch {
                    kotlinx.coroutines.delay(5000) // Wait 5 seconds
                    if (permissionManager.hasSmsPermissions()) {
                        smsServiceManager.restartSmsMonitoring()
                    }
                }
            }
            SmsServiceManager.ServiceState.RUNNING -> {
                // Service is running successfully
            }
            else -> {
                // Handle other states as needed
            }
        }
    }
    
    /**
     * Start continuous health monitoring of background services
     */
    private fun startHealthMonitoring() {
        healthMonitoringJob = coordinatorScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(HEALTH_CHECK_INTERVAL)
                    performHealthCheck()
                } catch (e: Exception) {
                    handleCoordinatorError("Health monitoring error", e)
                }
            }
        }
    }
    
    /**
     * Perform comprehensive health check and recovery if needed
     */
    private suspend fun performHealthCheck() {
        lastHealthCheck.set(System.currentTimeMillis())
        
        try {
            val isHealthy = areServicesHealthy()
            
            if (!isHealthy) {
                handleUnhealthyServices()
            }
            
            // Check for zombie services (services that should be stopped but are still running)
            if (!smsServiceManager.shouldMonitorSms() && smsServiceManager.isServiceRunning()) {
                stopSmsMonitoring()
            }
            
            // Check for missing services (services that should be running but aren't)
            if (smsServiceManager.shouldMonitorSms() && !smsServiceManager.isServiceRunning()) {
                startSmsMonitoring()
            }
            
        } catch (e: Exception) {
            handleCoordinatorError("Health check execution failed", e)
        }
    }
    
    /**
     * Handle unhealthy service states with recovery attempts
     */
    private suspend fun handleUnhealthyServices() {
        try {
            // Attempt to recover SMS monitoring if it should be running
            if (permissionManager.hasSmsPermissions() && !smsServiceManager.isServiceRunning()) {
                startSmsMonitoring()
                kotlinx.coroutines.delay(2000) // Wait for service to start
                
                // Verify recovery
                if (!smsServiceManager.isServiceRunning()) {
                    // If still not running, try a full restart
                    restartBackgroundServices()
                }
            }
            
        } catch (e: Exception) {
            handleCoordinatorError("Service recovery failed", e)
        }
    }
    
    /**
     * Stop health monitoring (called during cleanup)
     */
    fun stopHealthMonitoring() {
        healthMonitoringJob?.cancel()
        healthMonitoringJob = null
    }
    
    private fun handleCoordinatorError(message: String, exception: Exception) {
        // Log error for debugging
        exception.printStackTrace()
        
        // Could implement error reporting or recovery logic here
        // For now, ensure the coordinator continues to function
    }
    
    companion object {
        private const val HEALTH_CHECK_INTERVAL = 30_000L // 30 seconds
    }
    
    /**
     * Data class representing the status of all background services
     */
    data class BackgroundServiceStatus(
        val smsMonitoringEnabled: Boolean,
        val smsServiceRunning: Boolean,
        val batteryOptimizationEnabled: Boolean,
        val overallHealth: Boolean,
        val lastHealthCheck: Long,
        val error: String? = null
    )
}