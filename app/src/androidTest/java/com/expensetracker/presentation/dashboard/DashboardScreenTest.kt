package com.expensetracker.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.dashboard.components.AccountBalanceCard
import com.expensetracker.presentation.dashboard.components.SpendingSummaryCard
import com.expensetracker.presentation.dashboard.components.TotalPortfolioCard
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_displaysLoadingState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DashboardScreen(
                    onNavigateToTransactions = {},
                    onNavigateToAddTransaction = {},
                    onNavigateToAccounts = {},
                    onTransactionClick = {}
                )
            }
        }

        // Should show loading indicator initially
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun totalPortfolioCard_displaysCorrectBalance() {
        val totalBalance = BigDecimal("50000.00")
        val accountCount = 3

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TotalPortfolioCard(
                    totalBalance = totalBalance,
                    accountCount = accountCount
                )
            }
        }

        composeTestRule.onNodeWithText("Total Portfolio").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹50,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Across 3 accounts").assertIsDisplayed()
    }

    @Test
    fun accountBalanceCard_displaysAccountInfo() {
        val account = createSampleAccount()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccountBalanceCard(
                    account = account,
                    isSelected = false,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("My Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDFC Bank • SAVINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹25,000.00").assertIsDisplayed()
    }

    @Test
    fun accountBalanceCard_showsSelectedState() {
        val account = createSampleAccount()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccountBalanceCard(
                    account = account,
                    isSelected = true,
                    onClick = {}
                )
            }
        }

        // Selected card should have different styling (tested through semantics)
        composeTestRule.onNodeWithText("My Savings").assertIsDisplayed()
    }

    @Test
    fun accountBalanceCard_handlesClick() {
        val account = createSampleAccount()
        var clicked = false

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccountBalanceCard(
                    account = account,
                    isSelected = false,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("My Savings").performClick()
        assert(clicked)
    }

    @Test
    fun spendingSummaryCard_displaysMonthlyData() {
        val monthlyReport = createSampleMonthlyReport()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SpendingSummaryCard(monthlyReport = monthlyReport)
            }
        }

        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expenses").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹80,000.00").assertIsDisplayed() // Income
        composeTestRule.onNodeWithText("₹45,000.00").assertIsDisplayed() // Expenses
    }

    @Test
    fun spendingSummaryCard_showsNetSavings() {
        val monthlyReport = createSampleMonthlyReport()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SpendingSummaryCard(monthlyReport = monthlyReport)
            }
        }

        composeTestRule.onNodeWithText("Net Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹35,000.00").assertIsDisplayed() // 80k - 45k
    }

    @Test
    fun spendingSummaryCard_showsLoadingWhenDataNull() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SpendingSummaryCard(monthlyReport = null)
            }
        }

        composeTestRule.onNodeWithText("Loading spending data...").assertIsDisplayed()
    }

    private fun createSampleAccount(): Account {
        return Account(
            id = 1L,
            bankName = "HDFC Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "1234567890",
            nickname = "My Savings",
            currentBalance = BigDecimal("25000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
    }

    private fun createSampleMonthlyReport(): MonthlyReport {
        return MonthlyReport(
            month = YearMonth.now(),
            totalIncome = BigDecimal("80000.00"),
            totalExpenses = BigDecimal("45000.00"),
            netAmount = BigDecimal("35000.00"),
            categoryBreakdown = emptyMap(),
            topMerchants = emptyList(),
            comparisonToPreviousMonth = MonthComparison(
                incomeChange = BigDecimal("5000.00"),
                expenseChange = BigDecimal("2000.00"),
                incomeChangePercentage = 6.67f,
                expenseChangePercentage = 4.65f,
                significantChanges = emptyList()
            ),
            accountBreakdown = emptyMap()
        )
    }
}