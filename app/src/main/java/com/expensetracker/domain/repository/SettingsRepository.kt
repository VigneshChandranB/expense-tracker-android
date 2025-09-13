package com.expensetracker.domain.repository

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.DataManagementSettings
import com.expensetracker.domain.model.NotificationPreferences
import com.expensetracker.domain.model.PrivacySettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing application settings and preferences
 */
interface SettingsRepository {
    
    /**
     * Observes app settings changes
     */
    fun observeAppSettings(): Flow<AppSettings>
    
    /**
     * Gets current app settings
     */
    suspend fun getAppSettings(): AppSettings
    
    /**
     * Updates app settings
     */
    suspend fun updateAppSettings(settings: AppSettings)
    
    /**
     * Observes notification preferences changes
     */
    fun observeNotificationPreferences(): Flow<NotificationPreferences>
    
    /**
     * Gets current notification preferences
     */
    suspend fun getNotificationPreferences(): NotificationPreferences
    
    /**
     * Updates notification preferences
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Observes data management settings changes
     */
    fun observeDataManagementSettings(): Flow<DataManagementSettings>
    
    /**
     * Gets current data management settings
     */
    suspend fun getDataManagementSettings(): DataManagementSettings
    
    /**
     * Updates data management settings
     */
    suspend fun updateDataManagementSettings(settings: DataManagementSettings)
    
    /**
     * Observes privacy settings changes
     */
    fun observePrivacySettings(): Flow<PrivacySettings>
    
    /**
     * Gets current privacy settings
     */
    suspend fun getPrivacySettings(): PrivacySettings
    
    /**
     * Updates privacy settings
     */
    suspend fun updatePrivacySettings(settings: PrivacySettings)
    
    /**
     * Resets all settings to default values
     */
    suspend fun resetToDefaults()
    
    /**
     * Validates settings integrity
     */
    suspend fun validateSettings(): Boolean
}