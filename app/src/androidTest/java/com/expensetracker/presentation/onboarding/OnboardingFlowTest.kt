package com.expensetracker.presentation.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysCorrectContent() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                WelcomeScreen(onGetStarted = {})
            }
        }

        composeTestRule.onNodeWithText("Expense Tracker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart financial management made simple").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        composeTestRule.onNodeWithText("Multi-Account Support").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatic SMS Parsing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy First").assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_getStartedButtonWorks() {
        var getStartedClicked = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                WelcomeScreen(onGetStarted = { getStartedClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Get Started").performClick()
        assert(getStartedClicked)
    }

    @Test
    fun privacyExplanationScreen_displaysPrivacyFeatures() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                PrivacyExplanationScreen(
                    onAccept = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Your Privacy Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Local Data Only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encrypted Storage").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS Processing").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Cloud Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("I Understand & Continue").assertIsDisplayed()
    }

    @Test
    fun smsPermissionSetupScreen_displaysCorrectContentWhenPermissionNotGranted() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SmsPermissionSetupScreen(
                    onGrantPermission = {},
                    onSkip = {},
                    onBack = {},
                    smsPermissionGranted = false
                )
            }
        }

        composeTestRule.onNodeWithText("SMS Permission Setup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant SMS Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatic Transaction Detection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Time").assertIsDisplayed()
        composeTestRule.onNodeWithText("Secure Processing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Full Control").assertIsDisplayed()
    }

    @Test
    fun smsPermissionSetupScreen_displaysCorrectContentWhenPermissionGranted() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SmsPermissionSetupScreen(
                    onGrantPermission = {},
                    onSkip = {},
                    onBack = {},
                    smsPermissionGranted = true
                )
            }
        }

        composeTestRule.onNodeWithText("SMS Permission Granted!").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS Permission Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun accountCreationScreen_displaysFormFields() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccountCreationScreen(
                    onCreateAccount = { _, _, _ -> },
                    onSkip = {},
                    onBack = {},
                    isLoading = false
                )
            }
        }

        composeTestRule.onNodeWithText("Add Your First Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bank Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Type").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Nickname (Optional)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for Now").assertIsDisplayed()
    }

    @Test
    fun accountCreationScreen_createAccountButtonDisabledWhenBankNameEmpty() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AccountCreationScreen(
                    onCreateAccount = { _, _, _ -> },
                    onSkip = {},
                    onBack = {},
                    isLoading = false
                )
            }
        }

        composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
    }

    @Test
    fun featureIntroductionScreen_displaysTutorialSteps() {
        val sampleSteps = listOf(
            TutorialStep(
                id = "dashboard",
                title = "Dashboard Overview",
                description = "View your account balances and spending insights.",
                targetFeature = "dashboard",
                iconResource = "dashboard"
            ),
            TutorialStep(
                id = "accounts",
                title = "Multi-Account Management",
                description = "Manage multiple bank accounts and credit cards.",
                targetFeature = "accounts",
                iconResource = "account_balance"
            )
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FeatureIntroductionScreen(
                    tutorialSteps = sampleSteps,
                    onComplete = {},
                    onSkip = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("App Features").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dashboard Overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun sampleDataSetupScreen_displaysPreviewCards() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SampleDataSetupScreen(
                    onCreateSampleData = {},
                    onSkip = {},
                    onBack = {},
                    isLoading = false
                )
            }
        }

        composeTestRule.onNodeWithText("Try with Sample Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 Sample Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("15+ Sample Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Default Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instant Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Sample Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start with Empty App").assertIsDisplayed()
    }

    @Test
    fun sampleDataSetupScreen_showsLoadingStateCorrectly() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                SampleDataSetupScreen(
                    onCreateSampleData = {},
                    onSkip = {},
                    onBack = {},
                    isLoading = true
                )
            }
        }

        composeTestRule.onNodeWithText("Creating Sample Data...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start with Empty App").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Back").assertIsNotEnabled()
    }

    @Test
    fun onboardingCompletionScreen_displaysSetupSummary() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                OnboardingCompletionScreen(
                    onGetStarted = {},
                    sampleDataCreated = true,
                    accountsCreated = 2,
                    smsPermissionGranted = true
                )
            }
        }

        composeTestRule.onNodeWithText("You're All Set!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Setup Summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to Dashboard").assertIsDisplayed()
    }

    @Test
    fun onboardingCompletionScreen_showsCorrectAccountStatus() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                OnboardingCompletionScreen(
                    onGetStarted = {},
                    sampleDataCreated = false,
                    accountsCreated = 2,
                    smsPermissionGranted = false
                )
            }
        }

        composeTestRule.onNodeWithText("2 accounts created").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manual transaction entry (can enable later)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starting with clean slate").assertIsDisplayed()
    }
}