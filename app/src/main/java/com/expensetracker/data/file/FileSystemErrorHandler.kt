package com.expensetracker.data.file

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import com.expensetracker.domain.error.safeCall
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles file system errors with recovery mechanisms
 */
@Singleton
class FileSystemErrorHandler @Inject constructor(
    private val context: Context
) {
    
    private val maxRetries = 3
    private val retryDelayMs = 1000L
    
    /**
     * Executes file operation with error handling and retry logic
     */
    suspend fun <T> executeFileOperation(
        operation: suspend () -> T
    ): ErrorResult<T> {
        return retryFileOperation(maxRetries) {
            safeCall(
                errorType = ErrorType.FileSystemError.WriteError,
                message = "File operation failed"
            ) {
                operation()
            }
        }
    }
    
    /**
     * Validates file path and permissions before operation
     */
    fun validateFilePath(filePath: String): ErrorResult<File> {
        return safeCall(
            errorType = ErrorType.FileSystemError.InvalidPath(filePath),
            message = "Invalid file path: $filePath"
        ) {
            val file = File(filePath)
            
            // Check if path is valid
            if (!isValidPath(filePath)) {
                throw IllegalArgumentException("Invalid file path format")
            }
            
            // Check parent directory exists or can be created
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw IOException("Cannot create parent directory: ${parentDir.absolutePath}")
                }
            }
            
            file
        }
    }
    
    /**
     * Checks available storage space before file operations
     */
    fun checkStorageSpace(requiredBytes: Long, targetPath: String): ErrorResult<Boolean> {
        return safeCall(
            errorType = ErrorType.FileSystemError.InsufficientStorage,
            message = "Insufficient storage space"
        ) {
            val availableBytes = getAvailableSpace(targetPath)
            
            if (availableBytes < requiredBytes) {
                throw IOException("Insufficient storage: required $requiredBytes bytes, available $availableBytes bytes")
            }
            
            true
        }
    }
    
    /**
     * Safely reads file with error handling
     */
    suspend fun readFile(filePath: String): ErrorResult<String> {
        return executeFileOperation {
            val file = validateFilePath(filePath).getOrThrow()
            
            if (!file.exists()) {
                throw IOException("File does not exist: $filePath")
            }
            
            if (!file.canRead()) {
                throw SecurityException("No read permission for file: $filePath")
            }
            
            file.readText()
        }.let { result ->
            when (result) {
                is ErrorResult.Error -> {
                    val mappedError = mapFileException(result.cause, filePath)
                    if (mappedError != null) {
                        ErrorResult.Error(
                            mappedError,
                            getFileErrorMessage(mappedError, filePath),
                            isRecoverable = isRecoverableFileError(mappedError),
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
     * Safely writes file with error handling
     */
    suspend fun writeFile(filePath: String, content: String): ErrorResult<Boolean> {
        return executeFileOperation {
            val file = validateFilePath(filePath).getOrThrow()
            
            // Check storage space
            val contentBytes = content.toByteArray().size.toLong()
            checkStorageSpace(contentBytes, filePath).getOrThrow()
            
            // Check write permissions
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.canWrite()) {
                throw SecurityException("No write permission for directory: ${parentDir.absolutePath}")
            }
            
            file.writeText(content)
            true
        }.let { result ->
            when (result) {
                is ErrorResult.Error -> {
                    val mappedError = mapFileException(result.cause, filePath)
                    if (mappedError != null) {
                        ErrorResult.Error(
                            mappedError,
                            getFileErrorMessage(mappedError, filePath),
                            isRecoverable = isRecoverableFileError(mappedError),
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
     * Safely deletes file with error handling
     */
    suspend fun deleteFile(filePath: String): ErrorResult<Boolean> {
        return executeFileOperation {
            val file = validateFilePath(filePath).getOrThrow()
            
            if (!file.exists()) {
                return@executeFileOperation true // Already deleted
            }
            
            if (!file.canWrite()) {
                throw SecurityException("No delete permission for file: $filePath")
            }
            
            file.delete()
        }.let { result ->
            when (result) {
                is ErrorResult.Error -> {
                    val mappedError = mapFileException(result.cause, filePath)
                    if (mappedError != null) {
                        ErrorResult.Error(
                            mappedError,
                            getFileErrorMessage(mappedError, filePath),
                            isRecoverable = isRecoverableFileError(mappedError),
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
     * Creates backup of file before risky operations
     */
    suspend fun createFileBackup(filePath: String): ErrorResult<String> {
        return safeCall(
            errorType = ErrorType.FileSystemError.WriteError,
            message = "File backup creation failed"
        ) {
            val originalFile = File(filePath)
            if (!originalFile.exists()) {
                throw IOException("Original file does not exist: $filePath")
            }
            
            val backupPath = "$filePath.backup.${System.currentTimeMillis()}"
            val backupFile = File(backupPath)
            
            originalFile.copyTo(backupFile, overwrite = true)
            backupPath
        }
    }
    
    /**
     * Restores file from backup
     */
    suspend fun restoreFromBackup(originalPath: String, backupPath: String): ErrorResult<Boolean> {
        return safeCall(
            errorType = ErrorType.FileSystemError.WriteError,
            message = "File restore from backup failed"
        ) {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                throw IOException("Backup file does not exist: $backupPath")
            }
            
            val originalFile = File(originalPath)
            backupFile.copyTo(originalFile, overwrite = true)
            true
        }
    }
    
    private fun isValidPath(path: String): Boolean {
        return try {
            val file = File(path)
            val canonicalPath = file.canonicalPath
            
            // Check for path traversal attacks
            !canonicalPath.contains("..")
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAvailableSpace(path: String): Long {
        return try {
            val file = File(path)
            val parentDir = file.parentFile ?: file
            
            if (parentDir.exists()) {
                parentDir.freeSpace
            } else {
                // Use internal storage as fallback
                context.filesDir.freeSpace
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Maps file exceptions to domain error types
     */
    private fun mapFileException(throwable: Throwable?, filePath: String): ErrorType.FileSystemError? {
        return when (throwable) {
            is IOException -> {
                when {
                    throwable.message?.contains("No space left") == true -> 
                        ErrorType.FileSystemError.InsufficientStorage
                    throwable.message?.contains("does not exist") == true -> 
                        ErrorType.FileSystemError.FileNotFound
                    throwable.message?.contains("Permission denied") == true -> 
                        ErrorType.FileSystemError.PermissionDenied
                    else -> ErrorType.FileSystemError.WriteError
                }
            }
            is SecurityException -> ErrorType.FileSystemError.PermissionDenied
            is IllegalArgumentException -> ErrorType.FileSystemError.InvalidPath(filePath)
            else -> null
        }
    }
    
    /**
     * Determines if a file error is recoverable
     */
    private fun isRecoverableFileError(errorType: ErrorType.FileSystemError): Boolean {
        return when (errorType) {
            ErrorType.FileSystemError.WriteError,
            ErrorType.FileSystemError.ReadError -> true
            ErrorType.FileSystemError.PermissionDenied -> true
            ErrorType.FileSystemError.InsufficientStorage -> false
            ErrorType.FileSystemError.FileNotFound -> false
            is ErrorType.FileSystemError.InvalidPath -> false
        }
    }
    
    /**
     * Gets user-friendly error message for file errors
     */
    private fun getFileErrorMessage(errorType: ErrorType.FileSystemError, filePath: String): String {
        return when (errorType) {
            ErrorType.FileSystemError.InsufficientStorage -> 
                "Not enough storage space available"
            ErrorType.FileSystemError.PermissionDenied -> 
                "Permission denied to access file"
            ErrorType.FileSystemError.FileNotFound -> 
                "File not found: ${File(filePath).name}"
            ErrorType.FileSystemError.WriteError -> 
                "Failed to write file"
            ErrorType.FileSystemError.ReadError -> 
                "Failed to read file"
            is ErrorType.FileSystemError.InvalidPath -> 
                "Invalid file path"
        }
    }
    
    /**
     * Generic retry mechanism for file operations
     */
    private suspend fun <T> retryFileOperation(
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
            ErrorType.FileSystemError.WriteError,
            "File operation failed after $maxAttempts attempts"
        )
    }
}