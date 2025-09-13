package com.expensetracker.data.mapper

import com.expensetracker.data.local.entity.OnboardingEntity
import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingMapperTest {

    @Test
    fun `toEntity should convert OnboardingState correctly`() {
        val state = OnboardingState(
            currentStep = OnboardingStep.ACCOUNT_CREATION,
            completedSteps = setOf(OnboardingStep.WELCOME, OnboardingStep.PRIVACY_EXPLANATION),
            isCompleted = false,
            hasSkippedSmsPermission = true,
            createdAccounts = listOf(1L, 2L, 3L),
            sampleDataCreated = true
        )

        val entity = OnboardingMapper.toEntity(state)

        assertEquals(1L, entity.id)
        assertEquals("ACCOUNT_CREATION", entity.currentStep)
        assertTrue(entity.completedSteps.contains("WELCOME"))
        assertTrue(entity.completedSteps.contains("PRIVACY_EXPLANATION"))
        assertFalse(entity.isCompleted)
        assertTrue(entity.hasSkippedSmsPermission)
        assertTrue(entity.createdAccounts.contains("1"))
        assertTrue(entity.createdAccounts.contains("2"))
        assertTrue(entity.createdAccounts.contains("3"))
        assertTrue(entity.sampleDataCreated)
    }

    @Test
    fun `toDomain should convert OnboardingEntity correctly`() {
        val entity = OnboardingEntity(
            id = 1L,
            currentStep = "SMS_PERMISSION_SETUP",
            completedSteps = """["WELCOME","PRIVACY_EXPLANATION"]""",
            isCompleted = false,
            hasSkippedSmsPermission = true,
            createdAccounts = """[100,200,300]""",
            sampleDataCreated = false,
            updatedAt = System.currentTimeMillis()
        )

        val state = OnboardingMapper.toDomain(entity)

        assertEquals(OnboardingStep.SMS_PERMISSION_SETUP, state.currentStep)
        assertTrue(state.completedSteps.contains(OnboardingStep.WELCOME))
        assertTrue(state.completedSteps.contains(OnboardingStep.PRIVACY_EXPLANATION))
        assertEquals(2, state.completedSteps.size)
        assertFalse(state.isCompleted)
        assertTrue(state.hasSkippedSmsPermission)
        assertEquals(listOf(100L, 200L, 300L), state.createdAccounts)
        assertFalse(state.sampleDataCreated)
    }

    @Test
    fun `toDomain should handle empty completedSteps gracefully`() {
        val entity = OnboardingEntity(
            id = 1L,
            currentStep = "WELCOME",
            completedSteps = "[]",
            isCompleted = false,
            hasSkippedSmsPermission = false,
            createdAccounts = "[]",
            sampleDataCreated = false,
            updatedAt = System.currentTimeMillis()
        )

        val state = OnboardingMapper.toDomain(entity)

        assertTrue(state.completedSteps.isEmpty())
        assertTrue(state.createdAccounts.isEmpty())
    }

    @Test
    fun `toDomain should handle malformed JSON gracefully`() {
        val entity = OnboardingEntity(
            id = 1L,
            currentStep = "WELCOME",
            completedSteps = "invalid json",
            isCompleted = false,
            hasSkippedSmsPermission = false,
            createdAccounts = "also invalid",
            sampleDataCreated = false,
            updatedAt = System.currentTimeMillis()
        )

        val state = OnboardingMapper.toDomain(entity)

        assertTrue(state.completedSteps.isEmpty())
        assertTrue(state.createdAccounts.isEmpty())
        assertEquals(OnboardingStep.WELCOME, state.currentStep)
    }

    @Test
    fun `toDomain should handle invalid step names gracefully`() {
        val entity = OnboardingEntity(
            id = 1L,
            currentStep = "WELCOME",
            completedSteps = """["WELCOME","INVALID_STEP","PRIVACY_EXPLANATION"]""",
            isCompleted = false,
            hasSkippedSmsPermission = false,
            createdAccounts = "[]",
            sampleDataCreated = false,
            updatedAt = System.currentTimeMillis()
        )

        val state = OnboardingMapper.toDomain(entity)

        assertEquals(2, state.completedSteps.size)
        assertTrue(state.completedSteps.contains(OnboardingStep.WELCOME))
        assertTrue(state.completedSteps.contains(OnboardingStep.PRIVACY_EXPLANATION))
        assertFalse(state.completedSteps.any { it.name == "INVALID_STEP" })
    }

    @Test
    fun `getDefaultState should return correct initial state`() {
        val defaultState = OnboardingMapper.getDefaultState()

        assertEquals(OnboardingStep.WELCOME, defaultState.currentStep)
        assertTrue(defaultState.completedSteps.isEmpty())
        assertFalse(defaultState.isCompleted)
        assertFalse(defaultState.hasSkippedSmsPermission)
        assertTrue(defaultState.createdAccounts.isEmpty())
        assertFalse(defaultState.sampleDataCreated)
    }

    @Test
    fun `roundtrip conversion should preserve data`() {
        val originalState = OnboardingState(
            currentStep = OnboardingStep.FEATURE_INTRODUCTION,
            completedSteps = setOf(
                OnboardingStep.WELCOME,
                OnboardingStep.PRIVACY_EXPLANATION,
                OnboardingStep.SMS_PERMISSION_SETUP,
                OnboardingStep.ACCOUNT_CREATION
            ),
            isCompleted = false,
            hasSkippedSmsPermission = false,
            createdAccounts = listOf(10L, 20L),
            sampleDataCreated = true
        )

        val entity = OnboardingMapper.toEntity(originalState)
        val convertedState = OnboardingMapper.toDomain(entity)

        assertEquals(originalState.currentStep, convertedState.currentStep)
        assertEquals(originalState.completedSteps, convertedState.completedSteps)
        assertEquals(originalState.isCompleted, convertedState.isCompleted)
        assertEquals(originalState.hasSkippedSmsPermission, convertedState.hasSkippedSmsPermission)
        assertEquals(originalState.createdAccounts, convertedState.createdAccounts)
        assertEquals(originalState.sampleDataCreated, convertedState.sampleDataCreated)
    }

    @Test
    fun `toEntity should handle all onboarding steps`() {
        OnboardingStep.values().forEach { step ->
            val state = OnboardingState(currentStep = step)
            val entity = OnboardingMapper.toEntity(state)
            
            assertEquals(step.name, entity.currentStep)
        }
    }

    @Test
    fun `toDomain should handle all onboarding steps`() {
        OnboardingStep.values().forEach { step ->
            val entity = OnboardingEntity(
                id = 1L,
                currentStep = step.name,
                completedSteps = "[]",
                isCompleted = false,
                hasSkippedSmsPermission = false,
                createdAccounts = "[]",
                sampleDataCreated = false,
                updatedAt = System.currentTimeMillis()
            )
            
            val state = OnboardingMapper.toDomain(entity)
            assertEquals(step, state.currentStep)
        }
    }
}