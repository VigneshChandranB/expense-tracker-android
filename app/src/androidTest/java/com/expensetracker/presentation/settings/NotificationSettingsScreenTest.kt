package com.expensetracker.presentation.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun displaysPermissionStatusWhenNotGranted() {
        val uiState = NotificationSettingsUiState(
            isNotificationPermissionGranted = false
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNodeWithText("Notification Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
    
    @Test
    fun displaysPermissionStatusWhenGranted() {
        val uiState = NotificationSettingsUiState(
            isNotificationPermissionGranted = true
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNodeWithText("Notification Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications are enabled").assertIsDisplayed()
    }
    
    @Test
    fun displaysGeneralNotificationSettings() {
        val preferences = NotificationPreferences(
            billRemindersEnabled = true,
            spendingLimitAlertsEnabled = false,
            lowBalanceWarningsEnabled = true
        )
        val uiState = NotificationSettingsUiState(
            preferences = preferences,
            isNotificationPermissionGranted = true
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNodeWithText("General Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bill Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spending Limit Alerts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Low Balance Warnings").assertIsDisplayed()
    }
    
    @Test
    fun displaysAccountSpecificSettings() {
        val account = Account(
            id = 1L,
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234",
            nickname = "Main Account",
            currentBalance = BigDecimal("10000.00"),
            createdAt = LocalDateTime.now()
        )
        val accountSettings = AccountNotificationSettings(
            accountId = 1L,
            spendingLimitEnabled = true,
            spendingLimit = BigDecimal("5000.00")
        )
        val uiState = NotificationSettingsUiState(
            accounts = listOf(account),
            accountSettings = mapOf(1L to accountSettings),
            isNotificationPermissionGranted = true
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNodeWithText("Account-Specific Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Main Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Bank • CHECKING").assertIsDisplayed()
    }
    
    @Test
    fun showsLoadingState() {
        val uiState = NotificationSettingsUiState(isLoading = true)
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNode(hasTestTag("loading_indicator") or hasContentDescription("Loading"))
            .assertExists()
    }
    
    @Test
    fun canToggleBillReminders() {
        val preferences = NotificationPreferences(billRemindersEnabled = false)
        val uiState = NotificationSettingsUiState(
            preferences = preferences,
            isNotificationPermissionGranted = true
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        // Find the switch for bill reminders and toggle it
        composeTestRule.onAllNodes(hasClickAction())
            .filterToOne(hasAnyAncestor(hasText("Bill Reminders")))
            .performClick()
        
        // In a real test, you would verify the event was sent to the ViewModel
    }
    
    @Test
    fun saveButtonIsDisplayed() {
        val uiState = NotificationSettingsUiState(
            isNotificationPermissionGranted = true
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NotificationSettingsScreen(
                    onNavigateBack = {},
                    viewModel = createMockViewModel(uiState)
                )
            }
        }
        
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }
    
    private fun createMockViewModel(uiState: NotificationSettingsUiState): NotificationSettingsViewModel {
        // In a real test, you would create a mock or fake ViewModel
        // For this example, we'll assume the ViewModel is properly mocked
        TODO("Create mock ViewModel implementation")
    }
}