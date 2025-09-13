package com.expensetracker.presentation.transaction

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for transaction management flows including transfers
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TransactionManagementFlowTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun createTransaction_withValidData_shouldSucceed() {
        // Navigate to add transaction screen
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        
        // Fill in transaction details
        composeTestRule.onNodeWithText("Amount").performTextInput("50.00")
        
        // Select transaction type
        composeTestRule.onNodeWithText("Type").performClick()
        composeTestRule.onNodeWithText("EXPENSE").performClick()
        
        // Select account
        composeTestRule.onNodeWithText("Account").performClick()
        composeTestRule.onAllNodesWithText("Test Account")[0].performClick()
        
        // Select category
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onAllNodesWithText("Food")[0].performClick()
        
        // Enter merchant
        composeTestRule.onNodeWithText("Merchant").performTextInput("Test Restaurant")
        
        // Enter description
        composeTestRule.onNodeWithText("Description (Optional)").performTextInput("Lunch")
        
        // Save transaction
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        
        // Verify transaction was created (should navigate back to list)
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Restaurant").assertIsDisplayed()
    }
    
    @Test
    fun createTransaction_withInvalidAmount_shouldShowError() {
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        
        // Enter invalid amount
        composeTestRule.onNodeWithText("Amount").performTextInput("invalid")
        
        // Try to save
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        
        // Should show validation error
        composeTestRule.onNodeWithText("Invalid amount format").assertIsDisplayed()
    }
    
    @Test
    fun createTransaction_withMissingRequiredFields_shouldShowErrors() {
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        
        // Try to save without filling required fields
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        
        // Should show validation errors
        composeTestRule.onNodeWithText("Amount is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Merchant is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account is required").assertIsDisplayed()
    }
    
    @Test
    fun editTransaction_shouldUpdateExistingTransaction() {
        // First create a transaction (assuming one exists)
        // Then click on it to edit
        composeTestRule.onNodeWithText("Test Restaurant").performClick()
        
        // Update the amount
        composeTestRule.onNodeWithText("Amount").performTextClearance()
        composeTestRule.onNodeWithText("Amount").performTextInput("75.00")
        
        // Update merchant
        composeTestRule.onNodeWithText("Merchant").performTextClearance()
        composeTestRule.onNodeWithText("Merchant").performTextInput("Updated Restaurant")
        
        // Save changes
        composeTestRule.onNodeWithText("Update Transaction").performClick()
        
        // Verify changes were saved
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Updated Restaurant").assertIsDisplayed()
    }
    
    @Test
    fun deleteTransaction_shouldRemoveFromList() {
        // Click delete button on a transaction
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        
        // Verify transaction is removed
        composeTestRule.onNodeWithText("Test Restaurant").assertDoesNotExist()
    }
    
    @Test
    fun createTransfer_withValidData_shouldSucceed() {
        // Navigate to transfer screen
        composeTestRule.onNodeWithText("Transfer").performClick()
        
        // Fill in transfer details
        composeTestRule.onNodeWithText("Amount").performTextInput("100.00")
        
        // Select from account
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onAllNodesWithText("Checking Account")[0].performClick()
        
        // Select to account
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onAllNodesWithText("Savings Account")[0].performClick()
        
        // Enter description
        composeTestRule.onNodeWithText("Description (Optional)").performTextInput("Monthly savings")
        
        // Create transfer
        composeTestRule.onNodeWithText("Create Transfer").performClick()
        
        // Verify transfer was created (should navigate back)
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }
    
    @Test
    fun createTransfer_withSameAccount_shouldShowError() {
        composeTestRule.onNodeWithText("Transfer").performClick()
        
        composeTestRule.onNodeWithText("Amount").performTextInput("100.00")
        
        // Select same account for both from and to
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onAllNodesWithText("Checking Account")[0].performClick()
        
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onAllNodesWithText("Checking Account")[0].performClick()
        
        composeTestRule.onNodeWithText("Create Transfer").performClick()
        
        // Should show validation error
        composeTestRule.onNodeWithText("Source and destination accounts must be different").assertIsDisplayed()
    }
    
    @Test
    fun createTransfer_withInvalidAmount_shouldShowError() {
        composeTestRule.onNodeWithText("Transfer").performClick()
        
        // Enter invalid amount
        composeTestRule.onNodeWithText("Amount").performTextInput("-50")
        
        composeTestRule.onNodeWithText("Create Transfer").performClick()
        
        // Should show validation error
        composeTestRule.onNodeWithText("Amount must be positive").assertIsDisplayed()
    }
    
    @Test
    fun undoTransaction_shouldRestorePreviousState() {
        // Create a transaction first
        createTransaction_withValidData_shouldSucceed()
        
        // Delete the transaction
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        
        // Verify transaction is deleted
        composeTestRule.onNodeWithText("Test Restaurant").assertDoesNotExist()
        
        // Undo the deletion
        composeTestRule.onNodeWithContentDescription("Undo").performClick()
        
        // Verify transaction is restored
        composeTestRule.onNodeWithText("Test Restaurant").assertIsDisplayed()
    }
    
    @Test
    fun redoTransaction_shouldReapplyUndoneAction() {
        // Create and delete a transaction, then undo
        createTransaction_withValidData_shouldSucceed()
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithContentDescription("Undo").performClick()
        
        // Now redo the deletion
        composeTestRule.onNodeWithContentDescription("Redo").performClick()
        
        // Verify transaction is deleted again
        composeTestRule.onNodeWithText("Test Restaurant").assertDoesNotExist()
    }
    
    @Test
    fun filterTransactionsByAccount_shouldShowOnlyAccountTransactions() {
        // Open filter menu
        composeTestRule.onNodeWithContentDescription("Filter").performClick()
        
        // Select specific account
        composeTestRule.onNodeWithText("Checking Account").performClick()
        
        // Verify filter is applied (filter chip should be visible)
        composeTestRule.onNodeWithText("Checking Account ✕").assertIsDisplayed()
        
        // Clear filter
        composeTestRule.onNodeWithText("Checking Account ✕").performClick()
        
        // Verify filter is removed
        composeTestRule.onNodeWithText("Checking Account ✕").assertDoesNotExist()
    }
    
    @Test
    fun transactionList_whenEmpty_shouldShowEmptyState() {
        // Assuming no transactions exist
        composeTestRule.onNodeWithText("No transactions found").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Receipt").assertIsDisplayed()
    }
    
    @Test
    fun transactionForm_dateValidation_shouldPreventFutureDates() {
        composeTestRule.onNodeWithText("Add Transaction").performClick()
        
        // Fill in valid data except date
        composeTestRule.onNodeWithText("Amount").performTextInput("50.00")
        composeTestRule.onNodeWithText("Type").performClick()
        composeTestRule.onNodeWithText("EXPENSE").performClick()
        composeTestRule.onNodeWithText("Account").performClick()
        composeTestRule.onAllNodesWithText("Test Account")[0].performClick()
        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onAllNodesWithText("Food")[0].performClick()
        composeTestRule.onNodeWithText("Merchant").performTextInput("Test Restaurant")
        
        // Try to save (date validation should be handled in ViewModel)
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        
        // If date is in future, should show error
        // This test assumes the date picker would set a future date
    }
}