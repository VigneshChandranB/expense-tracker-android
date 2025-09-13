package com.expensetracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.local.entity.DataManagementSettingsEntity
import com.expensetracker.data.local.entity.PrivacySettingsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumentation tests for SettingsDao
 */
@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var settingsDao: SettingsDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        settingsDao = database.settingsDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndGetAppSettings() = runTest {
        // Given
        val settings = AppSettingsEntity(
            id = 1L,
            themeMode = "DARK",
            smsPermissionEnabled = true,
            currencyCode = "USD"
        )
        
        // When
        settingsDao.insertAppSettings(settings)
        val retrieved = settingsDao.getAppSettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals("DARK", retrieved?.themeMode)
        assertEquals(true, retrieved?.smsPermissionEnabled)
        assertEquals("USD", retrieved?.currencyCode)
    }
    
    @Test
    fun observeAppSettings() = runTest {
        // Given
        val settings = AppSettingsEntity(
            id = 1L,
            themeMode = "LIGHT",
            currencyCode = "EUR"
        )
        
        // When
        settingsDao.insertAppSettings(settings)
        val observed = settingsDao.observeAppSettings().first()
        
        // Then
        assertNotNull(observed)
        assertEquals("LIGHT", observed?.themeMode)
        assertEquals("EUR", observed?.currencyCode)
    }
    
    @Test
    fun updateAppSettings() = runTest {
        // Given
        val initialSettings = AppSettingsEntity(
            id = 1L,
            themeMode = "SYSTEM",
            currencyCode = "INR"
        )
        settingsDao.insertAppSettings(initialSettings)
        
        // When
        val updatedSettings = initialSettings.copy(
            themeMode = "DARK",
            currencyCode = "USD"
        )
        settingsDao.updateAppSettings(updatedSettings)
        val retrieved = settingsDao.getAppSettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals("DARK", retrieved?.themeMode)
        assertEquals("USD", retrieved?.currencyCode)
    }
    
    @Test
    fun deleteAppSettings() = runTest {
        // Given
        val settings = AppSettingsEntity(id = 1L)
        settingsDao.insertAppSettings(settings)
        
        // When
        settingsDao.deleteAppSettings()
        val retrieved = settingsDao.getAppSettings()
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun insertAndGetDataManagementSettings() = runTest {
        // Given
        val settings = DataManagementSettingsEntity(
            id = 1L,
            autoDeleteOldTransactions = true,
            retentionPeriodMonths = 12,
            autoBackupEnabled = true
        )
        
        // When
        settingsDao.insertDataManagementSettings(settings)
        val retrieved = settingsDao.getDataManagementSettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(true, retrieved?.autoDeleteOldTransactions)
        assertEquals(12, retrieved?.retentionPeriodMonths)
        assertEquals(true, retrieved?.autoBackupEnabled)
    }
    
    @Test
    fun insertAndGetPrivacySettings() = runTest {
        // Given
        val settings = PrivacySettingsEntity(
            id = 1L,
            smsDataProcessingEnabled = true,
            localDataOnlyMode = false,
            requireAuthForSensitiveActions = true
        )
        
        // When
        settingsDao.insertPrivacySettings(settings)
        val retrieved = settingsDao.getPrivacySettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(true, retrieved?.smsDataProcessingEnabled)
        assertEquals(false, retrieved?.localDataOnlyMode)
        assertEquals(true, retrieved?.requireAuthForSensitiveActions)
    }
    
    @Test
    fun resetAllSettings() = runTest {
        // Given
        settingsDao.insertAppSettings(AppSettingsEntity(id = 1L))
        settingsDao.insertDataManagementSettings(DataManagementSettingsEntity(id = 1L))
        settingsDao.insertPrivacySettings(PrivacySettingsEntity(id = 1L))
        
        // When
        settingsDao.resetAllSettings()
        
        // Then
        assertNull(settingsDao.getAppSettings())
        assertNull(settingsDao.getDataManagementSettings())
        assertNull(settingsDao.getPrivacySettings())
    }
}