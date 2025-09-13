package com.expensetracker.domain.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages runtime permissions for the expense tracker app
 */
@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {
    
    private val _smsPermissionState = MutableStateFlow(getSmsPermissionStatus())
    val smsPermissionState: StateFlow<PermissionStatus> = _smsPermissionState.asStateFlow()
    
    /**
     * Check if SMS permissions are granted
     */
    fun hasSmsPermissions(): Boolean {
        val readSms = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        val receiveSms = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        return readSms && receiveSms
    }
    
    /**
     * Get required SMS permissions
     */
    fun getRequiredSmsPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }
    
    /**
     * Update SMS permission status
     */
    fun updateSmsPermissionStatus() {
        _smsPermissionState.value = getSmsPermissionStatus()
    }
    
    /**
     * Get current SMS permission status
     */
    private fun getSmsPermissionStatus(): PermissionStatus {
        return if (hasSmsPermissions()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }
    
    /**
     * Check if we should show rationale for SMS permissions
     */
    fun shouldShowSmsPermissionRationale(activity: android.app.Activity): Boolean {
        return getRequiredSmsPermissions().any { permission ->
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity, 
                permission
            )
        }
    }
}

/**
 * Permission status enum
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}