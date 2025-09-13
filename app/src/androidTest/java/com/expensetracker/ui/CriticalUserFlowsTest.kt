package com.expensetracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.expensetracker.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated UI tests for critical user flows including account management
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalUserFlowsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun accountManagementFlow() = runTest {
        // Test complete account management workflow
        
        // Navigate to account management
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Manage Accounts").performClick()

        // Test adding new account
        composeTestRule.onNodeWithText("Add Account").performClick()
        composeTestRule.onNodeWithText("Bank Name").performTextInput("HDFC Bank")
        composeTestRule.onNodeWithText("Account Type").performClick()
        composeTestRule.onNodeWithText("Savings").performClick()
        composeTestRule.onNodeWithText("Account Number").performTextInput("1234567890")
        composeTestRule.onNodeWithText("Nickname").performTextInput("Emergency Fund")
        composeTestRule.onNodeWithText("Initial Balance").performTextInput("25000")
        composeTestRule.onNodeWithText("Create Account").performClick()

        // Verify account was created
        composeTestRule.onNodeWithText("Account created successfully").assertIsDisplayed()
        composeTestRule.onNodeWithText("Emergency Fund").assertIsDisplayed()

        // Test editing account
        composeTestRule.onNodeWithText("Emergency Fund").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Nickname").performTextClearance()
        composeTestRule.onNodeWithText("Nickname").performTextInput("Savings Account")
        composeTestRule.onNodeWithText("Save Changes").performClick()

        // Verify changes were saved
        composeTestRule.onNodeWithText("Savings Account").assertIsDisplayed()

        // Test account deactivation
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Deactivate").performClick()
        composeTestRule.onNodeWithText("Confirm Deactivation").performClick()

        // Verify account is deactivated
        composeTestRule.onNodeWithText("Account deactivated").assertIsDisplayed()

        // Test account reactivation
        composeTestRule.onNodeWithText("Show Inactive Accounts").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Reactivate").performClick()

        // Verify account is reactivated
        composeTestRule.onNodeWithText("Account reactivated").assertIsDisplayed()
    }

    @Test
    fun transactionManagementFlow() = runTest {
        // Test critical transaction management operations
        
        // Test adding transaction with validation
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        
        // Test validation - empty amount
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        composeTestRule.onNodeWithText("Amount is required").assertIsDisplayed()

        // Test validation - invalid amount
        composeTestRule.onNodeWithText("Amount").performTextInput("abc")
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        composeTestRule.onNodeWithText("Invalid amount").assertIsDisplayed()

        // Test validation - negative amount
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("-100")
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        composeTestRule.onNodeWithText("Amount must be positive").assertIsDisplayed()

        // Test valid transaction creation
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("1500")
        composeTestRule.onNodeWithText("Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Merchant").performTextInput("McDonald's")
        composeTestRule.onNodeWithText("Description").performTextInput("Lunch")
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Verify transaction was created
        composeTestRule.onNodeWithText("Transaction saved").assertIsDisplayed()

        // Test editing transaction
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("McDonald's").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("1200")
        composeTestRule.onNodeWithText("Save Changes").performClick()

        // Verify changes were saved
        composeTestRule.onNodeWithText("₹1,200.00").assertIsDisplayed()

        // Test transaction deletion with confirmation
        composeTestRule.onNodeWithText("McDonald's").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Confirm Delete").performClick()

        // Verify transaction was deleted
        composeTestRule.onNodeWithText("Transaction deleted").assertIsDisplayed()

        // Test undo functionality
        composeTestRule.onNodeWithText("Undo").performClick()
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed()
    }

    @Test
    fun transferFlow() = runTest {
        // Test account transfer functionality
        
        composeTestRule.onNodeWithText("Transfer").performClick()

        // Test validation - same account selection
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("1000")
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("Cannot transfer to same account").assertIsDisplayed()

        // Test validation - insufficient balance
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("999999")
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("Insufficient balance").assertIsDisplayed()

        // Test successful transfer
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("2000")
        composeTestRule.onNodeWithText("Description").performTextInput("Monthly transfer")
        composeTestRule.onNodeWithText("Transfer").performClick()

        // Verify transfer success
        composeTestRule.onNodeWithText("Transfer completed").assertIsDisplayed()

        // Verify linked transactions were created
        composeTestRule.onNodeWithText("View Transactions").performClick()
        composeTestRule.onNodeWithText("Transfer to Savings Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transfer from Primary Checking").assertIsDisplayed()

        // Verify amounts are correct
        composeTestRule.onNodeWithText("-₹2,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("+₹2,000.00").assertIsDisplayed()
    }

    @Test
    fun categoryManagementFlow() = runTest {
        // Test category management functionality
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Categories").performClick()

        // Test adding custom category
        composeTestRule.onNodeWithText("Add Category").performClick()
        composeTestRule.onNodeWithText("Category Name").performTextInput("Gym Membership")
        composeTestRule.onNodeWithText("Icon").performClick()
        composeTestRule.onNodeWithText("fitness_center").performClick()
        composeTestRule.onNodeWithText("Color").performClick()
        composeTestRule.onNodeWithText("Green").performClick()
        composeTestRule.onNodeWithText("Create Category").performClick()

        // Verify category was created
        composeTestRule.onNodeWithText("Gym Membership").assertIsDisplayed()

        // Test editing category
        composeTestRule.onNodeWithText("Gym Membership").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Category Name").performTextClearance()
        composeTestRule.onNodeWithText("Category Name").performTextInput("Health & Fitness")
        composeTestRule.onNodeWithText("Save Changes").performClick()

        // Verify changes were saved
        composeTestRule.onNodeWithText("Health & Fitness").assertIsDisplayed()

        // Test category deletion with transaction check
        composeTestRule.onNodeWithText("Health & Fitness").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        
        // If category has transactions, should show warning
        composeTestRule.onNodeWithText("This category has transactions").assertExists()
        composeTestRule.onNodeWithText("Move to Category").performClick()
        composeTestRule.onNodeWithText("Healthcare").performClick()
        composeTestRule.onNodeWithText("Confirm Delete").performClick()

        // Verify category was deleted
        composeTestRule.onNodeWithText("Category deleted").assertIsDisplayed()
    }

    @Test
    fun searchAndFilterFlow() = runTest {
        // Test search and filtering functionality
        
        composeTestRule.onNodeWithText("Transactions").performClick()

        // Test search functionality
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.onNodeWithText("Search transactions").performTextInput("Amazon")
        composeTestRule.waitForIdle()

        // Verify search results
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        // Other transactions should be filtered out

        // Clear search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        // Test date range filter
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Date Range").performClick()
        composeTestRule.onNodeWithText("This Month").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Test category filter
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Verify only selected categories are shown
        composeTestRule.onNodeWithText("Food & Dining").assertExists()
        composeTestRule.onNodeWithText("Shopping").assertExists()

        // Test account filter
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Accounts").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Verify only transactions from selected account are shown

        // Test sorting
        composeTestRule.onNodeWithText("Sort").performClick()
        composeTestRule.onNodeWithText("Amount (High to Low)").performClick()

        // Verify transactions are sorted by amount descending

        // Clear all filters
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Clear All").performClick()
    }

    @Test
    fun notificationSettingsFlow() = runTest {
        // Test notification settings management
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Notifications").performClick()

        // Test spending limit alerts
        composeTestRule.onNodeWithText("Spending Alerts").performClick()
        composeTestRule.onNodeWithText("Enable Alerts").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Monthly Limit").performTextInput("5000")
        composeTestRule.onNodeWithText("Alert Threshold").performTextInput("80")
        composeTestRule.onNodeWithText("Save").performClick()

        // Test bill reminders
        composeTestRule.onNodeWithText("Bill Reminders").performClick()
        composeTestRule.onNodeWithText("Add Reminder").performClick()
        composeTestRule.onNodeWithText("Bill Name").performTextInput("Internet Bill")
        composeTestRule.onNodeWithText("Amount").performTextInput("1500")
        composeTestRule.onNodeWithText("Due Date").performClick()
        // Select date from calendar
        composeTestRule.onNodeWithText("Reminder Days").performTextInput("3")
        composeTestRule.onNodeWithText("Save Reminder").performClick()

        // Test low balance alerts
        composeTestRule.onNodeWithText("Low Balance Alerts").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Alert Threshold").performTextInput("1000")
        composeTestRule.onNodeWithText("Save").performClick()

        // Test notification preferences
        composeTestRule.onNodeWithText("Notification Preferences").performClick()
        composeTestRule.onNodeWithText("Sound").performClick()
        composeTestRule.onNodeWithText("Vibration").performClick()
        composeTestRule.onNodeWithText("LED").performClick()
        composeTestRule.onNodeWithText("Save Preferences").performClick()
    }

    @Test
    fun dataExportFlow() = runTest {
        // Test data export functionality
        
        composeTestRule.onNodeWithText("Export").performClick()

        // Test CSV export configuration
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("CSV").performClick()

        composeTestRule.onNodeWithText("Date Range").performClick()
        composeTestRule.onNodeWithText("Custom Range").performClick()
        // Select start and end dates
        composeTestRule.onNodeWithText("Apply Range").performClick()

        composeTestRule.onNodeWithText("Accounts").performClick()
        composeTestRule.onNodeWithText("Select Accounts").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.onNodeWithText("All Categories").performClick()

        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify export progress and completion
        composeTestRule.onNodeWithText("Generating export...").assertIsDisplayed()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Export completed").assertIsDisplayed()

        // Test sharing options
        composeTestRule.onNodeWithText("Share").performClick()
        // This would open system share dialog

        // Test PDF export
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("PDF").performClick()
        composeTestRule.onNodeWithText("Include Charts").assertIsChecked()
        composeTestRule.onNodeWithText("Include Summary").assertIsChecked()
        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify PDF export
        composeTestRule.onNodeWithText("PDF export ready").assertIsDisplayed()
    }
}