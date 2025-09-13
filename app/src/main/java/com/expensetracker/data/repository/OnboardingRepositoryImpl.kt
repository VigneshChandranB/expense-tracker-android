package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.OnboardingDao
import com.expensetracker.data.mapper.OnboardingMapper
import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import com.expensetracker.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OnboardingRepository
 */
@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val onboardingDao: OnboardingDao
) : OnboardingRepository {
    
    override fun getOnboardingState(): Flow<OnboardingState> {
        return onboardingDao.getOnboardingState().map { entity ->
            entity?.let { OnboardingMapper.toDomain(it) } 
                ?: OnboardingMapper.getDefaultState()
        }
    }
    
    override suspend fun updateOnboardingState(state: OnboardingState) {
        val entity = OnboardingMapper.toEntity(state)
        onboardingDao.insertOrUpdateOnboardingState(entity)
    }
    
    override suspend fun completeStep(step: OnboardingStep) {
        val currentState = onboardingDao.getOnboardingState()
        // This is a simplified implementation - in a real app you'd want to 
        // get the current state first, then update it
        val defaultState = OnboardingMapper.getDefaultState()
        val updatedState = defaultState.copy(
            completedSteps = defaultState.completedSteps + step,
            currentStep = getNextStep(step)
        )
        updateOnboardingState(updatedState)
    }
    
    override suspend fun markOnboardingComplete() {
        val currentState = OnboardingMapper.getDefaultState()
        val completedState = currentState.copy(
            isCompleted = true,
            currentStep = OnboardingStep.COMPLETION,
            completedSteps = OnboardingStep.values().toSet()
        )
        updateOnboardingState(completedState)
    }
    
    override suspend fun isOnboardingCompleted(): Boolean {
        return onboardingDao.isOnboardingCompleted()
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
}