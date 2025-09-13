package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving application settings
 */
class GetAppSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Observes app settings changes
     */
    fun observeAppSettings(): Flow<AppSettings> {
        return settingsRepository.observeAppSettings()
    }
    
    /**
     * Gets current app settings
     */
    suspend fun getAppSettings(): AppSettings {
        return settingsRepository.getAppSettings()
    }
}