package com.expensetracker.domain.permission

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles graceful degradation when SMS permissions are not available
 */
@Singleton
class GracefulDegradationHandler @Inject constructor(
    private val permissionManager: PermissionManager
) {
    
    /**
     * Get the current app mode based on permission status
     */
    fun getAppMode(): Flow<AppMode> {
        return permissionManager.smsPermissionState.map { permissionStatus ->
            when (permissionStatus) {
                PermissionStatus.GRANTED -> AppMode.AUTOMATIC_WITH_SMS
                PermissionStatus.DENIED,
                PermissionStatus.PERMANENTLY_DENIED -> AppMode.MANUAL_ONLY
            }
        }
    }
    
    /**
     * Check if automatic transaction detection is available
     */
    fun isAutomaticDetectionAvailable(): Boolean {
        return permissionManager.hasSmsPermissions()
    }
    
    /**
     * Get user-friendly message about current app capabilities
     */
    fun getCapabilityMessage(): String {
        return if (isAutomaticDetectionAvailable()) {
            "Automatic transaction detection is active. Your expenses will be tracked automatically from SMS messages."
        } else {
            "Manual mode is active. You can add transactions manually or grant SMS permission for automatic detection."
        }
    }
    
    /**
     * Get features that are available in current mode
     */
    fun getAvailableFeatures(): List<AppFeature> {
        val baseFeatures = listOf(
            AppFeature.MANUAL_TRANSACTION_ENTRY,
            AppFeature.TRANSACTION_CATEGORIZATION,
            AppFeature.SPENDING_ANALYTICS,
            AppFeature.DATA_EXPORT,
            AppFeature.NOTIFICATIONS
        )
        
        return if (isAutomaticDetectionAvailable()) {
            baseFeatures + AppFeature.AUTOMATIC_SMS_DETECTION
        } else {
            baseFeatures
        }
    }
    
    /**
     * Get features that are disabled in current mode
     */
    fun getDisabledFeatures(): List<AppFeature> {
        return if (isAutomaticDetectionAvailable()) {
            emptyList()
        } else {
            listOf(AppFeature.AUTOMATIC_SMS_DETECTION)
        }
    }
}

/**
 * App operating modes based on permission availability
 */
enum class AppMode {
    AUTOMATIC_WITH_SMS,
    MANUAL_ONLY
}

/**
 * Available app features
 */
enum class AppFeature {
    AUTOMATIC_SMS_DETECTION,
    MANUAL_TRANSACTION_ENTRY,
    TRANSACTION_CATEGORIZATION,
    SPENDING_ANALYTICS,
    DATA_EXPORT,
    NOTIFICATIONS
}