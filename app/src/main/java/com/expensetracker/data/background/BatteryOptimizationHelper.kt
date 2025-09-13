package com.expensetracker.data.background

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing battery optimization settings and providing user guidance
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    private val context: Context
) {
    
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
     * Get battery optimization status with detailed information
     */
    fun getBatteryOptimizationStatus(): BatteryOptimizationStatus {
        val isOptimized = isBatteryOptimizationEnabled()
        
        return BatteryOptimizationStatus(
            isOptimizationEnabled = isOptimized,
            canRequestWhitelist = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            impactOnSmsMonitoring = if (isOptimized) {
                "Battery optimization may cause SMS monitoring to stop working reliably. " +
                "The system may kill the background service to save battery."
            } else {
                "SMS monitoring will work reliably in the background."
            },
            recommendation = if (isOptimized) {
                "Disable battery optimization for this app to ensure reliable SMS monitoring."
            } else {
                "Battery optimization is already disabled. SMS monitoring will work reliably."
            },
            severity = if (isOptimized) Severity.HIGH else Severity.NONE
        )
    }
    
    /**
     * Create intent to open battery optimization settings
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun createBatteryOptimizationIntent(): Intent? {
        return try {
            Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
        } catch (e: Exception) {
            // Fallback to general battery optimization settings
            createGeneralBatterySettingsIntent()
        }
    }
    
    /**
     * Create intent to open general battery optimization settings
     */
    fun createGeneralBatterySettingsIntent(): Intent? {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
                else -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get device-specific battery optimization guidance
     */
    fun getDeviceSpecificGuidance(): DeviceGuidance {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") -> DeviceGuidance(
                manufacturer = "Xiaomi (MIUI)",
                specificSteps = listOf(
                    "Open Settings > Apps > Manage apps",
                    "Find and select Expense Tracker",
                    "Tap 'Battery saver' and select 'No restrictions'",
                    "Go back and tap 'Autostart' - enable it for this app",
                    "In Security app > Permissions > Autostart, enable Expense Tracker",
                    "Optional: In Security app > Battery > Power, add app to whitelist"
                ),
                additionalInfo = "Xiaomi devices (MIUI) have very aggressive battery optimization. " +
                        "Both 'Battery saver' and 'Autostart' settings must be configured. " +
                        "Some MIUI versions may also require Security app configuration."
            )
            
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> DeviceGuidance(
                manufacturer = "Huawei/Honor",
                specificSteps = listOf(
                    "Open Settings > Apps > Expense Tracker",
                    "Tap 'Battery' and select 'Don't optimize'",
                    "Go to 'Launch' and enable 'Manage manually'",
                    "Enable 'Auto-launch', 'Secondary launch', and 'Run in background'"
                ),
                additionalInfo = "Huawei devices require multiple settings to be changed for reliable background operation."
            )
            
            manufacturer.contains("oppo") -> DeviceGuidance(
                manufacturer = "Oppo",
                specificSteps = listOf(
                    "Open Settings > Battery > Battery Optimization",
                    "Find Expense Tracker and select 'Don't optimize'",
                    "Go to Settings > Apps > Expense Tracker > Battery",
                    "Enable 'Allow background activity'"
                ),
                additionalInfo = "Oppo devices may also have 'Sleep Standby Optimization' that needs to be disabled."
            )
            
            manufacturer.contains("vivo") -> DeviceGuidance(
                manufacturer = "Vivo",
                specificSteps = listOf(
                    "Open Settings > Battery > Background App Refresh",
                    "Find Expense Tracker and enable it",
                    "Go to Settings > Apps > Expense Tracker",
                    "Enable 'High background app refresh'"
                ),
                additionalInfo = "Vivo devices have multiple battery management features that may affect background apps."
            )
            
            manufacturer.contains("oneplus") -> DeviceGuidance(
                manufacturer = "OnePlus",
                specificSteps = listOf(
                    "Open Settings > Battery > Battery Optimization",
                    "Select 'All apps' and find Expense Tracker",
                    "Select 'Don't optimize'",
                    "Also check Settings > Apps > Expense Tracker > Battery > Battery Optimization"
                ),
                additionalInfo = "OnePlus devices may have additional battery optimization in app-specific settings."
            )
            
            else -> DeviceGuidance(
                manufacturer = "Generic Android",
                specificSteps = listOf(
                    "Open Settings > Apps > Expense Tracker",
                    "Tap 'Battery' or 'Battery Usage'",
                    "Select 'Don't optimize' or 'Allow background activity'",
                    "If available, disable 'Adaptive Battery' for this app"
                ),
                additionalInfo = "Steps may vary depending on your device manufacturer and Android version."
            )
        }
    }
    
    /**
     * Check if the device is known to have aggressive battery optimization
     */
    fun hasAggressiveBatteryOptimization(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("oneplus")
    }
    
    /**
     * Get battery optimization impact assessment
     */
    fun getBatteryOptimizationImpact(): BatteryOptimizationImpact {
        val isOptimized = isBatteryOptimizationEnabled()
        val hasAggressive = hasAggressiveBatteryOptimization()
        
        return when {
            !isOptimized -> BatteryOptimizationImpact(
                level = ImpactLevel.NONE,
                description = "SMS monitoring will work reliably",
                recommendation = "No action needed"
            )
            
            isOptimized && hasAggressive -> BatteryOptimizationImpact(
                level = ImpactLevel.CRITICAL,
                description = "SMS monitoring will likely be killed by the system frequently",
                recommendation = "Strongly recommended to disable battery optimization and follow device-specific steps"
            )
            
            isOptimized -> BatteryOptimizationImpact(
                level = ImpactLevel.HIGH,
                description = "SMS monitoring may be interrupted occasionally",
                recommendation = "Recommended to disable battery optimization for reliable operation"
            )
            
            else -> BatteryOptimizationImpact(
                level = ImpactLevel.NONE,
                description = "SMS monitoring will work reliably",
                recommendation = "No action needed"
            )
        }
    }
    
    /**
     * Battery optimization status information
     */
    data class BatteryOptimizationStatus(
        val isOptimizationEnabled: Boolean,
        val canRequestWhitelist: Boolean,
        val impactOnSmsMonitoring: String,
        val recommendation: String,
        val severity: Severity
    )
    
    /**
     * Device-specific guidance for battery optimization
     */
    data class DeviceGuidance(
        val manufacturer: String,
        val specificSteps: List<String>,
        val additionalInfo: String
    )
    
    /**
     * Battery optimization impact assessment
     */
    data class BatteryOptimizationImpact(
        val level: ImpactLevel,
        val description: String,
        val recommendation: String
    )
    
    /**
     * Severity levels for battery optimization impact
     */
    enum class Severity {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Impact levels for battery optimization
     */
    enum class ImpactLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}