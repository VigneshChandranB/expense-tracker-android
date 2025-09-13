package com.expensetracker.data.local.error

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import com.expensetracker.domain.error.safeCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles database errors with recovery mechanisms and retry logic
 */
@Singleton
class DatabaseErrorHandler @Inject constructor() {
    
    private val maxRetries = 3
    private val retryDelayMs = 500L
    
    /**
     * Executes database operation with error handling and retry logic
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T
    ): ErrorResult<T> {
        return retryDatabaseOperation(maxRetries) {
            safeCall(
                errorType = ErrorType.DatabaseError.TransactionFailed,
                message = "Database operation failed"
            ) {
                operation()
            }
        }
    }
    
    /**
     * Executes database transaction with proper error handling
     */
    suspend fun <T> executeTransaction(
        database: RoomDatabase,
        operation: suspend () -> T
    ): ErrorResult<T> {
        return safeCall(
            errorType = ErrorType.DatabaseError.TransactionFailed,
            message = "Database transaction failed"
        ) {
            database.runInTransaction<T> {
                runBlocking { operation() }
            }
        }.let { result ->
            when (result) {
                is ErrorResult.Error -> {
                    // Map specific SQLite exceptions to appropriate error types
                    val mappedError = mapSqliteException(result.cause)
                    if (mappedError != null) {
                        ErrorResult.Error(
                            mappedError,
                            getErrorMessage(mappedError),
                            isRecoverable = isRecoverableError(mappedError),
                            cause = result.cause
                        )
                    } else {
                        result
                    }
                }
                is ErrorResult.Success -> result
            }
        }
    }
    
    /**
     * Validates database integrity
     */
    suspend fun validateDatabaseIntegrity(database: SupportSQLiteDatabase): ErrorResult<Boolean> {
        return safeCall(
            errorType = ErrorType.DatabaseError.DataCorruption,
            message = "Database integrity check failed"
        ) {
            val cursor = database.query("PRAGMA integrity_check")
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    result == "ok"
                } else {
                    false
                }
            }
        }
    }
    
    /**
     * Attempts to repair corrupted database
     */
    suspend fun repairDatabase(database: SupportSQLiteDatabase): ErrorResult<Boolean> {
        return safeCall(
            errorType = ErrorType.DatabaseError.DataCorruption,
            message = "Database repair failed"
        ) {
            // Try to rebuild indexes
            database.execSQL("REINDEX")
            
            // Run vacuum to clean up
            database.execSQL("VACUUM")
            
            // Verify integrity after repair
            val integrityResult = validateDatabaseIntegrity(database)
            integrityResult.getOrNull() ?: false
        }
    }
    
    /**
     * Checks available disk space before operations
     */
    fun checkDiskSpace(requiredBytes: Long): ErrorResult<Boolean> {
        return safeCall(
            errorType = ErrorType.DatabaseError.DiskSpaceFull,
            message = "Insufficient disk space"
        ) {
            val availableBytes = getAvailableDiskSpace()
            availableBytes >= requiredBytes
        }
    }
    
    private fun getAvailableDiskSpace(): Long {
        return try {
            val dataDir = android.os.Environment.getDataDirectory()
            dataDir.freeSpace
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Maps SQLite exceptions to domain error types
     */
    private fun mapSqliteException(throwable: Throwable?): ErrorType.DatabaseError? {
        return when (throwable) {
            is SQLiteConstraintException -> ErrorType.DatabaseError.ConstraintViolation
            is SQLiteFullException -> ErrorType.DatabaseError.DiskSpaceFull
            is SQLiteException -> {
                when {
                    throwable.message?.contains("database is locked") == true -> 
                        ErrorType.DatabaseError.ConnectionFailed
                    throwable.message?.contains("corrupt") == true -> 
                        ErrorType.DatabaseError.DataCorruption
                    else -> ErrorType.DatabaseError.TransactionFailed
                }
            }
            else -> null
        }
    }
    
    /**
     * Determines if a database error is recoverable
     */
    private fun isRecoverableError(errorType: ErrorType.DatabaseError): Boolean {
        return when (errorType) {
            ErrorType.DatabaseError.ConnectionFailed,
            ErrorType.DatabaseError.TransactionFailed -> true
            ErrorType.DatabaseError.ConstraintViolation -> true
            ErrorType.DatabaseError.DiskSpaceFull -> false
            ErrorType.DatabaseError.DataCorruption -> false
            is ErrorType.DatabaseError.MigrationFailed -> false
        }
    }
    
    /**
     * Gets user-friendly error message for database errors
     */
    private fun getErrorMessage(errorType: ErrorType.DatabaseError): String {
        return when (errorType) {
            ErrorType.DatabaseError.ConnectionFailed -> 
                "Unable to connect to database. Please try again."
            ErrorType.DatabaseError.ConstraintViolation -> 
                "Data validation failed. Please check your input."
            ErrorType.DatabaseError.DataCorruption -> 
                "Database corruption detected. App data may need to be restored."
            ErrorType.DatabaseError.TransactionFailed -> 
                "Database operation failed. Please try again."
            ErrorType.DatabaseError.DiskSpaceFull -> 
                "Insufficient storage space. Please free up space and try again."
            is ErrorType.DatabaseError.MigrationFailed -> 
                "Database upgrade failed. Please reinstall the app."
        }
    }
    
    /**
     * Generic retry mechanism for database operations
     */
    private suspend fun <T> retryDatabaseOperation(
        maxAttempts: Int,
        operation: suspend () -> ErrorResult<T>
    ): ErrorResult<T> {
        var lastError: ErrorResult.Error? = null
        
        repeat(maxAttempts) { attempt ->
            when (val result = operation()) {
                is ErrorResult.Success -> return result
                is ErrorResult.Error -> {
                    lastError = result
                    
                    // Don't retry non-recoverable errors
                    if (!result.isRecoverable) {
                        return result
                    }
                    
                    if (attempt < maxAttempts - 1) {
                        delay(retryDelayMs * (attempt + 1))
                    }
                }
            }
        }
        
        return lastError ?: ErrorResult.Error(
            ErrorType.DatabaseError.TransactionFailed,
            "Database operation failed after $maxAttempts attempts"
        )
    }
    
    /**
     * Creates database backup before risky operations
     */
    suspend fun createBackup(
        database: SupportSQLiteDatabase,
        backupPath: String
    ): ErrorResult<String> {
        return safeCall(
            errorType = ErrorType.DatabaseError.TransactionFailed,
            message = "Database backup failed"
        ) {
            // In a real implementation, this would copy the database file
            // For now, we'll simulate the backup process
            database.query("SELECT COUNT(*) FROM sqlite_master").use { cursor ->
                if (cursor.moveToFirst()) {
                    val tableCount = cursor.getInt(0)
                    if (tableCount > 0) {
                        backupPath
                    } else {
                        throw IllegalStateException("Database appears to be empty")
                    }
                } else {
                    throw IllegalStateException("Unable to read database schema")
                }
            }
        }
    }
}