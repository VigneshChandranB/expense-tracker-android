package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing onboarding state
 */
@Entity(tableName = "onboarding_state")
data class OnboardingEntity(
    @PrimaryKey
    val id: Long = 1, // Single row for app-wide onboarding state
    val currentStep: String,
    val completedSteps: String, // JSON string of completed steps
    val isCompleted: Boolean,
    val hasSkippedSmsPermission: Boolean,
    val createdAccounts: String, // JSON string of account IDs
    val sampleDataCreated: Boolean,
    val updatedAt: Long
)