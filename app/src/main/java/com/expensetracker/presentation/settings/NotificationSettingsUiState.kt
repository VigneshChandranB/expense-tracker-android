package com.expensetracker.presentation.settings

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountNotificationSettings
import com.expensetracker.domain.model.NotificationPreferences
import java.math.BigDecimal

/**
 * UI state for notification settings screen
 */
data class NotificationSettingsUiState(
    val isLoading: Boolean = false,
    val preferences: NotificationPreferences = NotificationPreferences(),
    val accounts: List<Account> = emptyList(),
    val accountSettings: Map<Long, AccountNotificationSettings> = emptyMap(),
    val isNotificationPermissionGranted: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Events for notification settings screen
 */
sealed class NotificationSettingsEvent {
    object LoadSettings : NotificationSettingsEvent()
    object RequestNotificationPermission : NotificationSettingsEvent()
    object DismissPermissionDialog : NotificationSettingsEvent()
    object ClearError : NotificationSettingsEvent()
    object ClearSuccessMessage : NotificationSettingsEvent()
    
    // General preferences
    data class UpdateBillRemindersEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateBillReminderDays(val days: Int) : NotificationSettingsEvent()
    data class UpdateSpendingLimitAlertsEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateLowBalanceWarningsEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateLowBalanceThreshold(val threshold: BigDecimal) : NotificationSettingsEvent()
    data class UpdateUnusualSpendingAlertsEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateBudgetExceededAlertsEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateLargeTransactionAlertsEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateLargeTransactionThreshold(val threshold: BigDecimal) : NotificationSettingsEvent()
    data class UpdateQuietHoursEnabled(val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateQuietHoursStart(val hour: Int) : NotificationSettingsEvent()
    data class UpdateQuietHoursEnd(val hour: Int) : NotificationSettingsEvent()
    
    // Account-specific settings
    data class UpdateAccountSpendingLimitEnabled(val accountId: Long, val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateAccountSpendingLimit(val accountId: Long, val limit: BigDecimal?) : NotificationSettingsEvent()
    data class UpdateAccountLowBalanceEnabled(val accountId: Long, val enabled: Boolean) : NotificationSettingsEvent()
    data class UpdateAccountLowBalanceThreshold(val accountId: Long, val threshold: BigDecimal?) : NotificationSettingsEvent()
    data class UpdateAccountUnusualSpendingEnabled(val accountId: Long, val enabled: Boolean) : NotificationSettingsEvent()
    
    object SaveSettings : NotificationSettingsEvent()
}