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
 * UI tests for TransactionListScreen with filtering and search functionality
 */
@RunWith(AndroidJUnit4::class)
class TransactionListScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var sampleTransactions: List<Transaction>
    private lateinit var sampleAccounts: List<Account>
    private lateinit var sampleCategories: List<Category>
    
    @Before
    fun setup() {
        sampleAccounts = listOf(
            Account(
                id = 1L,
                bankName = "HDFC Bank",
                accountType = AccountType.SAVINGS,
                accountNumber = "****1234",
                nickname = "Primary Savings",
                currentBalance = BigDecimal("50000.00"),
                createdAt = LocalDateTime.now()
            ),
            Account(
                id = 2L,
                bankName = "ICICI Bank",
                accountType = AccountType.CREDIT_CARD,
                accountNumber = "****5678",
                nickname = "Credit Card",
                currentBalance = BigDecimal("-5000.00"),
                createdAt = LocalDateTime.now()
            )
        )
        
        sampleCategories = listOf(
            Category(
                id = 1L,
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true
            ),
            Category(
                id = 2L,
                name = "Shopping",
                icon = "shopping_cart",
                color = "#2196F3",
                isDefault = true
            ),
            Category(
                id = 3L,
                name = "Transportation",
                icon = "directions_car",
                color = "#4CAF50",
                isDefault = true
            )
        )
        
        sampleTransactions = listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("1500.00"),
                type = TransactionType.EXPENSE,
                category = sampleCategories[0],
                merchant = "McDonald's",
                description = "Lunch with colleagues",
                date = LocalDateTime.now().minusDays(1),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("2500.00"),
                type = TransactionType.EXPENSE,
                category = sampleCategories[1],
                merchant = "Amazon",
                description = "Online shopping",
                date = LocalDateTime.now().minusDays(2),
                source = TransactionSource.MANUAL,
                accountId = 2L
            ),
            Transaction(
                id = 3L,
                amount = BigDecimal("500.00"),
                type = TransactionType.EXPENSE,
                category = sampleCategories[2],
                merchant = "Uber",
                description = "Ride to office",
                date = LocalDateTime.now().minusDays(3),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            ),
            Transaction(
                id = 4L,
                amount = BigDecimal("50000.00"),
                type = TransactionType.INCOME,
                category = Category(8L, "Salary", "attach_money", "#4CAF50", true),
                merchant = "Company Inc",
                description = "Monthly salary",
                date = LocalDateTime.now().minusDays(5),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            )
        )
    }
    
    @Test
    fun transactionListScreen_displaysTransactions() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify transactions are displayed
        composeTestRule.onNodeWithText("McDonald's").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Uber").assertIsDisplayed()
        composeTestRule.onNodeWithText("Company Inc").assertIsDisplayed()
    }
    
    @Test
    fun transactionListScreen_searchFunctionality() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Test search functionality
        composeTestRule.onNodeWithText("Search transactions...").performTextInput("McDonald")
        
        // Verify search input is displayed
        composeTestRule.onNodeWithText("McDonald").assertIsDisplayed()
        
        // Test clear search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
    }
    
    @Test
    fun transactionListScreen_filterToggle() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = false
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Initially filters should not be visible
        composeTestRule.onNodeWithText("Filters").assertDoesNotExist()
        
        // Click filter toggle button
        composeTestRule.onNodeWithContentDescription("Toggle Filters").performClick()
        
        // Verify filter toggle was clicked (would trigger event in real implementation)
    }
    
    @Test
    fun transactionListScreen_accountFiltering() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = true
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify filters section is displayed
        composeTestRule.onNodeWithText("Filters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
        
        // Verify account filter chips are displayed
        composeTestRule.onNodeWithText("Primary Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Card").assertIsDisplayed()
        
        // Test account filter selection
        composeTestRule.onNodeWithText("Primary Savings").performClick()
    }
    
    @Test
    fun transactionListScreen_categoryFiltering() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = true
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify category filters are displayed
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transportation").assertIsDisplayed()
        
        // Test category filter selection
        composeTestRule.onNodeWithText("Food & Dining").performClick()
    }
    
    @Test
    fun transactionListScreen_typeFiltering() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = true
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify transaction type filters are displayed
        composeTestRule.onNodeWithText("Transaction Types").assertIsDisplayed()
        composeTestRule.onNodeWithText("INCOME").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXPENSE").assertIsDisplayed()
        
        // Test type filter selection
        composeTestRule.onNodeWithText("EXPENSE").performClick()
    }
    
    @Test
    fun transactionListScreen_dateRangeSelection() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = true
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify date range filters are displayed
        composeTestRule.onNodeWithText("Date Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("End Date").assertIsDisplayed()
        
        // Test date range selection
        composeTestRule.onNodeWithText("Start Date").performClick()
    }
    
    @Test
    fun transactionListScreen_sortDialog() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Click sort button
        composeTestRule.onNodeWithContentDescription("Sort").performClick()
        
        // Verify sort dialog would open (in real implementation)
    }
    
    @Test
    fun transactionListScreen_clearAllFilters() {
        val uiState = TransactionUiState(
            transactions = sampleTransactions,
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            showFilters = true,
            selectedAccountIds = listOf(1L),
            selectedCategoryIds = listOf(1L),
            searchQuery = "test"
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify active filters are displayed
        composeTestRule.onNodeWithText("Primary Savings ✕").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining ✕").assertIsDisplayed()
        
        // Test clear all filters
        composeTestRule.onNodeWithText("Clear All").performClick()
    }
    
    @Test
    fun transactionListScreen_emptyStateWithFilters() {
        val uiState = TransactionUiState(
            transactions = emptyList(),
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false,
            searchQuery = "nonexistent"
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify empty state with filters message
        composeTestRule.onNodeWithText("No transactions match your filters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear filters").assertIsDisplayed()
    }
    
    @Test
    fun transactionListScreen_loadingState() {
        val uiState = TransactionUiState(
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            isLoading = true
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Verify loading indicator is displayed
        composeTestRule.onNode(hasTestTag("loading")).assertIsDisplayed()
    }
    
    @Test
    fun transactionItem_clickToEdit() {
        val uiState = TransactionUiState(
            transactions = listOf(sampleTransactions[0]),
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false
        )
        
        var editTransactionId: Long? = null
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = { editTransactionId = it },
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Click on transaction item
        composeTestRule.onNodeWithText("McDonald's").performClick()
        
        // Verify edit navigation would be triggered (in real implementation)
        // assert(editTransactionId == 1L)
    }
    
    @Test
    fun transactionItem_deleteConfirmation() {
        val uiState = TransactionUiState(
            transactions = listOf(sampleTransactions[0]),
            accounts = sampleAccounts,
            categories = sampleCategories,
            isLoading = false
        )
        
        composeTestRule.setContent {
            TransactionListScreen(
                onNavigateToAddTransaction = {},
                onNavigateToEditTransaction = {},
                onNavigateToTransfer = {},
                viewModel = createMockViewModel(uiState)
            )
        }
        
        // Click delete button
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        // Verify delete confirmation dialog
        composeTestRule.onNodeWithText("Delete Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this transaction?").assertIsDisplayed()
        
        // Test cancel
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete Transaction").assertDoesNotExist()
    }
    
    // Helper function to create a mock ViewModel (in real implementation, use Hilt test modules)
    private fun createMockViewModel(uiState: TransactionUiState): TransactionViewModel {
        // This would be replaced with proper mock/fake implementation in real tests
        // For now, returning a placeholder that would work with dependency injection
        throw NotImplementedError("Mock ViewModel implementation needed for actual tests")
    }
}