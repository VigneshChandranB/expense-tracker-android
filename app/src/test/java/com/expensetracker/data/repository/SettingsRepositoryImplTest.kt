package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.SettingsDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.dao.AccountDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.NotificationDao
import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.local.entity.DataManagementSettingsEntity
import com.expensetracker.data.local.entity.PrivacySettingsEntity
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SettingsRepositoryImpl
 */
class SettingsRepositoryImplTest {
    
    private lateinit var settingsDao: SettingsDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var notificationDao: NotificationDao
    private lateinit var repository: SettingsRepositoryImpl
    
    @Before
    fun setup() {
        settingsDao = mockk()
        transactionDao = mockk()
        accountDao = mockk()
        categoryDao = mockk()
        notificationDao = mockk()
        repository = SettingsRepositoryImpl(
            settingsDao = settingsDao,
            transactionDao = transactionDao,
            accountDao = accountDao,
            categoryDao = categoryDao,
            notificationDao = notificationDao
        )
    }
    
    @Test
    fun `getAppSettings returns default settings when none exist`() = runTest {
        // Given
        coEvery { settingsDao.getAppSettings() } returns null
        
        // When
        val result = repository.getAppSettings()
        
        // Then
        assertEquals(AppSettings(), result)
    }
    
    @Test
    fun `getAppSettings returns mapped settings when they exist`() = runTest {
        // Given
        val entity = AppSettingsEntity(
            id = 1L,
            themeMode = "DARK",
            smsPermissionEnabled = true,
            autoCategorizationEnabled = false,
            currencyCode = "USD"
        )
        coEvery { settingsDao.getAppSettings() } returns entity
        
        // When
        val result = repository.getAppSettings()
        
        // Then
        assertEquals(ThemeMode.DARK, result.themeMode)
        assertEquals(true, result.smsPermissionEnabled)
        assertEquals(false, result.autoCategorizationEnabled)
        assertEquals("USD", result.currencyCode)
    }
    
    @Test
    fun `updateAppSettings saves settings correctly`() = runTest {
        // Given
        val settings = AppSettings(
            themeMode = ThemeMode.LIGHT,
            smsPermissionEnabled = true,
            currencyCode = "EUR"
        )
        coEvery { settingsDao.insertAppSettings(any()) } returns Unit
        
        // When
        repository.updateAppSettings(settings)
        
        // Then
        coVerify {
            settingsDao.insertAppSettings(
                match { entity ->
                    entity.themeMode == "LIGHT" &&
                    entity.smsPermissionEnabled == true &&
                    entity.currencyCode == "EUR"
                }
            )
        }
    }
    
    @Test
    fun `observeAppSettings returns flow of settings`() = runTest {
        // Given
        val entity = AppSettingsEntity(themeMode = "SYSTEM")
        coEvery { settingsDao.observeAppSettings() } returns flowOf(entity)
        
        // When
        val flow = repository.observeAppSettings()
        
        // Then
        flow.collect { settings ->
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        }
    }
    
    @Test
    fun `deleteAllData calls all delete methods`() = runTest {
        // Given
        coEvery { transactionDao.deleteAllTransactions() } returns Unit
        coEvery { accountDao.deleteAllAccounts() } returns Unit
        coEvery { categoryDao.deleteAllCustomCategories() } returns Unit
        coEvery { settingsDao.resetAllSettings() } returns Unit
        coEvery { notificationDao.deleteAllNotifications() } returns Unit
        
        // When
        repository.deleteAllData()
        
        // Then
        coVerify { transactionDao.deleteAllTransactions() }
        coVerify { accountDao.deleteAllAccounts() }
        coVerify { categoryDao.deleteAllCustomCategories() }
        coVerify { settingsDao.resetAllSettings() }
        coVerify { notificationDao.deleteAllNotifications() }
    }
    
    @Test
    fun `deleteSmsData calls delete SMS transactions`() = runTest {
        // Given
        coEvery { transactionDao.deleteSmsTransactions() } returns Unit
        
        // When
        repository.deleteSmsData()
        
        // Then
        coVerify { transactionDao.deleteSmsTransactions() }
    }
}