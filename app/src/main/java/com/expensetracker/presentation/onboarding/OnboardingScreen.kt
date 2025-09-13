package com.expensetracker.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.model.OnboardingStep
import com.expensetracker.presentation.components.ErrorDialog

/**
 * Main onboarding screen that coordinates all onboarding steps
 */
@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle navigation events
    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is OnboardingNavigationEvent.RequestSmsPermission -> {
                    onRequestSmsPermission()
                }
                is OnboardingNavigationEvent.NavigateToDashboard -> {
                    onNavigateToDashboard()
                }
                is OnboardingNavigationEvent.NavigateToAccountCreation -> {
                    // Handle account creation navigation if needed
                }
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState.onboardingState.currentStep) {
            OnboardingStep.WELCOME -> {
                WelcomeScreen(
                    onGetStarted = {
                        viewModel.onEvent(OnboardingEvent.NextStep)
                    }
                )
            }
            
            OnboardingStep.PRIVACY_EXPLANATION -> {
                PrivacyExplanationScreen(
                    onAccept = {
                        viewModel.onEvent(OnboardingEvent.NextStep)
                    },
                    onBack = {
                        viewModel.onEvent(OnboardingEvent.PreviousStep)
                    }
                )
            }
            
            OnboardingStep.SMS_PERMISSION_SETUP -> {
                SmsPermissionSetupScreen(
                    onGrantPermission = {
                        if (uiState.smsPermissionGranted) {
                            viewModel.onEvent(OnboardingEvent.NextStep)
                        } else {
                            viewModel.onEvent(OnboardingEvent.RequestSmsPermission)
                        }
                    },
                    onSkip = {
                        viewModel.onEvent(OnboardingEvent.SkipSmsPermission)
                    },
                    onBack = {
                        viewModel.onEvent(OnboardingEvent.PreviousStep)
                    },
                    smsPermissionGranted = uiState.smsPermissionGranted
                )
            }
            
            OnboardingStep.ACCOUNT_CREATION -> {
                AccountCreationScreen(
                    onCreateAccount = { bankName, accountType, nickname ->
                        // Handle account creation
                        viewModel.onEvent(OnboardingEvent.CreateSampleAccount)
                    },
                    onSkip = {
                        viewModel.onEvent(OnboardingEvent.SkipAccountCreation)
                    },
                    onBack = {
                        viewModel.onEvent(OnboardingEvent.PreviousStep)
                    },
                    isLoading = uiState.accountCreationInProgress
                )
            }
            
            OnboardingStep.FEATURE_INTRODUCTION -> {
                FeatureIntroductionScreen(
                    tutorialSteps = uiState.tutorialSteps,
                    onComplete = {
                        viewModel.onEvent(OnboardingEvent.CompleteTutorial)
                    },
                    onSkip = {
                        viewModel.onEvent(OnboardingEvent.CompleteTutorial)
                    },
                    onBack = {
                        viewModel.onEvent(OnboardingEvent.PreviousStep)
                    }
                )
            }
            
            OnboardingStep.SAMPLE_DATA_SETUP -> {
                SampleDataSetupScreen(
                    onCreateSampleData = {
                        viewModel.onEvent(OnboardingEvent.CreateSampleData)
                    },
                    onSkip = {
                        viewModel.onEvent(OnboardingEvent.SkipSampleData)
                    },
                    onBack = {
                        viewModel.onEvent(OnboardingEvent.PreviousStep)
                    },
                    isLoading = uiState.sampleDataCreationInProgress
                )
            }
            
            OnboardingStep.COMPLETION -> {
                OnboardingCompletionScreen(
                    onGetStarted = {
                        viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                    },
                    sampleDataCreated = uiState.onboardingState.sampleDataCreated,
                    accountsCreated = uiState.onboardingState.createdAccounts.size,
                    smsPermissionGranted = uiState.smsPermissionGranted
                )
            }
        }
        
        // Error dialog
        uiState.error?.let { error ->
            ErrorDialog(
                title = "Setup Error",
                message = error,
                onDismiss = {
                    viewModel.clearError()
                }
            )
        }
    }
}