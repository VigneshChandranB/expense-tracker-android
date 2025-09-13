package com.expensetracker.presentation.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for SettingsScreen
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsScreen_displaysAllCategories() {
        // Given
        var navigatedToAppSettings = false
        var navigatedToNotifications = false
        var navigatedToDataManagement = false
        var navigatedToPrivacy = false
        var navigatedToAccounts = false
        var navigatedToBackup = false
        
        // When
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SettingsScreen(
                    onNavigateToAppSettings = { navigatedToAppSettings = true },
                    onNavigateToNotificationSettings = { navigatedToNotifications = true },
                    onNavigateToDataManagement = { navigatedToDataManagement = true },
                    onNavigateToPrivacySettings = { navigatedToPrivacy = true },
                    onNavigateToAccountManagement = { navigatedToAccounts = true },
                    onNavigateToBackupRestore = { navigatedToBackup = true },
                    onNavigateBack = { }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("App Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Management").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data Management").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy & Security").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_navigatesToAppSettings() {
        // Given
        var navigatedToAppSettings = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SettingsScreen(
                    onNavigateToAppSettings = { navigatedToAppSettings = true },
                    onNavigateToNotificationSettings = { },
                    onNavigateToDataManagement = { },
                    onNavigateToPrivacySettings = { },
                    onNavigateToAccountManagement = { },
                    onNavigateToBackupRestore = { },
                    onNavigateBack = { }
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithText("App Settings").performClick()
        
        // Then
        assert(navigatedToAppSettings)
    }
    
    @Test
    fun settingsScreen_navigatesToNotifications() {
        // Given
        var navigatedToNotifications = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SettingsScreen(
                    onNavigateToAppSettings = { },
                    onNavigateToNotificationSettings = { navigatedToNotifications = true },
                    onNavigateToDataManagement = { },
                    onNavigateToPrivacySettings = { },
                    onNavigateToAccountManagement = { },
                    onNavigateToBackupRestore = { },
                    onNavigateBack = { }
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithText("Notifications").performClick()
        
        // Then
        assert(navigatedToNotifications)
    }
    
    @Test
    fun settingsScreen_displaysAppInfo() {
        // When
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SettingsScreen(
                    onNavigateToAppSettings = { },
                    onNavigateToNotificationSettings = { },
                    onNavigateToDataManagement = { },
                    onNavigateToPrivacySettings = { },
                    onNavigateToAccountManagement = { },
                    onNavigateToBackupRestore = { },
                    onNavigateBack = { }
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("App Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version: 1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Build: 1").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_backButtonWorks() {
        // Given
        var navigatedBack = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SettingsScreen(
                    onNavigateToAppSettings = { },
                    onNavigateToNotificationSettings = { },
                    onNavigateToDataManagement = { },
                    onNavigateToPrivacySettings = { },
                    onNavigateToAccountManagement = { },
                    onNavigateToBackupRestore = { },
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Then
        assert(navigatedBack)
    }
}