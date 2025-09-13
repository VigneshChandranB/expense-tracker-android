package com.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AccountNotificationSettings
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for notification settings screen
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val accountRepository: AccountRepository,
    private val notificationService: NotificationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        checkNotificationPermission()
    }
    
    fun onEvent(event: NotificationSettingsEvent) {
        when (event) {
            is NotificationSettingsEvent.LoadSettings -> loadSettings()
            is NotificationSettingsEvent.RequestNotificationPermission -> requestNotificationPermission()
            is NotificationSettingsEvent.DismissPermissionDialog -> dismissPermissionDialog()
            is NotificationSettingsEvent.ClearError -> clearError()
            is NotificationSettingsEvent.ClearSuccessMessage -> clearSuccessMessage()
            
            // General preferences
            is NotificationSettingsEvent.UpdateBillRemindersEnabled -> updateBillRemindersEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateBillReminderDays -> updateBillReminderDays(event.days)
            is NotificationSettingsEvent.UpdateSpendingLimitAlertsEnabled -> updateSpendingLimitAlertsEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateLowBalanceWarningsEnabled -> updateLowBalanceWarningsEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateLowBalanceThreshold -> updateLowBalanceThreshold(event.threshold)
            is NotificationSettingsEvent.UpdateUnusualSpendingAlertsEnabled -> updateUnusualSpendingAlertsEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateBudgetExceededAlertsEnabled -> updateBudgetExceededAlertsEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateLargeTransactionAlertsEnabled -> updateLargeTransactionAlertsEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateLargeTransactionThreshold -> updateLargeTransactionThreshold(event.threshold)
            is NotificationSettingsEvent.UpdateQuietHoursEnabled -> updateQuietHoursEnabled(event.enabled)
            is NotificationSettingsEvent.UpdateQuietHoursStart -> updateQuietHoursStart(event.hour)
            is NotificationSettingsEvent.UpdateQuietHoursEnd -> updateQuietHoursEnd(event.hour)
            
            // Account-specific settings
            is NotificationSettingsEvent.UpdateAccountSpendingLimitEnabled -> updateAccountSpendingLimitEnabled(event.accountId, event.enabled)
            is NotificationSettingsEvent.UpdateAccountSpendingLimit -> updateAccountSpendingLimit(event.accountId, event.limit)
            is NotificationSettingsEvent.UpdateAccountLowBalanceEnabled -> updateAccountLowBalanceEnabled(event.accountId, event.enabled)
            is NotificationSettingsEvent.UpdateAccountLowBalanceThreshold -> updateAccountLowBalanceThreshold(event.accountId, event.threshold)
            is NotificationSettingsEvent.UpdateAccountUnusualSpendingEnabled -> updateAccountUnusualSpendingEnabled(event.accountId, event.enabled)
            
            is NotificationSettingsEvent.SaveSettings -> saveSettings()
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val preferences = notificationRepository.getNotificationPreferences()
                val accounts = accountRepository.getAllAccounts().first()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        preferences = preferences,
                        accounts = accounts,
                        accountSettings = preferences.accountSpecificSettings
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load notification settings: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun checkNotificationPermission() {
        viewModelScope.launch {
            val isGranted = notificationService.areNotificationsEnabled()
            _uiState.update { it.copy(isNotificationPermissionGranted = isGranted) }
        }
    }
    
    private fun requestNotificationPermission() {
        viewModelScope.launch {
            val granted = notificationService.requestNotificationPermission()
            _uiState.update {
                it.copy(
                    isNotificationPermissionGranted = granted,
                    showPermissionDialog = !granted
                )
            }
        }
    }
    
    private fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }
    
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    // General preference updates
    private fun updateBillRemindersEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(billRemindersEnabled = enabled))
        }
    }
    
    private fun updateBillReminderDays(days: Int) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(billReminderDaysBefore = days))
        }
    }
    
    private fun updateSpendingLimitAlertsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(spendingLimitAlertsEnabled = enabled))
        }
    }
    
    private fun updateLowBalanceWarningsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(lowBalanceWarningsEnabled = enabled))
        }
    }
    
    private fun updateLowBalanceThreshold(threshold: java.math.BigDecimal) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(lowBalanceThreshold = threshold))
        }
    }
    
    private fun updateUnusualSpendingAlertsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(unusualSpendingAlertsEnabled = enabled))
        }
    }
    
    private fun updateBudgetExceededAlertsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(budgetExceededAlertsEnabled = enabled))
        }
    }
    
    private fun updateLargeTransactionAlertsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(largeTransactionAlertsEnabled = enabled))
        }
    }
    
    private fun updateLargeTransactionThreshold(threshold: java.math.BigDecimal) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(largeTransactionThreshold = threshold))
        }
    }
    
    private fun updateQuietHoursEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(quietHoursEnabled = enabled))
        }
    }
    
    private fun updateQuietHoursStart(hour: Int) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(quietHoursStart = hour))
        }
    }
    
    private fun updateQuietHoursEnd(hour: Int) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(quietHoursEnd = hour))
        }
    }
    
    // Account-specific setting updates
    private fun updateAccountSpendingLimitEnabled(accountId: Long, enabled: Boolean) {
        val currentSettings = _uiState.value.accountSettings[accountId] ?: AccountNotificationSettings(accountId)
        val updatedSettings = currentSettings.copy(spendingLimitEnabled = enabled)
        
        _uiState.update {
            it.copy(
                accountSettings = it.accountSettings + (accountId to updatedSettings),
                preferences = it.preferences.copy(
                    accountSpecificSettings = it.preferences.accountSpecificSettings + (accountId to updatedSettings)
                )
            )
        }
    }
    
    private fun updateAccountSpendingLimit(accountId: Long, limit: java.math.BigDecimal?) {
        val currentSettings = _uiState.value.accountSettings[accountId] ?: AccountNotificationSettings(accountId)
        val updatedSettings = currentSettings.copy(spendingLimit = limit)
        
        _uiState.update {
            it.copy(
                accountSettings = it.accountSettings + (accountId to updatedSettings),
                preferences = it.preferences.copy(
                    accountSpecificSettings = it.preferences.accountSpecificSettings + (accountId to updatedSettings)
                )
            )
        }
    }
    
    private fun updateAccountLowBalanceEnabled(accountId: Long, enabled: Boolean) {
        val currentSettings = _uiState.value.accountSettings[accountId] ?: AccountNotificationSettings(accountId)
        val updatedSettings = currentSettings.copy(lowBalanceEnabled = enabled)
        
        _uiState.update {
            it.copy(
                accountSettings = it.accountSettings + (accountId to updatedSettings),
                preferences = it.preferences.copy(
                    accountSpecificSettings = it.preferences.accountSpecificSettings + (accountId to updatedSettings)
                )
            )
        }
    }
    
    private fun updateAccountLowBalanceThreshold(accountId: Long, threshold: java.math.BigDecimal?) {
        val currentSettings = _uiState.value.accountSettings[accountId] ?: AccountNotificationSettings(accountId)
        val updatedSettings = currentSettings.copy(lowBalanceThreshold = threshold)
        
        _uiState.update {
            it.copy(
                accountSettings = it.accountSettings + (accountId to updatedSettings),
                preferences = it.preferences.copy(
                    accountSpecificSettings = it.preferences.accountSpecificSettings + (accountId to updatedSettings)
                )
            )
        }
    }
    
    private fun updateAccountUnusualSpendingEnabled(accountId: Long, enabled: Boolean) {
        val currentSettings = _uiState.value.accountSettings[accountId] ?: AccountNotificationSettings(accountId)
        val updatedSettings = currentSettings.copy(unusualSpendingEnabled = enabled)
        
        _uiState.update {
            it.copy(
                accountSettings = it.accountSettings + (accountId to updatedSettings),
                preferences = it.preferences.copy(
                    accountSpecificSettings = it.preferences.accountSpecificSettings + (accountId to updatedSettings)
                )
            )
        }
    }
    
    private fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                notificationRepository.updateNotificationPreferences(_uiState.value.preferences)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Notification settings saved successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save settings: ${e.message}"
                    )
                }
            }
        }
    }
}