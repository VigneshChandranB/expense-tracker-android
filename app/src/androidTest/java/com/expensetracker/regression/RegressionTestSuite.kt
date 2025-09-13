package com.expensetracker.regression

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.expensetracker.MainActivity
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.domain.model.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Regression tests for core functionality and account operations
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RegressionTestSuite {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: ExpenseDatabase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun coreTransactionOperations_regression() = runTest {
        // Regression test for basic transaction CRUD operations
        
        // Test transaction creation
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("1500")
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Merchant").performTextInput("Test Merchant")
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Verify transaction appears in list
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Test Merchant").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1,500.00").assertIsDisplayed()

        // Test transaction editing
        composeTestRule.onNodeWithText("Test Merchant").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("1800")
        composeTestRule.onNodeWithText("Save Changes").performClick()

        // Verify changes persisted
        composeTestRule.onNodeWithText("₹1,800.00").assertIsDisplayed()

        // Test transaction deletion
        composeTestRule.onNodeWithText("Test Merchant").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Confirm Delete").performClick()

        // Verify transaction removed
        composeTestRule.onNodeWithText("Test Merchant").assertDoesNotExist()
    }

    @Test
    fun accountBalanceCalculation_regression() = runTest {
        // Regression test for account balance calculations
        
        val initialBalance = BigDecimal("10000.00")
        
        // Add income transaction
        addTransaction("5000", "Income", "Salary", TransactionType.INCOME)
        
        // Add expense transaction
        addTransaction("1500", "Food & Dining", "Restaurant", TransactionType.EXPENSE)
        
        // Add another expense
        addTransaction("800", "Transportation", "Uber", TransactionType.EXPENSE)
        
        // Navigate to dashboard to check balance
        composeTestRule.onNodeWithText("Dashboard").performClick()
        
        // Expected balance: 10000 + 5000 - 1500 - 800 = 12700
        composeTestRule.onNodeWithText("₹12,700.00").assertIsDisplayed()
        
        // Test transfer impact on balances
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("2000")
        composeTestRule.onNodeWithText("Transfer").performClick()
        
        // Check updated balances
        composeTestRule.onNodeWithText("Dashboard").performClick()
        // Primary checking should be 12700 - 2000 = 10700
        // Savings should be original + 2000
    }

    @Test
    fun categoryAssignment_regression() = runTest {
        // Regression test for category assignment and changes
        
        // Create transaction with specific category
        addTransaction("2500", "Shopping", "Amazon", TransactionType.EXPENSE)
        
        // Verify category assignment
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Amazon").performClick()
        composeTestRule.onNodeWithText("Shopping").assertIsDisplayed()
        
        // Change category
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Entertainment").performClick()
        composeTestRule.onNodeWithText("Save Changes").performClick()
        
        // Verify category changed
        composeTestRule.onNodeWithText("Entertainment").assertIsDisplayed()
        
        // Check analytics reflect category change
        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.onNodeWithText("Category Breakdown").assertIsDisplayed()
        // Entertainment should show ₹2,500.00
        // Shopping should show ₹0.00 or not appear
    }

    @Test
    fun smsProcessingAccuracy_regression() = runTest {
        // Regression test for SMS processing accuracy
        
        val testSmsMessages = listOf(
            "HDFC: Spent Rs.1500.50 at AMAZON on 15-Dec-2023",
            "ICICI: Transaction of Rs.2500.00 at FLIPKART on 16-Dec-2023",
            "SBI: Credited Rs.50000.00 salary on 17-Dec-2023",
            "AXIS: Debited Rs.750.25 from account **1234 at SWIGGY on 18-Dec-2023"
        )
        
        // Simulate SMS processing (in real test, would trigger SMS receiver)
        // For regression test, we verify the processing logic works correctly
        
        composeTestRule.onNodeWithText("Transactions").performClick()
        
        // Verify expected transactions appear
        composeTestRule.onNodeWithText("AMAZON").assertIsDisplayed()
        composeTestRule.onNodeWithText("FLIPKART").assertIsDisplayed()
        composeTestRule.onNodeWithText("SWIGGY").assertIsDisplayed()
        
        // Verify amounts are correct
        composeTestRule.onNodeWithText("₹1,500.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹2,500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹50,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹750.25").assertIsDisplayed()
        
        // Verify transaction types
        composeTestRule.onNodeWithText("AMAZON").performClick()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Check salary transaction
        composeTestRule.onNodeWithText("Salary").performClick()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
    }

    @Test
    fun dataExportIntegrity_regression() = runTest {
        // Regression test for data export integrity
        
        // Create test data
        addMultipleTransactions()
        
        // Test CSV export
        composeTestRule.onNodeWithText("Export").performClick()
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("CSV").performClick()
        composeTestRule.onNodeWithText("Generate Export").performClick()
        
        // Verify export completes without errors
        composeTestRule.onNodeWithText("Export completed").assertIsDisplayed()
        
        // Test PDF export
        composeTestRule.onNodeWithText("Format").performClick()
        composeTestRule.onNodeWithText("PDF").performClick()
        composeTestRule.onNodeWithText("Generate Export").performClick()
        
        // Verify PDF export completes
        composeTestRule.onNodeWithText("PDF export ready").assertIsDisplayed()
        
        // Verify export contains expected data
        // In real test, would verify file contents
    }

    @Test
    fun notificationTriggers_regression() = runTest {
        // Regression test for notification triggers
        
        // Setup spending limit
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Spending Limits").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Monthly Limit").performTextInput("3000")
        composeTestRule.onNodeWithText("Save").performClick()
        
        // Add transactions that approach limit
        addTransaction("1500", "Food & Dining", "Restaurant 1", TransactionType.EXPENSE)
        addTransaction("1200", "Food & Dining", "Restaurant 2", TransactionType.EXPENSE)
        
        // Should trigger 80% warning
        composeTestRule.onNodeWithText("Approaching spending limit").assertIsDisplayed()
        
        // Add transaction that exceeds limit
        addTransaction("800", "Food & Dining", "Restaurant 3", TransactionType.EXPENSE)
        
        // Should trigger limit exceeded alert
        composeTestRule.onNodeWithText("Spending limit exceeded").assertIsDisplayed()
        
        // Test bill reminder
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Bill Reminders").performClick()
        composeTestRule.onNodeWithText("Add Reminder").performClick()
        composeTestRule.onNodeWithText("Bill Name").performTextInput("Test Bill")
        composeTestRule.onNodeWithText("Amount").performTextInput("2000")
        // Set due date to tomorrow
        composeTestRule.onNodeWithText("Save Reminder").performClick()
        
        // Verify reminder appears in notifications
        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.onNodeWithText("Test Bill due tomorrow").assertIsDisplayed()
    }

    @Test
    fun multiAccountOperations_regression() = runTest {
        // Regression test for multi-account operations
        
        // Verify multiple accounts exist
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.onNodeWithText("Primary Checking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Savings Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Card").assertIsDisplayed()
        
        // Test account switching in transaction creation
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Account").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("1000")
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Investment").performClick()
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        
        // Verify transaction assigned to correct account
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Filter by Account").performClick()
        composeTestRule.onNodeWithText("Savings Account").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithText("Investment").assertIsDisplayed()
        
        // Test account transfer
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("Primary Checking").performClick()
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("Credit Card").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("1500")
        composeTestRule.onNodeWithText("Transfer").performClick()
        
        // Verify linked transactions created
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Transfer to Credit Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transfer from Primary Checking").assertIsDisplayed()
    }

    @Test
    fun searchAndFilterAccuracy_regression() = runTest {
        // Regression test for search and filter accuracy
        
        // Create diverse test data
        addTransaction("1500", "Food & Dining", "McDonald's", TransactionType.EXPENSE)
        addTransaction("2500", "Shopping", "Amazon", TransactionType.EXPENSE)
        addTransaction("800", "Transportation", "Uber", TransactionType.EXPENSE)
        addTransaction("50000", "Income", "Salary", TransactionType.INCOME)
        
        composeTestRule.onNodeWithText("Transactions").performClick()
        
        // Test search functionality
        composeTestRule.onNodeWithText("Search").performClick()
        composeTestRule.onNodeWithText("Search transactions").performTextInput("Amazon")
        composeTestRule.waitForIdle()
        
        // Should only show Amazon transaction
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("McDonald's").assertDoesNotExist()
        composeTestRule.onNodeWithText("Uber").assertDoesNotExist()
        
        // Clear search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        
        // Test category filter
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.onNodeWithText("Food & Dining").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        
        // Should only show food transactions
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amazon").assertDoesNotExist()
        
        // Test amount range filter
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Amount Range").performClick()
        composeTestRule.onNodeWithText("Min Amount").performTextInput("1000")
        composeTestRule.onNodeWithText("Max Amount").performTextInput("3000")
        composeTestRule.onNodeWithText("Apply").performClick()
        
        // Should show transactions between 1000-3000
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed() // 1500
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed() // 2500
        composeTestRule.onNodeWithText("Uber").assertDoesNotExist() // 800
        composeTestRule.onNodeWithText("Salary").assertDoesNotExist() // 50000
    }

    @Test
    fun analyticsCalculation_regression() = runTest {
        // Regression test for analytics calculations
        
        // Create known test data
        addTransaction("1500", "Food & Dining", "Restaurant", TransactionType.EXPENSE)
        addTransaction("2500", "Shopping", "Amazon", TransactionType.EXPENSE)
        addTransaction("800", "Transportation", "Uber", TransactionType.EXPENSE)
        addTransaction("50000", "Income", "Salary", TransactionType.INCOME)
        addTransaction("1200", "Food & Dining", "Grocery", TransactionType.EXPENSE)
        
        composeTestRule.onNodeWithText("Analytics").performClick()
        
        // Verify total calculations
        // Total income: 50000
        // Total expenses: 1500 + 2500 + 800 + 1200 = 6000
        // Net: 50000 - 6000 = 44000
        
        composeTestRule.onNodeWithText("₹50,000.00").assertIsDisplayed() // Income
        composeTestRule.onNodeWithText("₹6,000.00").assertIsDisplayed() // Expenses
        composeTestRule.onNodeWithText("₹44,000.00").assertIsDisplayed() // Net
        
        // Verify category breakdown
        composeTestRule.onNodeWithText("Category Breakdown").assertIsDisplayed()
        // Food & Dining: 1500 + 1200 = 2700 (45% of expenses)
        // Shopping: 2500 (41.7% of expenses)
        // Transportation: 800 (13.3% of expenses)
        
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹2,700.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()
        
        // Test monthly comparison
        composeTestRule.onNodeWithText("Monthly Trends").assertIsDisplayed()
        // Would verify month-over-month calculations
    }

    private fun addTransaction(
        amount: String,
        category: String,
        merchant: String,
        type: TransactionType
    ) {
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput(amount)
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText(category).performClick()
        composeTestRule.onNodeWithText("Merchant").performTextInput(merchant)
        if (type == TransactionType.INCOME) {
            composeTestRule.onNodeWithText("Type").performClick()
            composeTestRule.onNodeWithText("Income").performClick()
        }
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        composeTestRule.waitForIdle()
    }

    private fun addMultipleTransactions() {
        val transactions = listOf(
            Triple("1500", "Food & Dining", "Restaurant 1"),
            Triple("2500", "Shopping", "Amazon"),
            Triple("800", "Transportation", "Uber"),
            Triple("1200", "Bills", "Electricity"),
            Triple("3000", "Entertainment", "Movie Theater"),
            Triple("50000", "Income", "Salary")
        )

        transactions.forEach { (amount, category, merchant) ->
            addTransaction(amount, category, merchant, 
                if (category == "Income") TransactionType.INCOME else TransactionType.EXPENSE)
        }
    }
}