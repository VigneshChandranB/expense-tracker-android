package com.expensetracker.data.mapper

import com.expensetracker.data.local.entity.OnboardingEntity
import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Mapper for converting between OnboardingEntity and OnboardingState
 */
object OnboardingMapper {
    
    fun toEntity(state: OnboardingState): OnboardingEntity {
        return OnboardingEntity(
            id = 1,
            currentStep = state.currentStep.name,
            completedSteps = Json.encodeToString(state.completedSteps.map { it.name }),
            isCompleted = state.isCompleted,
            hasSkippedSmsPermission = state.hasSkippedSmsPermission,
            createdAccounts = Json.encodeToString(state.createdAccounts),
            sampleDataCreated = state.sampleDataCreated,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun toDomain(entity: OnboardingEntity): OnboardingState {
        return OnboardingState(
            currentStep = OnboardingStep.valueOf(entity.currentStep),
            completedSteps = try {
                Json.decodeFromString<List<String>>(entity.completedSteps)
                    .mapNotNull { stepName ->
                        try {
                            OnboardingStep.valueOf(stepName)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }.toSet()
            } catch (e: Exception) {
                emptySet()
            },
            isCompleted = entity.isCompleted,
            hasSkippedSmsPermission = entity.hasSkippedSmsPermission,
            createdAccounts = try {
                Json.decodeFromString<List<Long>>(entity.createdAccounts)
            } catch (e: Exception) {
                emptyList()
            },
            sampleDataCreated = entity.sampleDataCreated
        )
    }
    
    fun getDefaultState(): OnboardingState {
        return OnboardingState(
            currentStep = OnboardingStep.WELCOME,
            completedSteps = emptySet(),
            isCompleted = false,
            hasSkippedSmsPermission = false,
            createdAccounts = emptyList(),
            sampleDataCreated = false
        )
    }
}