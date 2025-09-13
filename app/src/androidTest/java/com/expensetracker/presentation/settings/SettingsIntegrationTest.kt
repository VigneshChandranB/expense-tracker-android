package com.expensetracker.presentation.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.mapper.SettingsMapper
import com.expensetracker.data.repository.SettingsRepositoryImpl
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for settings functionality
 */
@RunWith(AndroidJUnit4::class)
class SettingsIntegrationTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: SettingsRepositoryImpl
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = SettingsRepositoryImpl(
            settingsDao = database.settingsDao(),
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            notificationDao = database.notificationDao()
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun settingsFlow_saveAndRetrieve() = runTest {
        // Given
        val settings = AppSettings(
            themeMode = ThemeMode.DARK,
            smsPermissionEnabled = true,
            currencyCode = "USD",
            autoCategorizationEnabled = false
        )
        
        // When
        repository.updateAppSettings(settings)
        val retrieved = repository.getAppSettings()
        
        // Then
        assertEquals(ThemeMode.DARK, retrieved.themeMode)
        assertEquals(true, retrieved.smsPermissionEnabled)
        assertEquals("USD", retrieved.currencyCode)
        assertEquals(false, retrieved.autoCategorizationEnabled)
    }
    
    @Test
    fun settingsFlow_observeChanges() = runTest {
        // Given
        val initialSettings = AppSettings(themeMode = ThemeMode.LIGHT)
        repository.updateAppSettings(initialSettings)
        
        // When
        val flow = repository.observeAppSettings()
        
        // Then
        flow.collect { settings ->
            assertEquals(ThemeMode.LIGHT, settings.themeMode)
        }
    }
    
    @Test
    fun dataManagement_deleteAllData() = runTest {
        // Given - Insert some test data
        val settings = AppSettings(themeMode = ThemeMode.DARK)
        repository.updateAppSettings(settings)
        
        // When
        repository.deleteAllData()
        
        // Then
        val retrievedSettings = repository.getAppSettings()
        assertEquals(AppSettings(), retrievedSettings) // Should return default settings
    }
}