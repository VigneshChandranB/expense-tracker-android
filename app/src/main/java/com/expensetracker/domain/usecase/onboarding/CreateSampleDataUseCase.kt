package com.expensetracker.domain.usecase.onboarding

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case to create sample data for first-time users
 */
class CreateSampleDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun createSampleData(config: SampleDataConfig): Result<Unit> {
        return try {
            val accountIds = mutableListOf<Long>()
            
            // Create sample accounts
            if (config.createSampleAccounts) {
                accountIds.addAll(createSampleAccounts())
            }
            
            // Create default categories if they don't exist
            if (config.createSampleCategories) {
                createDefaultCategories()
            }
            
            // Create sample transactions
            if (config.createSampleTransactions && accountIds.isNotEmpty()) {
                createSampleTransactions(accountIds, config.numberOfSampleTransactions)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createSampleAccounts(): List<Long> {
        val accounts = listOf(
            Account(
                bankName = "HDFC Bank",
                accountType = AccountType.CHECKING,
                accountNumber = "****1234",
                nickname = "Primary Checking",
                currentBalance = BigDecimal("25000.00"),
                createdAt = LocalDateTime.now()
            ),
            Account(
                bankName = "SBI",
                accountType = AccountType.SAVINGS,
                accountNumber = "****5678",
                nickname = "Savings Account",
                currentBalance = BigDecimal("50000.00"),
                createdAt = LocalDateTime.now()
            ),
            Account(
                bankName = "ICICI Bank",
                accountType = AccountType.CREDIT_CARD,
                accountNumber = "****9012",
                nickname = "Credit Card",
                currentBalance = BigDecimal("-5000.00"),
                createdAt = LocalDateTime.now()
            )
        )
        
        return accounts.map { account ->
            accountRepository.insertAccount(account)
        }
    }
    
    private suspend fun createDefaultCategories() {
        val defaultCategories = listOf(
            Category(1, "Food & Dining", "restaurant", "#FF9800", true),
            Category(2, "Shopping", "shopping_cart", "#2196F3", true),
            Category(3, "Transportation", "directions_car", "#4CAF50", true),
            Category(4, "Bills & Utilities", "receipt", "#F44336", true),
            Category(5, "Entertainment", "movie", "#9C27B0", true),
            Category(6, "Healthcare", "local_hospital", "#E91E63", true),
            Category(7, "Investment", "trending_up", "#009688", true),
            Category(8, "Income", "attach_money", "#8BC34A", true),
            Category(9, "Transfer", "swap_horiz", "#607D8B", true),
            Category(10, "Other", "category", "#795548", true)
        )
        
        defaultCategories.forEach { category ->
            try {
                categoryRepository.insertCategory(category)
            } catch (e: Exception) {
                // Category might already exist, continue
            }
        }
    }
    
    private suspend fun createSampleTransactions(accountIds: List<Long>, count: Int) {
        val categories = categoryRepository.getAllCategories()
        val now = LocalDateTime.now()
        
        val sampleTransactions = listOf(
            // Recent transactions for demonstration
            Transaction(
                amount = BigDecimal("500.00"),
                type = TransactionType.EXPENSE,
                category = categories.find { it.name == "Food & Dining" } ?: categories.first(),
                merchant = "Swiggy",
                description = "Lunch order",
                date = now.minusDays(1),
                source = TransactionSource.MANUAL,
                accountId = accountIds[0]
            ),
            Transaction(
                amount = BigDecimal("2500.00"),
                type = TransactionType.EXPENSE,
                category = categories.find { it.name == "Shopping" } ?: categories.first(),
                merchant = "Amazon",
                description = "Electronics purchase",
                date = now.minusDays(2),
                source = TransactionSource.MANUAL,
                accountId = accountIds[0]
            ),
            Transaction(
                amount = BigDecimal("50000.00"),
                type = TransactionType.INCOME,
                category = categories.find { it.name == "Income" } ?: categories.first(),
                merchant = "Salary",
                description = "Monthly salary",
                date = now.minusDays(5),
                source = TransactionSource.MANUAL,
                accountId = accountIds[1]
            ),
            Transaction(
                amount = BigDecimal("1200.00"),
                type = TransactionType.EXPENSE,
                category = categories.find { it.name == "Bills & Utilities" } ?: categories.first(),
                merchant = "Electricity Board",
                description = "Monthly electricity bill",
                date = now.minusDays(7),
                source = TransactionSource.MANUAL,
                accountId = accountIds[0]
            ),
            Transaction(
                amount = BigDecimal("800.00"),
                type = TransactionType.EXPENSE,
                category = categories.find { it.name == "Transportation" } ?: categories.first(),
                merchant = "Uber",
                description = "Cab fare",
                date = now.minusDays(3),
                source = TransactionSource.MANUAL,
                accountId = accountIds[2]
            ),
            // Transfer between accounts
            Transaction(
                amount = BigDecimal("10000.00"),
                type = TransactionType.TRANSFER_OUT,
                category = categories.find { it.name == "Transfer" } ?: categories.first(),
                merchant = "Account Transfer",
                description = "Transfer to savings",
                date = now.minusDays(10),
                source = TransactionSource.MANUAL,
                accountId = accountIds[0],
                transferAccountId = accountIds[1]
            ),
            Transaction(
                amount = BigDecimal("10000.00"),
                type = TransactionType.TRANSFER_IN,
                category = categories.find { it.name == "Transfer" } ?: categories.first(),
                merchant = "Account Transfer",
                description = "Transfer from checking",
                date = now.minusDays(10),
                source = TransactionSource.MANUAL,
                accountId = accountIds[1],
                transferAccountId = accountIds[0]
            )
        )
        
        // Add more random transactions to reach the desired count
        val additionalTransactions = generateAdditionalSampleTransactions(
            accountIds, 
            categories, 
            count - sampleTransactions.size,
            now
        )
        
        (sampleTransactions + additionalTransactions).forEach { transaction ->
            transactionRepository.insertTransaction(transaction)
        }
    }
    
    private fun generateAdditionalSampleTransactions(
        accountIds: List<Long>,
        categories: List<Category>,
        count: Int,
        baseDate: LocalDateTime
    ): List<Transaction> {
        val merchants = listOf(
            "Zomato", "BigBasket", "Flipkart", "Myntra", "BookMyShow",
            "Ola", "Metro", "Petrol Pump", "Grocery Store", "Pharmacy",
            "Coffee Shop", "Restaurant", "Mall", "Online Store", "ATM"
        )
        
        return (1..count).map { i ->
            val randomCategory = categories.random()
            val randomMerchant = merchants.random()
            val randomAmount = BigDecimal((100..5000).random())
            val randomDays = (1..30).random()
            val randomAccountId = accountIds.random()
            
            Transaction(
                amount = randomAmount,
                type = if (randomCategory.name == "Income") TransactionType.INCOME else TransactionType.EXPENSE,
                category = randomCategory,
                merchant = randomMerchant,
                description = "Sample transaction $i",
                date = baseDate.minusDays(randomDays.toLong()),
                source = TransactionSource.MANUAL,
                accountId = randomAccountId
            )
        }
    }
}