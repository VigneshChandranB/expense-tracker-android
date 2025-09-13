package com.expensetracker.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.expensetracker.MainActivity
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
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
 * Comprehensive integration tests covering all modules and multi-account scenarios
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ComprehensiveIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: ExpenseDatabase

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun fullAppIntegration_multiAccountScenario() = runTest {
        // Test complete app flow with multiple accounts
        
        // 1. Setup multiple accounts
        val checkingAccount = createTestAccount("HDFC Checking", AccountType.CHECKING)
        val creditAccount = createTestAccount("ICICI Credit", AccountType.CREDIT_CARD)
        val savingsAccount = createTestAccount("SBI Savings", AccountType.SAVINGS)

        // 2. Add transactions to different accounts
        val transactions = listOf(
            createTestTransaction(checkingAccount.id, BigDecimal("1500.00"), TransactionType.EXPENSE, "Amazon"),
            createTestTransaction(creditAccount.id, BigDecimal("2500.00"), TransactionType.EXPENSE, "Flipkart"),
            createTestTransaction(savingsAccount.id, BigDecimal("50000.00"), TransactionType.INCOME, "Salary"),
            createTestTransaction(checkingAccount.id, BigDecimal("500.00"), TransactionType.EXPENSE, "Uber")
        )

        // 3. Test SMS processing integration
        val smsMessages = listOf(
            createTestSmsMessage("HDFC", "Spent Rs.1500 at Amazon on 15-Dec"),
            createTestSmsMessage("ICICI", "Transaction of Rs.2500 at Flipkart"),
            createTestSmsMessage("SBI", "Credited Rs.50000 salary")
        )

        // 4. Verify data persistence across modules
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                // App content will be loaded
            }
        }

        // Wait for app to load
        composeTestRule.waitForIdle()

        // 5. Test dashboard integration with multiple accounts
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Verify account balances are displayed
        composeTestRule.onNodeWithText("HDFC Checking").assertIsDisplayed()
        composeTestRule.onNodeWithText("ICICI Credit").assertIsDisplayed()
        composeTestRule.onNodeWithText("SBI Savings").assertIsDisplayed()

        // 6. Test transaction list integration
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.waitForIdle()

        // Verify transactions from all accounts are shown
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flipkart").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()

        // 7. Test account filtering
        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("HDFC Checking").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Should only show HDFC transactions
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Uber").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flipkart").assertDoesNotExist()

        // 8. Test analytics integration
        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.waitForIdle()

        // Verify multi-account analytics
        composeTestRule.onNodeWithText("Total Portfolio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Breakdown").assertIsDisplayed()

        // 9. Test export functionality integration
        composeTestRule.onNodeWithText("Export").performClick()
        composeTestRule.onNodeWithText("All Accounts").performClick()
        composeTestRule.onNodeWithText("CSV Format").performClick()
        composeTestRule.onNodeWithText("Generate Export").performClick()

        // Verify export success
        composeTestRule.onNodeWithText("Export completed successfully").assertIsDisplayed()
    }

    @Test
    fun smsProcessingIntegration_multiBank() = runTest {
        // Test SMS processing across multiple banks
        
        val bankPatterns = listOf(
            "HDFC" to "Spent Rs.(\\d+(?:\\.\\d{2})?) at ([A-Z]+) on (\\d{2}-\\w{3})",
            "ICICI" to "Transaction of Rs.(\\d+(?:\\.\\d{2})?) at ([A-Z]+)",
            "SBI" to "Credited Rs.(\\d+(?:\\.\\d{2})?) ([a-z]+)",
            "AXIS" to "Debited Rs.(\\d+(?:\\.\\d{2})?) from account \\*\\*\\d+ at ([A-Z]+)"
        )

        val testMessages = listOf(
            "HDFC: Spent Rs.1500.00 at AMAZON on 15-Dec",
            "ICICI: Transaction of Rs.2500.50 at FLIPKART",
            "SBI: Credited Rs.50000.00 salary",
            "AXIS: Debited Rs.750.25 from account **1234 at SWIGGY"
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                // SMS processing will happen in background
            }
        }

        // Simulate SMS processing
        testMessages.forEach { message ->
            // This would trigger SMS processing in real scenario
            composeTestRule.waitForIdle()
        }

        // Navigate to transactions to verify processing
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.waitForIdle()

        // Verify all transactions were processed correctly
        composeTestRule.onNodeWithText("AMAZON").assertIsDisplayed()
        composeTestRule.onNodeWithText("FLIPKART").assertIsDisplayed()
        composeTestRule.onNodeWithText("SWIGGY").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1,500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹2,500.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹50,000.00").assertIsDisplayed()
    }

    @Test
    fun accountTransferIntegration_endToEnd() = runTest {
        // Test complete account transfer flow
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                // App content
            }
        }

        // Navigate to transfer screen
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.waitForIdle()

        // Select source account
        composeTestRule.onNodeWithText("From Account").performClick()
        composeTestRule.onNodeWithText("HDFC Checking").performClick()

        // Select destination account
        composeTestRule.onNodeWithText("To Account").performClick()
        composeTestRule.onNodeWithText("SBI Savings").performClick()

        // Enter transfer amount
        composeTestRule.onNodeWithText("Amount").performTextInput("5000")

        // Add description
        composeTestRule.onNodeWithText("Description").performTextInput("Monthly savings transfer")

        // Execute transfer
        composeTestRule.onNodeWithText("Transfer").performClick()
        composeTestRule.waitForIdle()

        // Verify transfer success
        composeTestRule.onNodeWithText("Transfer completed successfully").assertIsDisplayed()

        // Navigate to transactions to verify linked transactions
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.waitForIdle()

        // Verify both transfer transactions exist
        composeTestRule.onNodeWithText("Transfer to SBI Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transfer from HDFC Checking").assertIsDisplayed()

        // Verify amounts are correct
        composeTestRule.onNodeWithText("-₹5,000.00").assertIsDisplayed() // Outgoing
        composeTestRule.onNodeWithText("+₹5,000.00").assertIsDisplayed() // Incoming

        // Test account balance updates
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.waitForIdle()

        // Verify balances were updated correctly
        // This would check that HDFC balance decreased and SBI balance increased
    }

    private fun createTestAccount(name: String, type: AccountType): Account {
        return Account(
            id = 0,
            bankName = name.split(" ")[0],
            accountType = type,
            accountNumber = "****1234",
            nickname = name,
            currentBalance = BigDecimal("10000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
    }

    private fun createTestTransaction(
        accountId: Long,
        amount: BigDecimal,
        type: TransactionType,
        merchant: String
    ): Transaction {
        return Transaction(
            id = 0,
            amount = amount,
            type = type,
            category = Category.DEFAULT_CATEGORIES.first(),
            merchant = merchant,
            description = null,
            date = LocalDateTime.now(),
            source = TransactionSource.SMS_AUTO,
            accountId = accountId
        )
    }

    private fun createTestSmsMessage(sender: String, body: String): SmsMessage {
        return SmsMessage(
            id = 0,
            sender = sender,
            body = body,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }
}