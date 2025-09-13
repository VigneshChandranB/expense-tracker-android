package com.expensetracker.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.presentation.MainActivity
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun dashboardScreen_loadsAndDisplaysContent() {
        // Wait for the dashboard to load
        composeTestRule.waitForIdle()
        
        // Check that the dashboard title is displayed
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Check that the accounts section is displayed
        composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
        
        // Check that the "All Accounts" option is available
        composeTestRule.onNodeWithText("All Accounts").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_accountSwitching_works() {
        composeTestRule.waitForIdle()
        
        // Initially "All Accounts" should be selected
        composeTestRule.onNodeWithText("All Accounts").assertIsSelected()
        
        // If there are accounts, try switching to one
        // This test assumes there's at least one account in the test database
        composeTestRule.onAllNodesWithContentDescription("Account card")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify that the view has changed (specific assertions would depend on test data)
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_refreshButton_works() {
        composeTestRule.waitForIdle()
        
        // Find and click the refresh button
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the dashboard is still displayed (refresh completed)
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_quickActions_areDisplayed() {
        composeTestRule.waitForIdle()
        
        // Scroll to the bottom to find quick actions
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("View All Transactions").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_spendingSummary_isDisplayed() {
        composeTestRule.waitForIdle()
        
        // Check that spending summary section exists
        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
    }
}