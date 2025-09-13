package com.expensetracker.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.*
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.usecase.onboarding.CreateSampleDataUseCase
import com.expensetracker.domain.usecase.onboarding.GetOnboardingStateUseCase
import com.expensetracker.domain.usecase.onboarding.UpdateOnboardingStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing onboarding flow
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getOnboardingStateUseCase: GetOnboardingStateUseCase,
    private val updateOnboardingStateUseCase: UpdateOnboardingStateUseCase,
    private val createSampleDataUseCase: CreateSampleDataUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private val _navigationEvent = MutableSharedFlow<OnboardingNavigationEvent>()
    val navigationEvent: SharedFlow<OnboardingNavigationEvent> = _navigationEvent.asSharedFlow()
    
    init {
        observeOnboardingState()
        initializeTutorialSteps()
    }
    
    private fun observeOnboardingState() {
        viewModelScope.launch {
            getOnboardingStateUseCase().collect { onboardingState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        onboardingState = onboardingState,
                        smsPermissionGranted = permissionManager.hasSmsPermission()
                    )
                }
            }
        }
    }
    
    private fun initializeTutorialSteps() {
        val tutorialSteps = listOf(
            TutorialStep(
                id = "dashboard",
                title = "Dashboard Overview",
                description = "View your account balances, recent transactions, and spending insights all in one place.",
                targetFeature = "dashboard",
                iconResource = "dashboard"
            ),
            TutorialStep(
                id = "accounts",
                title = "Multi-Account Management",
                description = "Manage multiple bank accounts and credit cards. Switch between accounts and track transfers.",
                targetFeature = "accounts",
                iconResource = "account_balance"
            ),
            TutorialStep(
                id = "sms_parsing",
                title = "Automatic SMS Parsing",
                description = "Automatically extract transaction details from bank SMS messages for effortless tracking.",
                targetFeature = "sms",
                iconResource = "sms"
            ),
            TutorialStep(
                id = "categorization",
                title = "Smart Categorization",
                description = "Transactions are automatically categorized. You can customize categories and teach the system.",
                targetFeature = "categories",
                iconResource = "category"
            ),
            TutorialStep(
                id = "analytics",
                title = "Spending Analytics",
                description = "Get insights into your spending patterns with detailed charts and trend analysis.",
                targetFeature = "analytics",
                iconResource = "analytics"
            ),
            TutorialStep(
                id = "export",
                title = "Data Export",
                description = "Export your transaction data to CSV or PDF for external analysis or record keeping.",
                targetFeature = "export",
                iconResource = "file_download"
            )
        )
        
        _uiState.update { it.copy(tutorialSteps = tutorialSteps) }
    }
    
    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NextStep -> handleNextStep()
            is OnboardingEvent.PreviousStep -> handlePreviousStep()
            is OnboardingEvent.SkipStep -> handleSkipStep()
            is OnboardingEvent.RequestSmsPermission -> handleRequestSmsPermission()
            is OnboardingEvent.SkipSmsPermission -> handleSkipSmsPermission()
            is OnboardingEvent.CreateSampleAccount -> handleCreateSampleAccount()
            is OnboardingEvent.SkipAccountCreation -> handleSkipAccountCreation()
            is OnboardingEvent.CreateSampleData -> handleCreateSampleData()
            is OnboardingEvent.SkipSampleData -> handleSkipSampleData()
            is OnboardingEvent.CompleteTutorial -> handleCompleteTutorial()
            is OnboardingEvent.CompleteOnboarding -> handleCompleteOnboarding()
            is OnboardingEvent.NavigateToStep -> handleNavigateToStep(event.step)
        }
    }
    
    private fun handleNextStep() {
        val currentStep = _uiState.value.onboardingState.currentStep
        val nextStep = getNextStep(currentStep)
        
        viewModelScope.launch {
            updateOnboardingStateUseCase.completeStep(currentStep)
            
            if (nextStep == OnboardingStep.COMPLETION) {
                handleCompleteOnboarding()
            }
        }
    }
    
    private fun handlePreviousStep() {
        val currentStep = _uiState.value.onboardingState.currentStep
        val previousStep = getPreviousStep(currentStep)
        
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(currentStep = previousStep)
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    private fun handleSkipStep() {
        handleNextStep()
    }
    
    private fun handleRequestSmsPermission() {
        viewModelScope.launch {
            _navigationEvent.emit(OnboardingNavigationEvent.RequestSmsPermission)
        }
    }
    
    private fun handleSkipSmsPermission() {
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(
                hasSkippedSmsPermission = true,
                currentStep = OnboardingStep.ACCOUNT_CREATION
            )
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    private fun handleCreateSampleAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(accountCreationInProgress = true) }
            
            // Navigate to account creation screen
            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToAccountCreation)
            
            _uiState.update { it.copy(accountCreationInProgress = false) }
        }
    }
    
    private fun handleSkipAccountCreation() {
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(currentStep = OnboardingStep.FEATURE_INTRODUCTION)
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    private fun handleCreateSampleData() {
        viewModelScope.launch {
            _uiState.update { it.copy(sampleDataCreationInProgress = true, isLoading = true) }
            
            val config = SampleDataConfig(
                createSampleAccounts = true,
                createSampleTransactions = true,
                createSampleCategories = true,
                numberOfSampleTransactions = 15
            )
            
            createSampleDataUseCase.createSampleData(config)
                .onSuccess {
                    val currentState = _uiState.value.onboardingState
                    val updatedState = currentState.copy(
                        sampleDataCreated = true,
                        currentStep = OnboardingStep.COMPLETION
                    )
                    updateOnboardingStateUseCase.updateState(updatedState)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            error = "Failed to create sample data: ${error.message}",
                            isLoading = false,
                            sampleDataCreationInProgress = false
                        )
                    }
                }
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    sampleDataCreationInProgress = false
                )
            }
        }
    }
    
    private fun handleSkipSampleData() {
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(currentStep = OnboardingStep.COMPLETION)
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    private fun handleCompleteTutorial() {
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(currentStep = OnboardingStep.SAMPLE_DATA_SETUP)
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    private fun handleCompleteOnboarding() {
        viewModelScope.launch {
            updateOnboardingStateUseCase.markOnboardingComplete()
            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToDashboard)
        }
    }
    
    private fun handleNavigateToStep(step: OnboardingStep) {
        viewModelScope.launch {
            val currentState = _uiState.value.onboardingState
            val updatedState = currentState.copy(currentStep = step)
            updateOnboardingStateUseCase.updateState(updatedState)
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun getNextStep(currentStep: OnboardingStep): OnboardingStep {
        return when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.PRIVACY_EXPLANATION
            OnboardingStep.PRIVACY_EXPLANATION -> OnboardingStep.SMS_PERMISSION_SETUP
            OnboardingStep.SMS_PERMISSION_SETUP -> OnboardingStep.ACCOUNT_CREATION
            OnboardingStep.ACCOUNT_CREATION -> OnboardingStep.FEATURE_INTRODUCTION
            OnboardingStep.FEATURE_INTRODUCTION -> OnboardingStep.SAMPLE_DATA_SETUP
            OnboardingStep.SAMPLE_DATA_SETUP -> OnboardingStep.COMPLETION
            OnboardingStep.COMPLETION -> OnboardingStep.COMPLETION
        }
    }
    
    private fun getPreviousStep(currentStep: OnboardingStep): OnboardingStep {
        return when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.PRIVACY_EXPLANATION -> OnboardingStep.WELCOME
            OnboardingStep.SMS_PERMISSION_SETUP -> OnboardingStep.PRIVACY_EXPLANATION
            OnboardingStep.ACCOUNT_CREATION -> OnboardingStep.SMS_PERMISSION_SETUP
            OnboardingStep.FEATURE_INTRODUCTION -> OnboardingStep.ACCOUNT_CREATION
            OnboardingStep.SAMPLE_DATA_SETUP -> OnboardingStep.FEATURE_INTRODUCTION
            OnboardingStep.COMPLETION -> OnboardingStep.SAMPLE_DATA_SETUP
        }
    }
}

/**
 * Navigation events for onboarding
 */
sealed class OnboardingNavigationEvent {
    object RequestSmsPermission : OnboardingNavigationEvent()
    object NavigateToAccountCreation : OnboardingNavigationEvent()
    object NavigateToDashboard : OnboardingNavigationEvent()
}