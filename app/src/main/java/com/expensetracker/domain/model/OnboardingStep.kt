package com.expensetracker.domain.model

/**
 * Represents different steps in the onboarding flow
 */
enum class OnboardingStep {
    WELCOME,
    PRIVACY_EXPLANATION,
    SMS_PERMISSION_SETUP,
    ACCOUNT_CREATION,
    FEATURE_INTRODUCTION,
    SAMPLE_DATA_SETUP,
    COMPLETION
}

/**
 * Onboarding state and progress tracking
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val completedSteps: Set<OnboardingStep> = emptySet(),
    val isCompleted: Boolean = false,
    val hasSkippedSmsPermission: Boolean = false,
    val createdAccounts: List<Long> = emptyList(),
    val sampleDataCreated: Boolean = false
)

/**
 * Sample data configuration for first-time users
 */
data class SampleDataConfig(
    val createSampleAccounts: Boolean = true,
    val createSampleTransactions: Boolean = true,
    val createSampleCategories: Boolean = true,
    val numberOfSampleTransactions: Int = 20,
    val sampleAccountTypes: List<AccountType> = listOf(
        AccountType.CHECKING,
        AccountType.SAVINGS,
        AccountType.CREDIT_CARD
    )
)

/**
 * Tutorial step for feature introduction
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val targetFeature: String,
    val iconResource: String,
    val isCompleted: Boolean = false
)