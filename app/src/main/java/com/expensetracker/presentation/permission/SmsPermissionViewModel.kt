package com.expensetracker.presentation.permission

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.permission.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SMS permission screen
 */
@HiltViewModel
class SmsPermissionViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    private val _showRationale = MutableStateFlow(false)
    
    val uiState: StateFlow<SmsPermissionUiState> = combine(
        permissionManager.smsPermissionState,
        _isLoading,
        _showRationale
    ) { permissionStatus, isLoading, showRationale ->
        SmsPermissionUiState(
            permissionStatus = permissionStatus,
            isLoading = isLoading,
            showRationale = showRationale
        )
    }.asStateFlow()
    
    /**
     * Request SMS permissions using the provided launcher
     */
    fun requestSmsPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        viewModelScope.launch {
            _isLoading.value = true
            _showRationale.value = false
            
            val permissions = permissionManager.getRequiredSmsPermissions()
            launcher.launch(permissions)
        }
    }
    
    /**
     * Handle the result of permission request
     */
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        viewModelScope.launch {
            _isLoading.value = false
            
            val allGranted = permissions.values.all { it }
            
            if (allGranted) {
                permissionManager.updateSmsPermissionStatus()
            } else {
                _showRationale.value = true
            }
        }
    }
    
    /**
     * Check if rationale should be shown
     */
    fun checkShouldShowRationale(activity: android.app.Activity) {
        _showRationale.value = permissionManager.shouldShowSmsPermissionRationale(activity)
    }
}

/**
 * UI state for SMS permission screen
 */
data class SmsPermissionUiState(
    val permissionStatus: PermissionStatus = PermissionStatus.DENIED,
    val isLoading: Boolean = false,
    val showRationale: Boolean = false
)