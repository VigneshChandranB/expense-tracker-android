package com.expensetracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OnboardingRepositoryIntegrationTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var repository: OnboardingRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = OnboardingRepositoryImpl(database.onboardingDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getOnboardingState_returnsDefaultStateWhenNoDataExists() = runTest {
        val state = repository.getOnboardingState().first()
        
        assertEquals(OnboardingStep.WELCOME, state.currentStep)
        assertFalse(state.isCompleted)
        assertTrue(state.completedSteps.isEmpty())
        assertFalse(state.hasSkippedSmsPermission)
        assertTrue(state.createdAccounts.isEmpty())
        assertFalse(state.sampleDataCreated)
    }

    @Test
    fun updateOnboardingState_persistsStateCorrectly() = runTest {
        val testState = OnboardingState(
            currentStep = OnboardingStep.PRIVACY_EXPLANATION,
            completedSteps = setOf(OnboardingStep.WELCOME),
            isCompleted = false,
            hasSkippedSmsPermission = true,
            createdAccounts = listOf(1L, 2L),
            sampleDataCreated = true
        )

        repository.updateOnboardingState(testState)
        val retrievedState = repository.getOnboardingState().first()

        assertEquals(testState.currentStep, retrievedState.currentStep)
        assertEquals(testState.completedSteps, retrievedState.completedSteps)
        assertEquals(testState.isCompleted, retrievedState.isCompleted)
        assertEquals(testState.hasSkippedSmsPermission, retrievedState.hasSkippedSmsPermission)
        assertEquals(testState.createdAccounts, retrievedState.createdAccounts)
        assertEquals(testState.sampleDataCreated, retrievedState.sampleDataCreated)
    }

    @Test
    fun completeStep_updatesStateCorrectly() = runTest {
        repository.completeStep(OnboardingStep.WELCOME)
        
        val state = repository.getOnboardingState().first()
        assertEquals(OnboardingStep.PRIVACY_EXPLANATION, state.currentStep)
        assertTrue(state.completedSteps.contains(OnboardingStep.WELCOME))
    }

    @Test
    fun markOnboardingComplete_setsCompletedState() = runTest {
        repository.markOnboardingComplete()
        
        val state = repository.getOnboardingState().first()
        assertTrue(state.isCompleted)
        assertEquals(OnboardingStep.COMPLETION, state.currentStep)
        assertEquals(OnboardingStep.values().toSet(), state.completedSteps)
    }

    @Test
    fun isOnboardingCompleted_returnsFalseInitially() = runTest {
        val isCompleted = repository.isOnboardingCompleted()
        assertFalse(isCompleted)
    }

    @Test
    fun isOnboardingCompleted_returnsTrueAfterCompletion() = runTest {
        repository.markOnboardingComplete()
        
        val isCompleted = repository.isOnboardingCompleted()
        assertTrue(isCompleted)
    }

    @Test
    fun multipleUpdates_maintainConsistency() = runTest {
        // First update
        val state1 = OnboardingState(
            currentStep = OnboardingStep.SMS_PERMISSION_SETUP,
            completedSteps = setOf(OnboardingStep.WELCOME, OnboardingStep.PRIVACY_EXPLANATION)
        )
        repository.updateOnboardingState(state1)

        // Second update
        val state2 = state1.copy(
            currentStep = OnboardingStep.ACCOUNT_CREATION,
            completedSteps = state1.completedSteps + OnboardingStep.SMS_PERMISSION_SETUP,
            hasSkippedSmsPermission = true
        )
        repository.updateOnboardingState(state2)

        val finalState = repository.getOnboardingState().first()
        assertEquals(OnboardingStep.ACCOUNT_CREATION, finalState.currentStep)
        assertEquals(3, finalState.completedSteps.size)
        assertTrue(finalState.hasSkippedSmsPermission)
    }

    @Test
    fun stateWithAccountIds_persistsCorrectly() = runTest {
        val accountIds = listOf(100L, 200L, 300L)
        val state = OnboardingState(
            currentStep = OnboardingStep.FEATURE_INTRODUCTION,
            createdAccounts = accountIds
        )

        repository.updateOnboardingState(state)
        val retrievedState = repository.getOnboardingState().first()

        assertEquals(accountIds, retrievedState.createdAccounts)
    }

    @Test
    fun stateWithAllStepsCompleted_persistsCorrectly() = runTest {
        val allSteps = OnboardingStep.values().toSet()
        val state = OnboardingState(
            currentStep = OnboardingStep.COMPLETION,
            completedSteps = allSteps,
            isCompleted = true,
            hasSkippedSmsPermission = false,
            createdAccounts = listOf(1L, 2L, 3L),
            sampleDataCreated = true
        )

        repository.updateOnboardingState(state)
        val retrievedState = repository.getOnboardingState().first()

        assertEquals(allSteps, retrievedState.completedSteps)
        assertTrue(retrievedState.isCompleted)
        assertEquals(3, retrievedState.createdAccounts.size)
        assertTrue(retrievedState.sampleDataCreated)
    }
}