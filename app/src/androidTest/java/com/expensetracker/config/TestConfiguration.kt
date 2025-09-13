package com.expensetracker.config

import androidx.test.platform.app.InstrumentationRegistry
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.domain.model.*
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Test configuration and utilities for integration tests
 */
object TestConfiguration {

    /**
     * Sets up test data for integration tests
     */
    fun setupTestData(database: ExpenseDatabase) = runBlocking {
        // Clear existing data
        database.clearAllTables()

        // Create test accounts
        val accounts = listOf(
            createTestAccount("HDFC Bank", AccountType.CHECKING, "Primary Checking", "10000.00"),
            createTestAccount("ICICI Bank", AccountType.CREDIT_CARD, "Credit Card", "0.00"),
            createTestAccount("SBI Bank", AccountType.SAVINGS, "Savings Account", "25000.00"),
            createTestAccount("AXIS Bank", AccountType.CHECKING, "Secondary Checking", "5000.00")
        )

        accounts.forEach { account ->
            database.accountDao().insertAccount(account.toEntity())
        }

        // Create test categories (if not using defaults)
        val customCategories = listOf(
            createTestCategory("Gym Membership", "fitness_center", "#4CAF50"),
            createTestCategory("Pet Care", "pets", "#FF9800"),
            createTestCategory("Education", "school", "#2196F3")
        )

        customCategories.forEach { category ->
            database.categoryDao().insertCategory(category.toEntity())
        }

        // Create test transactions
        val transactions = listOf(
            createTestTransaction(1L, "1500.00", TransactionType.EXPENSE, "Food & Dining", "McDonald's"),
            createTestTransaction(1L, "2500.00", TransactionType.EXPENSE, "Shopping", "Amazon"),
            createTestTransaction(2L, "3000.00", TransactionType.EXPENSE, "Entertainment", "Movie Theater"),
            createTestTransaction(3L, "50000.00", TransactionType.INCOME, "Income", "Salary"),
            createTestTransaction(1L, "800.00", TransactionType.EXPENSE, "Transportation", "Uber"),
            createTestTransaction(3L, "1200.00", TransactionType.EXPENSE, "Bills", "Electricity")
        )

        transactions.forEach { transaction ->
            database.transactionDao().insertTransaction(transaction.toEntity())
        }

        // Create test SMS patterns
        val smsPatterns = listOf(
            createSmsPattern("HDFC", "HDFC.*", "Rs\\.(\\d+(?:\\.\\d{2})?)", "at ([A-Z]+)", "on (\\d{2}-\\w{3})"),
            createSmsPattern("ICICI", "ICICI.*", "Rs\\.(\\d+(?:\\.\\d{2})?)", "at ([A-Z]+)", ""),
            createSmsPattern("SBI", "SBI.*", "Rs\\.(\\d+(?:\\.\\d{2})?)", "([a-z]+)", ""),
            createSmsPattern("AXIS", "AXIS.*", "Rs\\.(\\d+(?:\\.\\d{2})?)", "at ([A-Z]+)", "")
        )

        smsPatterns.forEach { pattern ->
            database.smsPatternDao().insertPattern(pattern)
        }
    }

    /**
     * Creates test account with specified parameters
     */
    private fun createTestAccount(
        bankName: String,
        type: AccountType,
        nickname: String,
        balance: String
    ): Account {
        return Account(
            id = 0,
            bankName = bankName,
            accountType = type,
            accountNumber = "****${(1000..9999).random()}",
            nickname = nickname,
            currentBalance = BigDecimal(balance),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
    }

    /**
     * Creates test category with specified parameters
     */
    private fun createTestCategory(
        name: String,
        icon: String,
        color: String
    ): Category {
        return Category(
            id = 0,
            name = name,
            icon = icon,
            color = android.graphics.Color.parseColor(color),
            isDefault = false,
            parentCategory = null
        )
    }

    /**
     * Creates test transaction with specified parameters
     */
    private fun createTestTransaction(
        accountId: Long,
        amount: String,
        type: TransactionType,
        categoryName: String,
        merchant: String
    ): Transaction {
        val category = Category.DEFAULT_CATEGORIES.find { it.name == categoryName }
            ?: Category.DEFAULT_CATEGORIES.first()

        return Transaction(
            id = 0,
            amount = BigDecimal(amount),
            type = type,
            category = category,
            merchant = merchant,
            description = null,
            date = LocalDateTime.now().minusDays((0..30).random().toLong()),
            source = TransactionSource.MANUAL,
            accountId = accountId
        )
    }

    /**
     * Creates SMS pattern for testing
     */
    private fun createSmsPattern(
        bankName: String,
        senderPattern: String,
        amountPattern: String,
        merchantPattern: String,
        datePattern: String
    ): SmsPatternEntity {
        return SmsPatternEntity(
            id = 0,
            bankName = bankName,
            senderPattern = senderPattern,
            amountPattern = amountPattern,
            merchantPattern = merchantPattern,
            datePattern = datePattern,
            typePattern = "spent|debited",
            isActive = true
        )
    }

    /**
     * Test data constants
     */
    object TestData {
        const val TEST_ACCOUNT_BALANCE = "10000.00"
        const val TEST_TRANSACTION_AMOUNT = "1500.00"
        const val TEST_TRANSFER_AMOUNT = "2000.00"
        
        val TEST_SMS_MESSAGES = listOf(
            "HDFC: Spent Rs.1500.00 at AMAZON on 15-Dec",
            "ICICI: Transaction of Rs.2500.00 at FLIPKART",
            "SBI: Credited Rs.50000.00 salary",
            "AXIS: Debited Rs.750.25 from account **1234 at SWIGGY"
        )

        val EXPECTED_MERCHANTS = listOf("AMAZON", "FLIPKART", "SWIGGY")
        val EXPECTED_AMOUNTS = listOf("₹1,500.00", "₹2,500.00", "₹50,000.00", "₹750.25")
    }

    /**
     * Test utilities
     */
    object TestUtils {
        fun waitForAnimation() {
            Thread.sleep(500) // Wait for animations to complete
        }

        fun getTestContext() = InstrumentationRegistry.getInstrumentation().targetContext

        fun isRunningOnEmulator(): Boolean {
            return android.os.Build.FINGERPRINT.contains("generic") ||
                    android.os.Build.MODEL.contains("Emulator") ||
                    android.os.Build.MODEL.contains("Android SDK")
        }

        fun skipIfNotEmulator() {
            org.junit.Assume.assumeTrue("Test requires emulator", isRunningOnEmulator())
        }
    }
}