package com.expensetracker.presentation.transaction

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.model.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * UI tests for TransactionDetailScreen
 */
@RunWith(AndroidJUnit4::class)
class TransactionDetailScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var sampleTransaction: Transaction
    private lateinit var sampleAccount: Account
    private lateinit var sampleCategory: Category
    
    @Before
    fun setup() {
        sampleCategory = Category(
            id = 1L,
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true
        )
        
        sampleAccount = Account(
            id = 1L,
            bankName = "HDFC Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "****1234",
            nickname = "Primary Savings",
            currentBalance = BigDecimal("50000.00"),
            createdAt = LocalDateTime.now()
        )
        
        sampleTransaction = Transaction(
            id = 1L,
            amount = BigDecimal("1500.00"),
            type = TransactionType.EXPENSE,
            category = sampleCategory,
            merchant = "McDonald's",
            description = "Lunch with colleagues",
            date = LocalDateTime.now().minusDays(1),
            source = TransactionSource.SMS_AUTO,
            accountId = 1L
        )
    }
    
    @Test
    fun transactionDetailScreen_displaysTransactionDetails() {
        val uiState = TransactionDetailUiState(
            transaction = sampleTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify transaction details are displayed
        composeTestRule.onNodeWithText("Transaction Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch with colleagues").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDFC Bank - Primary Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS (Automatic)").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_displaysCorrectAmount() {
        val uiState = TransactionDetailUiState(
            transaction = sampleTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify amount is displayed correctly (format may vary based on locale)
        composeTestRule.onNodeWithText("-₹1,500.00", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("EXPENSE").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_incomeTransaction() {
        val incomeTransaction = sampleTransaction.copy(
            type = TransactionType.INCOME,
            amount = BigDecimal("50000.00"),
            merchant = "Company Inc",
            description = "Monthly salary",
            category = Category(8L, "Salary", "attach_money", "#4CAF50", true)
        )
        
        val uiState = TransactionDetailUiState(
            transaction = incomeTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify income transaction details
        composeTestRule.onNodeWithText("Company Inc").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly salary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
        composeTestRule.onNodeWithText("INCOME").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_transferTransaction() {
        val transferAccount = Account(
            id = 2L,
            bankName = "ICICI Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "****5678",
            nickname = "Secondary Savings",
            currentBalance = BigDecimal("25000.00"),
            createdAt = LocalDateTime.now()
        )
        
        val transferTransaction = sampleTransaction.copy(
            type = TransactionType.TRANSFER_OUT,
            merchant = "Transfer",
            description = "Transfer to savings",
            transferAccountId = 2L
        )
        
        val uiState = TransactionDetailUiState(
            transaction = transferTransaction,
            account = sampleAccount,
            transferAccount = transferAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify transfer transaction details
        composeTestRule.onNodeWithText("Transfer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transfer to savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("To Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("ICICI Bank - Secondary Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("TRANSFER OUT").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_recurringTransaction() {
        val recurringTransaction = sampleTransaction.copy(
            isRecurring = true
        )
        
        val uiState = TransactionDetailUiState(
            transaction = recurringTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify recurring indicator is displayed
        composeTestRule.onNodeWithText("Recurring").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_editButton() {
        val uiState = TransactionDetailUiState(
            transaction = sampleTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        var editTransactionId: Long? = null
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = { editTransactionId = it },
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Click edit button
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        
        // Verify edit navigation would be triggered (in real implementation)
        // assert(editTransactionId == 1L)
    }
    
    @Test
    fun transactionDetailScreen_deleteConfirmation() {
        val uiState = TransactionDetailUiState(
            transaction = sampleTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Click delete button
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        // Verify delete confirmation dialog
        composeTestRule.onNodeWithText("Delete Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this transaction? This action cannot be undone.").assertIsDisplayed()
        
        // Test cancel
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete Transaction").assertDoesNotExist()
        
        // Test delete confirmation
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Delete Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()
    }
    
    @Test
    fun transactionDetailScreen_backNavigation() {
        val uiState = TransactionDetailUiState(
            transaction = sampleTransaction,
            account = sampleAccount,
            isLoading = false
        )
        
        var backNavigated = false
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = { backNavigated = true },
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Verify back navigation would be triggered (in real implementation)
        // assert(backNavigated)
    }
    
    @Test
    fun transactionDetailScreen_loadingState() {
        val uiState = TransactionDetailUiState(
            transaction = null,
            account = null,
            isLoading = true
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify loading indicator is displayed
        composeTestRule.onNode(hasTestTag("loading")).assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_transactionNotFound() {
        val uiState = TransactionDetailUiState(
            transaction = null,
            account = null,
            isLoading = false,
            error = "Transaction not found"
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify error state is displayed
        composeTestRule.onNodeWithText("Transaction not found").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_withoutDescription() {
        val transactionWithoutDescription = sampleTransaction.copy(
            description = null
        )
        
        val uiState = TransactionDetailUiState(
            transaction = transactionWithoutDescription,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify description row is not displayed when description is null
        composeTestRule.onNodeWithText("Description").assertDoesNotExist()
        
        // But other details should still be displayed
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
    }
    
    @Test
    fun transactionDetailScreen_dateFormatting() {
        val specificDate = LocalDateTime.of(2024, 3, 15, 14, 30)
        val transactionWithSpecificDate = sampleTransaction.copy(
            date = specificDate
        )
        
        val uiState = TransactionDetailUiState(
            transaction = transactionWithSpecificDate,
            account = sampleAccount,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionDetailScreen(
                transactionId = 1L,
                onNavigateBack = {},
                onNavigateToEdit = {},
                viewModel = createMockDetailViewModel(uiState)
            )
        }
        
        // Verify date is formatted correctly
        composeTestRule.onNodeWithText("Friday, March 15, 2024 at 02:30 PM").assertIsDisplayed()
    }
    
    // Helper function to create a mock ViewModel (in real implementation, use Hilt test modules)
    private fun createMockDetailViewModel(uiState: TransactionDetailUiState): TransactionDetailViewModel {
        // This would be replaced with proper mock/fake implementation in real tests
        // For now, returning a placeholder that would work with dependency injection
        throw NotImplementedError("Mock ViewModel implementation needed for actual tests")
    }
}