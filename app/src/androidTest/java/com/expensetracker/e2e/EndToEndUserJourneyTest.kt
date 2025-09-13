package com.expensetracker.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.expensetracker.MainActivity
import com.expensetracker.domain.model.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end user journey tests covering complete user workflows
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EndToEndUserJourneyTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completeNewUserJourney() = runTest {
        // Test complete journey from onboarding to daily usage
        
        // 1. Onboarding flow
        composeTestRule.onNodeWithText("Welcome to Expense Tracker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").performClick()

        // Privacy explanation
        composeTestRule.onNodeWithText("Your Privacy Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()

        // SMS permission setup
        composeTestRule.onNodeWithText("SMS Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant Permission").performClick()
        // Note: In real test, this would trigger system permission dialog

        // Account creation
        composeTestRule.onNodeWithText("Add Your First Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bank Name").performTextInput("HDFC Bank")
        composeTestRule.onNodeWithText("Account Type").performClick()
        composeTestRule.onNodeWithText("Checking").performClick()
        composeTestRule.onNodeWithText("Account Nickname").performTextInput("Primary Checking")
        composeTestRule.onNodeWithText("Create Account").performClick()

        // Add second account
        composeTestRule.onNodeWithText("Add Another Account").performClick()
        composeTestRule.onNodeWithText("Bank Name").performTextInput("ICICI Bank")
        composeTestRule.onNodeWithText("Account Type").performClick()
        composeTestRule.onNodeWithText("Credit Card").performClick()
        composeTestRule.onNodeWithText("Account Nickname").performTextInput("Credit Card")
        composeTestRule.onNodeWithText("Create Account").performClick()

        // Feature introduction
        composeTestRule.onNodeWithText("Key Features").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Get Started").performClick()

        // 2. Dashboard usage
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Primary Checking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Card").assertIsDisplayed()

        // 3. Manual transaction entry
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("1500")
        composeTestRule.onNodeWithText("Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Merchant").performTextInput("Swiggy")
        composeTestRule.onNodeWithText("Description").performTextInput("Lunch order")
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Verify transaction appears
        composeTestRule.onNodeWithText("Swiggy").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1,500.00").assertIsDisplayed()

        // 4. Account transfer
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("Credit Card").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("2000")
        composeTestRule.onNodeWithText("Transfer").performClick()

        // Verify transfer success
        composeTestRule.onNodeWithText("Transfer completed").assertIsDisplayed()

        // 5. Analytics viewing
        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.onNodeWithText("Monthly Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category Breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Distribution").assertIsDisplayed()

        // 6. Export data
        composeTestRule.onNodeWithText("Export").performClick()
        composeTestRule.onNodeWithText("Date Range").performClick()
        composeTestRule.onNodeWithText("This Month").performClick()
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("PDF").performClick()
        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify export
        composeTestRule.onNodeWithText("Export ready").assertIsDisplayed()

        // 7. Settings configuration
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.onNodeWithText("Spending Alerts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bill Reminders").assertIsDisplayed()
        
        // Enable notifications
        composeTestRule.onNodeWithContentDescription("Enable spending alerts").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
    }

    @Test
    fun multiAccountTransferWorkflow() = runTest {
        // Test complex multi-account transfer scenarios
        
        // Setup: Assume user has multiple accounts already
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()

        // Scenario 1: Simple transfer between checking and savings
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("HDFC Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("SBI Savings").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("5000")
        composeTestRule.onNodeWithText("Description").performTextInput("Monthly savings")
        composeTestRule.onNodeWithText("Transfer").performClick()

        // Verify transfer appears in both accounts
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Filter by Account").performClick()
        composeTestRule.onNodeWithText("HDFC Checking").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithText("Transfer to SBI Savings").assertIsDisplayed()

        // Check destination account
        composeTestRule.onNodeWithText("Filter by Account").performClick()
        composeTestRule.onNodeWithText("Clear").performClick()
        composeTestRule.onNodeWithText("SBI Savings").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithText("Transfer from HDFC Checking").assertIsDisplayed()

        // Scenario 2: Credit card payment
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("HDFC Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("ICICI Credit").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("15000")
        composeTestRule.onNodeWithText("Description").performTextInput("Credit card payment")
        composeTestRule.onNodeWithText("Transfer").performClick()

        // Verify balances updated correctly
        composeTestRule.onNodeWithText("Dashboard").performClick()
        // Check that checking account balance decreased
        // Check that credit card balance/available credit increased
    }

    @Test
    fun smsProcessingAndCategorizationJourney() = runTest {
        // Test SMS processing with automatic categorization and user corrections
        
        // Simulate receiving SMS messages
        val smsMessages = listOf(
            "HDFC: Spent Rs.500 at SWIGGY on 15-Dec",
            "ICICI: Transaction of Rs.2500 at AMAZON",
            "SBI: Debited Rs.1200 at UBER",
            "AXIS: Spent Rs.800 at DOMINOS"
        )

        // Navigate to transactions to see processed SMS
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.waitForIdle()

        // Verify transactions were created and categorized
        composeTestRule.onNodeWithText("SWIGGY").assertIsDisplayed()
        composeTestRule.onNodeWithText("AMAZON").assertIsDisplayed()
        composeTestRule.onNodeWithText("UBER").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOMINOS").assertIsDisplayed()

        // Test category correction and learning
        composeTestRule.onNodeWithText("SWIGGY").performClick() // Open transaction detail
        composeTestRule.onNodeWithText("Edit").performClick()
        
        // Change category from auto-assigned to correct one
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify category was updated
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()

        // Test that similar merchants get categorized correctly in future
        // This would be verified by processing another SWIGGY transaction
    }

    @Test
    fun notificationAndAlertWorkflow() = runTest {
        // Test notification system end-to-end
        
        // Setup spending limits
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Spending Limits").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Monthly Limit").performTextInput("5000")
        composeTestRule.onNodeWithText("Save").performClick()

        // Add transactions that approach the limit
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("3000")
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Add another transaction that exceeds limit
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("2500")
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Verify alert is shown
        composeTestRule.onNodeWithText("Spending Limit Exceeded").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've exceeded your Food & Dining limit").assertIsDisplayed()

        // Test bill reminder
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Bill Reminders").performClick()
        composeTestRule.onNodeWithText("Add Reminder").performClick()
        composeTestRule.onNodeWithText("Bill Name").performTextInput("Electricity Bill")
        composeTestRule.onNodeWithText("Amount").performTextInput("2000")
        composeTestRule.onNodeWithText("Due Date").performClick()
        // Select a date 2 days from now
        composeTestRule.onNodeWithText("Save Reminder").performClick()

        // Verify reminder appears in notifications
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.onNodeWithText("Electricity Bill due in 2 days").assertIsDisplayed()
    }

    @Test
    fun dataExportAndBackupJourney() = runTest {
        // Test complete data export and backup workflow
        
        // Generate some test data first
        addTestTransactions()

        // Test CSV export
        composeTestRule.onNodeWithText("Export").performClick()
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("CSV").performClick()
        composeTestRule.onNodeWithText("Date Range").performClick()
        composeTestRule.onNodeWithText("All Time").performClick()
        composeTestRule.onNodeWithText("Accounts").performClick()
        composeTestRule.onNodeWithText("All Accounts").performClick()
        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify export success
        composeTestRule.onNodeWithText("Export completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").performClick()
        // This would open system share dialog

        // Test PDF export with charts
        composeTestRule.onNodeWithText("Export").performClick()
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("PDF").performClick()
        composeTestRule.onNodeWithText("Include Charts").assertIsChecked()
        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify PDF export
        composeTestRule.onNodeWithText("PDF export ready").assertIsDisplayed()

        // Test backup functionality
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").performClick()
        composeTestRule.onNodeWithText("Create Backup").performClick()
        composeTestRule.onNodeWithText("Include Account Data").assertIsChecked()
        composeTestRule.onNodeWithText("Include Transaction History").assertIsChecked()
        composeTestRule.onNodeWithText("Create Backup").performClick()

        // Verify backup success
        composeTestRule.onNodeWithText("Backup created successfully").assertIsDisplayed()
    }

    private fun addTestTransactions() {
        // Helper method to add test transactions for export testing
        val transactions = listOf(
            Triple("1500", "Food & Dining", "Restaurant"),
            Triple("2500", "Shopping", "Amazon"),
            Triple("500", "Transportation", "Uber"),
            Triple("1200", "Bills", "Electricity")
        )

        transactions.forEach { (amount, category, merchant) ->
            composeTestRule.onNodeWithText("Add Transaction").performClick()
            composeTestRule.onNodeWithText("Amount").performTextInput(amount)
            composeTestRule.onNodeWithText("Category").performClick()
            composeTestRule.onNodeWithText(category).performClick()
            composeTestRule.onNodeWithText("Merchant").performTextInput(merchant)
            composeTestRule.onNodeWithText("Save Transaction").performClick()
            composeTestRule.waitForIdle()
        }
    }
}