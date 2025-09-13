package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entity.OnboardingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for onboarding state operations
 */
@Dao
interface OnboardingDao {
    
    @Query("SELECT * FROM onboarding_state WHERE id = 1")
    fun getOnboardingState(): Flow<OnboardingEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOnboardingState(onboardingEntity: OnboardingEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM onboarding_state WHERE id = 1 AND isCompleted = 1)")
    suspend fun isOnboardingCompleted(): Boolean
    
    @Query("DELETE FROM onboarding_state")
    suspend fun clearOnboardingState()
}