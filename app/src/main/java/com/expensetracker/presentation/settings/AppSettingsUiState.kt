package com.expensetracker.presentation.settings

import com.expensetracker.domain.model.AppSettings

/**
 * UI state for app settings screen
 */
data class AppSettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val error: String? = null,
    val successMessage: String? = null
)