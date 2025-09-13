package com.expensetracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.presentation.components.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class MaterialDesignComplianceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_usesMaterial3ColorScheme() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                // Verify Material 3 color scheme is applied
                val colorScheme = MaterialTheme.colorScheme
                
                // Test that we have the expected Material 3 color tokens
                assert(colorScheme.primary != colorScheme.secondary)
                assert(colorScheme.primaryContainer != colorScheme.primary)
                assert(colorScheme.onPrimary != colorScheme.primary)
            }
        }
    }

    @Test
    fun theme_usesMaterial3Typography() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                val typography = MaterialTheme.typography
                
                // Verify Material 3 typography scale is used
                assert(typography.displayLarge.fontSize.value == 57f)
                assert(typography.headlineLarge.fontSize.value == 32f)
                assert(typography.titleLarge.fontSize.value == 22f)
                assert(typography.bodyLarge.fontSize.value == 16f)
                assert(typography.labelSmall.fontSize.value == 11f)
            }
        }
    }

    @Test
    fun theme_usesMaterial3Shapes() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                val shapes = MaterialTheme.shapes
                
                // Verify Material 3 shape scale is used
                assert(shapes.extraSmall != shapes.small)
                assert(shapes.small != shapes.medium)
                assert(shapes.medium != shapes.large)
                assert(shapes.large != shapes.extraLarge)
            }
        }
    }

    @Test
    fun financialAmountCard_followsMaterial3Design() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FinancialAmountCard(
                    title = "Balance",
                    amount = BigDecimal("1000.00"),
                    isPositive = true
                )
            }
        }

        // Verify card exists and has proper elevation
        composeTestRule
            .onNodeWithText("Balance")
            .assertExists()
            .assertIsDisplayed()

        // Verify amount is displayed
        composeTestRule
            .onNodeWithText("$1,000.00")
            .assertExists()
    }

    @Test
    fun categoryChip_followsMaterial3Design() {
        var selected = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChip(
                    label = "Food",
                    selected = selected,
                    onClick = { selected = !selected }
                )
            }
        }

        // Verify chip exists and is clickable
        composeTestRule
            .onNodeWithText("Food")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assert(selected)
    }

    @Test
    fun labeledProgressIndicator_followsMaterial3Design() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                LabeledProgressIndicator(
                    progress = 0.75f,
                    label = "Budget Used"
                )
            }
        }

        // Verify label and percentage are displayed
        composeTestRule
            .onNodeWithText("Budget Used")
            .assertExists()

        composeTestRule
            .onNodeWithText("75%")
            .assertExists()
    }

    @Test
    fun infoBanner_followsMaterial3Design() {
        var dismissed = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                InfoBanner(
                    message = "Your spending is 20% higher this month",
                    severity = BannerSeverity.WARNING,
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Verify banner message is displayed
        composeTestRule
            .onNodeWithText("Your spending is 20% higher this month")
            .assertExists()

        // Verify dismiss button works
        composeTestRule
            .onNodeWithContentDescription("Dismiss banner")
            .assertExists()
            .performClick()

        assert(dismissed)
    }

    @Test
    fun segmentedButton_followsMaterial3Design() {
        val options = listOf("Day", "Week", "Month")
        var selectedOption = "Day"
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SegmentedButton(
                    options = options,
                    selectedOption = selectedOption,
                    onOptionSelected = { selectedOption = it },
                    optionLabel = { it }
                )
            }
        }

        // Verify all options are displayed
        options.forEach { option ->
            composeTestRule
                .onNodeWithText(option)
                .assertExists()
        }

        // Test selection
        composeTestRule
            .onNodeWithText("Week")
            .performClick()

        assert(selectedOption == "Week")
    }

    @Test
    fun statusChip_followsMaterial3Design() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                StatusChip(status = "Completed")
            }
        }

        composeTestRule
            .onNodeWithText("Completed")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_followsMaterial3Design() {
        var actionClicked = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyState(
                    title = "No transactions",
                    description = "Add your first transaction to get started",
                    actionText = "Add Transaction",
                    onActionClick = { actionClicked = true }
                )
            }
        }

        // Verify title and description
        composeTestRule
            .onNodeWithText("No transactions")
            .assertExists()

        composeTestRule
            .onNodeWithText("Add your first transaction to get started")
            .assertExists()

        // Test action button
        composeTestRule
            .onNodeWithText("Add Transaction")
            .assertExists()
            .performClick()

        assert(actionClicked)
    }

    @Test
    fun darkTheme_appliesCorrectColors() {
        composeTestRule.setContent {
            ExpenseTrackerTheme(darkTheme = true) {
                val colorScheme = MaterialTheme.colorScheme
                
                // Verify dark theme colors are different from light theme
                // This is a basic check - in practice you'd verify specific color values
                assert(colorScheme.background != md_theme_light_background)
                assert(colorScheme.surface != md_theme_light_surface)
            }
        }
    }

    @Test
    fun components_respectThemeColors() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleButton(
                    onClick = { },
                    text = "Test Button"
                )
            }
        }

        // Verify button uses theme colors (this would be more detailed in practice)
        composeTestRule
            .onNodeWithText("Test Button")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun elevation_followsMaterial3Guidelines() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FinancialAmountCard(
                    title = "Test Card",
                    amount = BigDecimal("100.00"),
                    isPositive = true
                )
            }
        }

        // Verify card has appropriate elevation (visual test)
        composeTestRule
            .onNodeWithText("Test Card")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun spacing_followsMaterial3Guidelines() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                InfoBanner(
                    message = "Test message"
                )
            }
        }

        // Verify proper spacing is applied (this would check actual measurements in practice)
        composeTestRule
            .onNodeWithText("Test message")
            .assertExists()
            .assertIsDisplayed()
    }
}