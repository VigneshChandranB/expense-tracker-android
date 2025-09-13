package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.local.entity.DataManagementSettingsEntity
import com.expensetracker.data.local.entity.PrivacySettingsEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.data.local.entity.AccountNotificationPreferencesEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing application settings
 */
@Dao
interface SettingsDao {
    
    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettings(): AppSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettingsEntity)
    
    @Update
    suspend fun updateAppSettings(settings: AppSettingsEntity)
    
    @Query("DELETE FROM app_settings")
    suspend fun deleteAppSettings()
    
    // Data Management Settings
    @Query("SELECT * FROM data_management_settings WHERE id = 1 LIMIT 1")
    fun observeDataManagementSettings(): Flow<DataManagementSettingsEntity?>
    
    @Query("SELECT * FROM data_management_settings WHERE id = 1 LIMIT 1")
    suspend fun getDataManagementSettings(): DataManagementSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataManagementSettings(settings: DataManagementSettingsEntity)
    
    @Update
    suspend fun updateDataManagementSettings(settings: DataManagementSettingsEntity)
    
    @Query("DELETE FROM data_management_settings")
    suspend fun deleteDataManagementSettings()
    
    // Privacy Settings
    @Query("SELECT * FROM privacy_settings WHERE id = 1 LIMIT 1")
    fun observePrivacySettings(): Flow<PrivacySettingsEntity?>
    
    @Query("SELECT * FROM privacy_settings WHERE id = 1 LIMIT 1")
    suspend fun getPrivacySettings(): PrivacySettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivacySettings(settings: PrivacySettingsEntity)
    
    @Update
    suspend fun updatePrivacySettings(settings: PrivacySettingsEntity)
    
    @Query("DELETE FROM privacy_settings")
    suspend fun deletePrivacySettings()
    
    // Notification Preferences
    @Query("SELECT * FROM notification_preferences WHERE id = 1 LIMIT 1")
    fun observeNotificationPreferences(): Flow<NotificationPreferencesEntity?>
    
    @Query("SELECT * FROM notification_preferences WHERE id = 1 LIMIT 1")
    suspend fun getNotificationPreferences(): NotificationPreferencesEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationPreferences(preferences: NotificationPreferencesEntity)
    
    @Update
    suspend fun updateNotificationPreferences(preferences: NotificationPreferencesEntity)
    
    @Query("DELETE FROM notification_preferences")
    suspend fun deleteNotificationPreferences()
    
    // Account Notification Preferences
    @Query("SELECT * FROM account_notification_preferences WHERE accountId = :accountId")
    fun observeAccountNotificationPreferences(accountId: Long): Flow<List<AccountNotificationPreferencesEntity>>
    
    @Query("SELECT * FROM account_notification_preferences WHERE accountId = :accountId")
    suspend fun getAccountNotificationPreferences(accountId: Long): List<AccountNotificationPreferencesEntity>
    
    @Query("SELECT * FROM account_notification_preferences")
    suspend fun getAllAccountNotificationPreferences(): List<AccountNotificationPreferencesEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountNotificationPreferences(preferences: AccountNotificationPreferencesEntity)
    
    @Update
    suspend fun updateAccountNotificationPreferences(preferences: AccountNotificationPreferencesEntity)
    
    @Query("DELETE FROM account_notification_preferences WHERE accountId = :accountId")
    suspend fun deleteAccountNotificationPreferences(accountId: Long)
    
    @Query("DELETE FROM account_notification_preferences")
    suspend fun deleteAllAccountNotificationPreferences()
    
    // Utility methods
    @Transaction
    suspend fun resetAllSettings() {
        deleteAppSettings()
        deleteDataManagementSettings()
        deletePrivacySettings()
        deleteNotificationPreferences()
        deleteAllAccountNotificationPreferences()
    }
}