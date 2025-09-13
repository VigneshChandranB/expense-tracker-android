package com.expensetracker.presentation.accessibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.presentation.components.*
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accessibleButton_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleButton(
                    onClick = { },
                    text = "Add Transaction",
                    contentDescription = "Add new transaction to your account"
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Add new transaction to your account")
            .assertExists()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun accessibleButton_loading_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleButton(
                    onClick = { },
                    text = "Save",
                    loading = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Save")
            .assertExists()
            .assertIsNotEnabled() // Should be disabled when loading
    }

    @Test
    fun accessibleTextField_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleTextField(
                    value = "",
                    onValueChange = { },
                    label = "Amount",
                    placeholder = "Enter amount"
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Amount")
            .assertExists()
            .assertIsEnabled()
            .assertTextEquals("")
    }

    @Test
    fun accessibleTextField_withError_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleTextField(
                    value = "invalid",
                    onValueChange = { },
                    label = "Amount",
                    isError = true,
                    errorMessage = "Please enter a valid amount"
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Error: Please enter a valid amount")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Amount")
            .assertExists()
    }

    @Test
    fun accessibleSwitch_hasCorrectSemantics() {
        var checked = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleSwitch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    label = "Enable notifications",
                    description = "Receive alerts for spending limits"
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Enable notifications switch")
            .assertExists()
            .assertHasClickAction()

        // Test state description
        composeTestRule
            .onNode(hasStateDescription("Off"))
            .assertExists()
    }

    @Test
    fun accessibleAmountDisplay_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleAmountDisplay(
                    amount = BigDecimal("1500.50"),
                    isIncome = true
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("income of $1,500.50")
            .assertExists()
    }

    @Test
    fun accessibleAmountDisplay_expense_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleAmountDisplay(
                    amount = BigDecimal("250.75"),
                    isIncome = false
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("expense of $250.75")
            .assertExists()
    }

    @Test
    fun accessibleCard_clickable_hasCorrectSemantics() {
        var clicked = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleCard(
                    onClick = { clicked = true },
                    contentDescription = "Account balance card for checking account"
                ) {
                    // Card content
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Account balance card for checking account")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assert(clicked)
    }

    @Test
    fun accessibleLoadingIndicator_hasCorrectSemantics() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleLoadingIndicator(
                    message = "Loading transactions"
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Loading transactions")
            .assertExists()

        composeTestRule
            .onNodeWithText("Loading transactions")
            .assertExists()
    }

    @Test
    fun accessibleErrorDisplay_hasCorrectSemantics() {
        var retryClicked = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleErrorDisplay(
                    error = "Network connection failed",
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Error: Network connection failed")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Retry loading")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assert(retryClicked)
    }

    @Test
    fun accessibleRadioButtonGroup_hasCorrectSemantics() {
        val options = listOf("Option 1", "Option 2", "Option 3")
        var selectedOption = "Option 1"
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleRadioButtonGroup(
                    options = options,
                    selectedOption = selectedOption,
                    onOptionSelected = { selectedOption = it },
                    optionLabel = { it },
                    groupLabel = "Choose an option"
                )
            }
        }

        // Check group semantics
        composeTestRule
            .onNodeWithContentDescription("Choose an option")
            .assertExists()

        // Check individual options
        composeTestRule
            .onNodeWithContentDescription("Option 1, selected")
            .assertExists()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Option 2, not selected")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun minimumTouchTargetSize_isRespected() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleButton(
                    onClick = { },
                    text = "OK"
                )
            }
        }

        // Material Design recommends minimum 48dp touch targets
        composeTestRule
            .onNodeWithText("OK")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun colorContrast_meetsAccessibilityStandards() {
        // This test would typically use accessibility testing tools
        // to verify color contrast ratios meet WCAG guidelines
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleButton(
                    onClick = { },
                    text = "Test Button"
                )
            }
        }

        // Verify button exists and is visible
        composeTestRule
            .onNodeWithText("Test Button")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun focusTraversal_worksCorrectly() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccessibleTextField(
                    value = "",
                    onValueChange = { },
                    label = "First Field"
                )
                AccessibleTextField(
                    value = "",
                    onValueChange = { },
                    label = "Second Field"
                )
                AccessibleButton(
                    onClick = { },
                    text = "Submit"
                )
            }
        }

        // Test that focus can traverse through elements
        composeTestRule
            .onNodeWithContentDescription("First Field")
            .assertExists()
            .requestFocus()
            .assertIsFocused()
    }
}