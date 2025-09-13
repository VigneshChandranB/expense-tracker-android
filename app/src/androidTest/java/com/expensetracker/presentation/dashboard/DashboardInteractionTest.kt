package com.expensetracker.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.dashboard.components.CategoryBreakdownChart
import com.expensetracker.presentation.dashboard.components.RecentTransactionsList
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class DashboardInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun categoryBreakdownChart_displaysCategories() {
        val categoryBreakdown = createSampleCategoryBreakdown()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryBreakdownChart(categoryBreakdown = categoryBreakdown)
            }
        }

        composeTestRule.onNodeWithText("Category Breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transportation").assertIsDisplayed()
    }

    @Test
    fun categoryBreakdownChart_showsEmptyState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryBreakdownChart(categoryBreakdown = null)
            }
        }

        composeTestRule.onNodeWithText("No spending data available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start adding transactions to see breakdown").assertIsDisplayed()
    }

    @Test
    fun recentTransactionsList_displaysTransactions() {
        val transactions = createSampleTransactions()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = transactions,
                    onTransactionClick = {},
                    onViewAllClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Uber").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starbucks").assertIsDisplayed()
    }

    @Test
    fun recentTransactionsList_handlesTransactionClick() {
        val transactions = createSampleTransactions()
        var clickedTransaction: Transaction? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = transactions,
                    onTransactionClick = { clickedTransaction = it },
                    onViewAllClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Amazon").performClick()
        assert(clickedTransaction != null)
        assert(clickedTransaction?.merchant == "Amazon")
    }

    @Test
    fun recentTransactionsList_handlesViewAllClick() {
        val transactions = createSampleTransactions()
        var viewAllClicked = false

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = transactions,
                    onTransactionClick = {},
                    onViewAllClick = { viewAllClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("View All").performClick()
        assert(viewAllClicked)
    }

    @Test
    fun recentTransactionsList_showsEmptyState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = emptyList(),
                    onTransactionClick = {},
                    onViewAllClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No recent transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your transactions will appear here").assertIsDisplayed()
    }

    @Test
    fun recentTransactionsList_displaysTransactionAmounts() {
        val transactions = createSampleTransactions()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = transactions,
                    onTransactionClick = {},
                    onViewAllClick = {}
                )
            }
        }

        // Check for expense amounts (negative)
        composeTestRule.onNodeWithText("-₹2,500.00").assertIsDisplayed() // Amazon
        composeTestRule.onNodeWithText("-₹450.00").assertIsDisplayed()   // Uber
        composeTestRule.onNodeWithText("-₹350.00").assertIsDisplayed()   // Starbucks
    }

    @Test
    fun recentTransactionsList_displaysIncomeTransactions() {
        val transactions = listOf(
            createSampleTransaction(
                id = 1L,
                merchant = "Salary",
                amount = BigDecimal("75000.00"),
                type = TransactionType.INCOME,
                category = createSampleCategory(name = "Income")
            )
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                RecentTransactionsList(
                    transactions = transactions,
                    onTransactionClick = {},
                    onViewAllClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
        composeTestRule.onNodeWithText("+₹75,000.00").assertIsDisplayed() // Income (positive)
    }

    private fun createSampleCategoryBreakdown(): CategoryBreakdown {
        val categories = listOf(
            CategorySpending(
                category = createSampleCategory(name = "Food & Dining"),
                amount = BigDecimal("15000.00"),
                percentage = 40f,
                transactionCount = 25,
                averageTransactionAmount = BigDecimal("600.00"),
                trend = SpendingTrend(TrendDirection.INCREASING, 5.2f, true)
            ),
            CategorySpending(
                category = createSampleCategory(name = "Shopping"),
                amount = BigDecimal("10000.00"),
                percentage = 27f,
                transactionCount = 15,
                averageTransactionAmount = BigDecimal("666.67"),
                trend = SpendingTrend(TrendDirection.STABLE, 1.1f, false)
            ),
            CategorySpending(
                category = createSampleCategory(name = "Transportation"),
                amount = BigDecimal("7500.00"),
                percentage = 20f,
                transactionCount = 30,
                averageTransactionAmount = BigDecimal("250.00"),
                trend = SpendingTrend(TrendDirection.DECREASING, -3.5f, true)
            )
        )

        return CategoryBreakdown(
            period = DateRange.currentMonth(),
            totalAmount = BigDecimal("37500.00"),
            categorySpending = categories,
            topCategories = categories,
            unusedCategories = emptyList()
        )
    }

    private fun createSampleTransactions(): List<Transaction> {
        return listOf(
            createSampleTransaction(
                id = 1L,
                merchant = "Amazon",
                amount = BigDecimal("2500.00"),
                category = createSampleCategory(name = "Shopping")
            ),
            createSampleTransaction(
                id = 2L,
                merchant = "Uber",
                amount = BigDecimal("450.00"),
                category = createSampleCategory(name = "Transportation")
            ),
            createSampleTransaction(
                id = 3L,
                merchant = "Starbucks",
                amount = BigDecimal("350.00"),
                category = createSampleCategory(name = "Food & Dining")
            )
        )
    }

    private fun createSampleTransaction(
        id: Long,
        merchant: String,
        amount: BigDecimal,
        type: TransactionType = TransactionType.EXPENSE,
        category: Category
    ): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            description = null,
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1L,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false
        )
    }

    private fun createSampleCategory(
        id: Long = 1L,
        name: String,
        icon: String = "category",
        color: String = "#FF9800"
    ): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isDefault = true,
            parentCategory = null
        )
    }
}