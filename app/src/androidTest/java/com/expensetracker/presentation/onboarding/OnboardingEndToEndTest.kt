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
class OnboardingEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completeOnboardingFlow_withSampleData() {
        var currentStep = OnboardingStep.WELCOME
        var smsPermissionGranted = false
        var sampleDataCreated = false
        var accountsCreated = 0
        var onboardingCompleted = false

        val tutorialSteps = listOf(
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
                when (currentStep) {
                    OnboardingStep.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = { currentStep = OnboardingStep.PRIVACY_EXPLANATION }
                        )
                    }
                    OnboardingStep.PRIVACY_EXPLANATION -> {
                        PrivacyExplanationScreen(
                            onAccept = { currentStep = OnboardingStep.SMS_PERMISSION_SETUP },
                            onBack = { currentStep = OnboardingStep.WELCOME }
                        )
                    }
                    OnboardingStep.SMS_PERMISSION_SETUP -> {
                        SmsPermissionSetupScreen(
                            onGrantPermission = {
                                smsPermissionGranted = true
                                currentStep = OnboardingStep.ACCOUNT_CREATION
                            },
                            onSkip = { currentStep = OnboardingStep.ACCOUNT_CREATION },
                            onBack = { currentStep = OnboardingStep.PRIVACY_EXPLANATION },
                            smsPermissionGranted = smsPermissionGranted
                        )
                    }
                    OnboardingStep.ACCOUNT_CREATION -> {
                        AccountCreationScreen(
                            onCreateAccount = { _, _, _ ->
                                accountsCreated = 1
                                currentStep = OnboardingStep.FEATURE_INTRODUCTION
                            },
                            onSkip = { currentStep = OnboardingStep.FEATURE_INTRODUCTION },
                            onBack = { currentStep = OnboardingStep.SMS_PERMISSION_SETUP }
                        )
                    }
                    OnboardingStep.FEATURE_INTRODUCTION -> {
                        FeatureIntroductionScreen(
                            tutorialSteps = tutorialSteps,
                            onComplete = { currentStep = OnboardingStep.SAMPLE_DATA_SETUP },
                            onSkip = { currentStep = OnboardingStep.SAMPLE_DATA_SETUP },
                            onBack = { currentStep = OnboardingStep.ACCOUNT_CREATION }
                        )
                    }
                    OnboardingStep.SAMPLE_DATA_SETUP -> {
                        SampleDataSetupScreen(
                            onCreateSampleData = {
                                sampleDataCreated = true
                                currentStep = OnboardingStep.COMPLETION
                            },
                            onSkip = { currentStep = OnboardingStep.COMPLETION },
                            onBack = { currentStep = OnboardingStep.FEATURE_INTRODUCTION }
                        )
                    }
                    OnboardingStep.COMPLETION -> {
                        OnboardingCompletionScreen(
                            onGetStarted = { onboardingCompleted = true },
                            sampleDataCreated = sampleDataCreated,
                            accountsCreated = accountsCreated,
                            smsPermissionGranted = smsPermissionGranted
                        )
                    }
                }
            }
        }

        // Step 1: Welcome Screen
        composeTestRule.onNodeWithText("Expense Tracker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").performClick()

        // Step 2: Privacy Explanation
        composeTestRule.onNodeWithText("Your Privacy Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("I Understand & Continue").performClick()

        // Step 3: SMS Permission Setup
        composeTestRule.onNodeWithText("SMS Permission Setup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant SMS Permission").performClick()

        // Step 4: Account Creation
        composeTestRule.onNodeWithText("Add Your First Account").assertIsDisplayed()
        
        // Fill in bank name
        composeTestRule.onNodeWithText("Bank Name").performTextInput("HDFC Bank")
        
        // Create account
        composeTestRule.onNodeWithText("Create Account").performClick()

        // Step 5: Feature Introduction
        composeTestRule.onNodeWithText("App Features").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dashboard Overview").assertIsDisplayed()
        
        // Navigate through tutorial steps
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Multi-Account Management").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").performClick()

        // Step 6: Sample Data Setup
        composeTestRule.onNodeWithText("Try with Sample Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Sample Data").performClick()

        // Step 7: Completion
        composeTestRule.onNodeWithText("You're All Set!").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 account created").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatic transaction detection enabled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample transactions and categories created").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to Dashboard").performClick()

        // Verify onboarding completed
        assert(onboardingCompleted)
        assert(smsPermissionGranted)
        assert(sampleDataCreated)
        assert(accountsCreated == 1)
    }

    @Test
    fun completeOnboardingFlow_skipAllOptionalSteps() {
        var currentStep = OnboardingStep.WELCOME
        var onboardingCompleted = false

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                when (currentStep) {
                    OnboardingStep.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = { currentStep = OnboardingStep.PRIVACY_EXPLANATION }
                        )
                    }
                    OnboardingStep.PRIVACY_EXPLANATION -> {
                        PrivacyExplanationScreen(
                            onAccept = { currentStep = OnboardingStep.SMS_PERMISSION_SETUP },
                            onBack = { currentStep = OnboardingStep.WELCOME }
                        )
                    }
                    OnboardingStep.SMS_PERMISSION_SETUP -> {
                        SmsPermissionSetupScreen(
                            onGrantPermission = { currentStep = OnboardingStep.ACCOUNT_CREATION },
                            onSkip = { currentStep = OnboardingStep.ACCOUNT_CREATION },
                            onBack = { currentStep = OnboardingStep.PRIVACY_EXPLANATION },
                            smsPermissionGranted = false
                        )
                    }
                    OnboardingStep.ACCOUNT_CREATION -> {
                        AccountCreationScreen(
                            onCreateAccount = { _, _, _ -> currentStep = OnboardingStep.FEATURE_INTRODUCTION },
                            onSkip = { currentStep = OnboardingStep.FEATURE_INTRODUCTION },
                            onBack = { currentStep = OnboardingStep.SMS_PERMISSION_SETUP }
                        )
                    }
                    OnboardingStep.FEATURE_INTRODUCTION -> {
                        FeatureIntroductionScreen(
                            tutorialSteps = emptyList(),
                            onComplete = { currentStep = OnboardingStep.SAMPLE_DATA_SETUP },
                            onSkip = { currentStep = OnboardingStep.SAMPLE_DATA_SETUP },
                            onBack = { currentStep = OnboardingStep.ACCOUNT_CREATION }
                        )
                    }
                    OnboardingStep.SAMPLE_DATA_SETUP -> {
                        SampleDataSetupScreen(
                            onCreateSampleData = { currentStep = OnboardingStep.COMPLETION },
                            onSkip = { currentStep = OnboardingStep.COMPLETION },
                            onBack = { currentStep = OnboardingStep.FEATURE_INTRODUCTION }
                        )
                    }
                    OnboardingStep.COMPLETION -> {
                        OnboardingCompletionScreen(
                            onGetStarted = { onboardingCompleted = true },
                            sampleDataCreated = false,
                            accountsCreated = 0,
                            smsPermissionGranted = false
                        )
                    }
                }
            }
        }

        // Navigate through all steps by skipping optional ones
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.onNodeWithText("I Understand & Continue").performClick()
        composeTestRule.onNodeWithText("Skip for Now").performClick()
        composeTestRule.onNodeWithText("Skip for Now").performClick()
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Start with Empty App").performClick()
        
        // Verify completion screen shows minimal setup
        composeTestRule.onNodeWithText("Ready to add your first account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manual transaction entry (can enable later)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starting with clean slate").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Go to Dashboard").performClick()
        assert(onboardingCompleted)
    }

    @Test
    fun onboardingFlow_backNavigation() {
        var currentStep = OnboardingStep.ACCOUNT_CREATION

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                when (currentStep) {
                    OnboardingStep.ACCOUNT_CREATION -> {
                        AccountCreationScreen(
                            onCreateAccount = { _, _, _ -> },
                            onSkip = { },
                            onBack = { currentStep = OnboardingStep.SMS_PERMISSION_SETUP }
                        )
                    }
                    OnboardingStep.SMS_PERMISSION_SETUP -> {
                        SmsPermissionSetupScreen(
                            onGrantPermission = { },
                            onSkip = { },
                            onBack = { currentStep = OnboardingStep.PRIVACY_EXPLANATION },
                            smsPermissionGranted = false
                        )
                    }
                    OnboardingStep.PRIVACY_EXPLANATION -> {
                        PrivacyExplanationScreen(
                            onAccept = { },
                            onBack = { currentStep = OnboardingStep.WELCOME }
                        )
                    }
                    OnboardingStep.WELCOME -> {
                        WelcomeScreen(onGetStarted = { })
                    }
                    else -> {
                        // Handle other steps if needed
                    }
                }
            }
        }

        // Test back navigation from Account Creation to SMS Permission
        composeTestRule.onNodeWithText("Add Your First Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()

        // Should be on SMS Permission screen
        composeTestRule.onNodeWithText("SMS Permission Setup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()

        // Should be on Privacy Explanation screen
        composeTestRule.onNodeWithText("Your Privacy Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()

        // Should be on Welcome screen
        composeTestRule.onNodeWithText("Expense Tracker").assertIsDisplayed()
    }
}