package com.expensetracker.presentation.onboarding

import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import com.expensetracker.domain.model.TutorialStep

/**
 * UI state for onboarding screens
 */
data class OnboardingUiState(
    val onboardingState: OnboardingState = OnboardingState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canProceed: Boolean = true,
    val tutorialSteps: List<TutorialStep> = emptyList(),
    val currentTutorialIndex: Int = 0,
    val smsPermissionGranted: Boolean = false,
    val accountCreationInProgress: Boolean = false,
    val sampleDataCreationInProgress: Boolean = false
)

/**
 * UI events for onboarding
 */
sealed class OnboardingEvent {
    object NextStep : OnboardingEvent()
    object PreviousStep : OnboardingEvent()
    object SkipStep : OnboardingEvent()
    object RequestSmsPermission : OnboardingEvent()
    object SkipSmsPermission : OnboardingEvent()
    object CreateSampleAccount : OnboardingEvent()
    object SkipAccountCreation : OnboardingEvent()
    object CreateSampleData : OnboardingEvent()
    object SkipSampleData : OnboardingEvent()
    object CompleteTutorial : OnboardingEvent()
    object CompleteOnboarding : OnboardingEvent()
    data class NavigateToStep(val step: OnboardingStep) : OnboardingEvent()
}