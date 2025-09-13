package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for updating application settings
 */
class UpdateAppSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Updates app settings
     */
    suspend fun updateAppSettings(settings: AppSettings) {
        val validatedSettings = validateSettings(settings)
        settingsRepository.updateAppSettings(validatedSettings)
    }
    
    /**
     * Updates theme mode
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        val currentSettings = settingsRepository.getAppSettings()
        val updatedSettings = currentSettings.copy(themeMode = themeMode)
        settingsRepository.updateAppSettings(updatedSettings)
    }
    
    /**
     * Updates SMS permission setting
     */
    suspend fun updateSmsPermission(enabled: Boolean) {
        val currentSettings = settingsRepository.getAppSettings()
        val updatedSettings = currentSettings.copy(smsPermissionEnabled = enabled)
        settingsRepository.updateAppSettings(updatedSettings)
    }
    
    /**
     * Updates biometric authentication setting
     */
    suspend fun updateBiometricAuth(enabled: Boolean) {
        val currentSettings = settingsRepository.getAppSettings()
        val updatedSettings = currentSettings.copy(biometricAuthEnabled = enabled)
        settingsRepository.updateAppSettings(updatedSettings)
    }
    
    /**
     * Updates auto-lock timeout
     */
    suspend fun updateAutoLockTimeout(timeoutMinutes: Int) {
        val currentSettings = settingsRepository.getAppSettings()
        val validTimeout = timeoutMinutes.coerceIn(1, 60) // 1-60 minutes
        val updatedSettings = currentSettings.copy(autoLockTimeoutMinutes = validTimeout)
        settingsRepository.updateAppSettings(updatedSettings)
    }
    
    /**
     * Validates settings before saving
     */
    private fun validateSettings(settings: AppSettings): AppSettings {
        return settings.copy(
            autoLockTimeoutMinutes = settings.autoLockTimeoutMinutes.coerceIn(1, 60),
            dataRetentionMonths = settings.dataRetentionMonths.coerceIn(1, 120),
            currencyCode = if (settings.currencyCode.isBlank()) "INR" else settings.currencyCode,
            dateFormat = if (settings.dateFormat.isBlank()) "dd/MM/yyyy" else settings.dateFormat
        )
    }
}