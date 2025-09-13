package com.expensetracker.data.repository


import com.expensetracker.data.local.dao.SettingsDao
import com.expensetracker.data.mapper.SettingsMapper.getDefaultNotificationPreferences
import com.expensetracker.data.mapper.SettingsMapper.getDefaultAppSettings
import com.expensetracker.data.mapper.SettingsMapper.getDefaultDataManagementSettings
import com.expensetracker.data.mapper.SettingsMapper.getDefaultPrivacySettings
import com.expensetracker.data.mapper.SettingsMapper.toDomain
import com.expensetracker.data.mapper.SettingsMapper.toEntity
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.DataManagementSettings
import com.expensetracker.domain.model.NotificationPreferences
import com.expensetracker.domain.model.PrivacySettings
import com.expensetracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override fun observeAppSettings(): Flow<AppSettings> {
        return settingsDao.observeAppSettings().map { entity ->
            entity?.toDomain() ?: getDefaultAppSettings()
        }
    }
    
    override suspend fun getAppSettings(): AppSettings {
        return settingsDao.getAppSettings()?.toDomain() ?: getDefaultAppSettings()
    }
    
    override suspend fun updateAppSettings(settings: AppSettings) {
        val entity = settings.toEntity()
        val existing = settingsDao.getAppSettings()
        
        if (existing != null) {
            settingsDao.updateAppSettings(entity)
        } else {
            settingsDao.insertAppSettings(entity)
        }
    }
    
    override fun observeNotificationPreferences(): Flow<NotificationPreferences> {
        return settingsDao.observeNotificationPreferences().map { entity ->
            entity?.toDomain() ?: NotificationPreferences()
        }
    }
    
    override suspend fun getNotificationPreferences(): NotificationPreferences {
        return settingsDao.getNotificationPreferences()?.toDomain() 
            ?: NotificationPreferences()
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        val entity = preferences.toEntity()
        val existing = settingsDao.getNotificationPreferences()
        
        if (existing != null) {
            settingsDao.updateNotificationPreferences(entity)
        } else {
            settingsDao.insertNotificationPreferences(entity)
        }
    }
    
    override fun observeDataManagementSettings(): Flow<DataManagementSettings> {
        return settingsDao.observeDataManagementSettings().map { entity ->
            entity?.toDomain() ?: getDefaultDataManagementSettings()
        }
    }
    
    override suspend fun getDataManagementSettings(): DataManagementSettings {
        return settingsDao.getDataManagementSettings()?.toDomain() 
            ?: getDefaultDataManagementSettings()
    }
    
    override suspend fun updateDataManagementSettings(settings: DataManagementSettings) {
        val entity = settings.toEntity()
        val existing = settingsDao.getDataManagementSettings()
        
        if (existing != null) {
            settingsDao.updateDataManagementSettings(entity)
        } else {
            settingsDao.insertDataManagementSettings(entity)
        }
    }
    
    override fun observePrivacySettings(): Flow<PrivacySettings> {
        return settingsDao.observePrivacySettings().map { entity ->
            entity?.toDomain() ?: getDefaultPrivacySettings()
        }
    }
    
    override suspend fun getPrivacySettings(): PrivacySettings {
        return settingsDao.getPrivacySettings()?.toDomain() ?: getDefaultPrivacySettings()
    }
    
    override suspend fun updatePrivacySettings(settings: PrivacySettings) {
        val entity = settings.toEntity()
        val existing = settingsDao.getPrivacySettings()
        
        if (existing != null) {
            settingsDao.updatePrivacySettings(entity)
        } else {
            settingsDao.insertPrivacySettings(entity)
        }
    }
    
    override suspend fun resetToDefaults() {
        settingsDao.resetAllSettings()
        // Insert default values
        settingsDao.insertAppSettings(getDefaultAppSettings().toEntity())
        settingsDao.insertDataManagementSettings(getDefaultDataManagementSettings().toEntity())
        settingsDao.insertPrivacySettings(getDefaultPrivacySettings().toEntity())
        settingsDao.insertNotificationPreferences(getDefaultNotificationPreferences().toEntity())
    }
    
    override suspend fun validateSettings(): Boolean {
        return try {
            // Try to read all settings
            getAppSettings()
            getNotificationPreferences()
            getDataManagementSettings()
            getPrivacySettings()
            true
        } catch (e: Exception) {
            false
        }
    }
}