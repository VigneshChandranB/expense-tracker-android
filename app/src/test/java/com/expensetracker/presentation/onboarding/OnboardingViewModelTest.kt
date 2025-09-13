package com.expensetracker.presentation.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.expensetracker.domain.model.*
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.usecase.onboarding.CreateSampleDataUseCase
import com.expensetracker.domain.usecase.onboarding.GetOnboardingStateUseCase
import com.expensetracker.domain.usecase.onboarding.UpdateOnboardingStateUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getOnboardingStateUseCase: GetOnboardingStateUseCase
    private lateinit var updateOnboardingStateUseCase: UpdateOnboardingStateUseCase
    private lateinit var createSampleDataUseCase: CreateSampleDataUseCase
    private lateinit var permissionManager: PermissionManager
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getOnboardingStateUseCase = mockk()
        updateOnboardingStateUseCase = mockk()
        createSampleDataUseCase = mockk()
        permissionManager = mockk()

        every { getOnboardingStateUseCase() } returns flowOf(OnboardingState())
        every { permissionManager.hasSmsPermission() } returns false
        coEvery { updateOnboardingStateUseCase.completeStep(any()) } just Runs
        coEvery { updateOnboardingStateUseCase.updateState(any()) } just Runs
        coEvery { updateOnboardingStateUseCase.markOnboardingComplete() } just Runs

        viewModel = OnboardingViewModel(
            getOnboardingStateUseCase,
            updateOnboardingStateUseCase,
            createSampleDataUseCase,
            permissionManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val uiState = viewModel.uiState.value
        
        assertEquals(OnboardingStep.WELCOME, uiState.onboardingState.currentStep)
        assertFalse(uiState.isLoading)
        assertFalse(uiState.smsPermissionGranted)
        assertTrue(uiState.tutorialSteps.isNotEmpty())
    }

    @Test
    fun `next step should advance to privacy explanation`() = runTest {
        viewModel.onEvent(OnboardingEvent.NextStep)
        
        coVerify { updateOnboardingStateUseCase.completeStep(OnboardingStep.WELCOME) }
    }

    @Test
    fun `skip SMS permission should update state`() = runTest {
        viewModel.onEvent(OnboardingEvent.SkipSmsPermission)
        
        coVerify { 
            updateOnboardingStateUseCase.updateState(
                match { state ->
                    state.hasSkippedSmsPermission && 
                    state.currentStep == OnboardingStep.ACCOUNT_CREATION
                }
            )
        }
    }

    @Test
    fun `create sample data should call use case`() = runTest {
        coEvery { createSampleDataUseCase.createSampleData(any()) } returns Result.success(Unit)
        
        viewModel.onEvent(OnboardingEvent.CreateSampleData)
        
        coVerify { createSampleDataUseCase.createSampleData(any()) }
    }

    @Test
    fun `create sample data failure should set error`() = runTest {
        val errorMessage = "Failed to create sample data"
        coEvery { createSampleDataUseCase.createSampleData(any()) } returns 
            Result.failure(Exception(errorMessage))
        
        viewModel.onEvent(OnboardingEvent.CreateSampleData)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `complete onboarding should mark as complete and emit navigation event`() = runTest {
        viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
        
        coVerify { updateOnboardingStateUseCase.markOnboardingComplete() }
    }

    @Test
    fun `clear error should reset error state`() = runTest {
        // First set an error
        coEvery { createSampleDataUseCase.createSampleData(any()) } returns 
            Result.failure(Exception("Test error"))
        viewModel.onEvent(OnboardingEvent.CreateSampleData)
        
        // Then clear it
        viewModel.clearError()
        
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.error)
    }

    @Test
    fun `tutorial steps should be initialized correctly`() = runTest {
        val uiState = viewModel.uiState.value
        val tutorialSteps = uiState.tutorialSteps
        
        assertTrue(tutorialSteps.isNotEmpty())
        assertTrue(tutorialSteps.any { it.id == "dashboard" })
        assertTrue(tutorialSteps.any { it.id == "accounts" })
        assertTrue(tutorialSteps.any { it.id == "sms_parsing" })
        assertTrue(tutorialSteps.any { it.id == "categorization" })
        assertTrue(tutorialSteps.any { it.id == "analytics" })
        assertTrue(tutorialSteps.any { it.id == "export" })
    }

    @Test
    fun `navigate to step should update current step`() = runTest {
        val targetStep = OnboardingStep.FEATURE_INTRODUCTION
        
        viewModel.onEvent(OnboardingEvent.NavigateToStep(targetStep))
        
        coVerify { 
            updateOnboardingStateUseCase.updateState(
                match { state -> state.currentStep == targetStep }
            )
        }
    }

    @Test
    fun `SMS permission granted should be reflected in UI state`() = runTest {
        every { permissionManager.hasSmsPermission() } returns true
        
        // Create new viewModel with SMS permission granted
        val viewModelWithPermission = OnboardingViewModel(
            getOnboardingStateUseCase,
            updateOnboardingStateUseCase,
            createSampleDataUseCase,
            permissionManager
        )
        
        val uiState = viewModelWithPermission.uiState.value
        assertTrue(uiState.smsPermissionGranted)
    }
}