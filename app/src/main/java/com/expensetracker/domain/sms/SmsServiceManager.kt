package com.expensetracker.domain.sms

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.expensetracker.data.sms.SmsMonitoringService
import com.expensetracker.domain.permission.PermissionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced manager for SMS monitoring service lifecycle with battery optimization and error recovery
 */
@Singleton
class SmsServiceManager @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    
    private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
    val serviceState: Flow<ServiceState> = _serviceState.asStateFlow()
    
    /**
     * Start SMS monitoring service with battery optimization handling
     */
    fun startSmsMonitoring() {
        if (!permissionManager.hasSmsPermissions()) {
            _serviceState.value = ServiceState.PERMISSION_REQUIRED
            return
        }
        
        try {
            val intent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_START_MONITORING
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            _serviceState.value = ServiceState.STARTING
            
        } catch (e: Exception) {
            _serviceState.value = ServiceState.ERROR
            handleServiceError("Failed to start SMS monitoring", e)
        }
    }
    
    /**
     * Stop SMS monitoring service
     */
    fun stopSmsMonitoring() {
        try {
            val intent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_STOP_MONITORING
            }
            context.startService(intent)
            _serviceState.value = ServiceState.STOPPING
            
        } catch (e: Exception) {
            handleServiceError("Failed to stop SMS monitoring", e)
        }
    }
    
    /**
     * Restart SMS monitoring service for error recovery
     */
    fun restartSmsMonitoring() {
        try {
            val intent = Intent(context, SmsMonitoringService::class.java).apply {
                action = SmsMonitoringService.ACTION_RESTART_MONITORING
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            _serviceState.value = ServiceState.RESTARTING
            
        } catch (e: Exception) {
            _serviceState.value = ServiceState.ERROR
            handleServiceError("Failed to restart SMS monitoring", e)
        }
    }
    
    /**
     * Check if SMS monitoring should be active
     */
    fun shouldMonitorSms(): Boolean {
        return permissionManager.hasSmsPermissions() && !isBatteryOptimizationEnabled()
    }
    
    /**
     * Check if the SMS monitoring service is currently running
     */
    fun isServiceRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == SmsMonitoringService::class.java.name }
    }
    
    /**
     * Check if battery optimization is enabled for the app
     */
    fun isBatteryOptimizationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }
    }
    
    /**
     * Get battery optimization status and recommendations
     */
    fun getBatteryOptimizationInfo(): BatteryOptimizationInfo {
        val isOptimized = isBatteryOptimizationEnabled()
        
        return BatteryOptimizationInfo(
            isOptimizationEnabled = isOptimized,
            recommendation = if (isOptimized) {
                "Disable battery optimization for reliable SMS monitoring"
            } else {
                "Battery optimization is disabled - SMS monitoring will work reliably"
            },
            impactLevel = if (isOptimized) ImpactLevel.HIGH else ImpactLevel.NONE
        )
    }
    
    /**
     * Update service state (called by the service itself)
     */
    fun updateServiceState(state: ServiceState) {
        _serviceState.value = state
    }
    
    private fun handleServiceError(message: String, exception: Exception) {
        // Log error for debugging
        exception.printStackTrace()
        
        // Update state to error
        _serviceState.value = ServiceState.ERROR
        
        // Could implement additional error reporting here
    }
    
    /**
     * Service state enumeration
     */
    enum class ServiceState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        RESTARTING,
        ERROR,
        PERMISSION_REQUIRED
    }
    
    /**
     * Battery optimization information
     */
    data class BatteryOptimizationInfo(
        val isOptimizationEnabled: Boolean,
        val recommendation: String,
        val impactLevel: ImpactLevel
    )
    
    /**
     * Impact level for battery optimization
     */
    enum class ImpactLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }
}