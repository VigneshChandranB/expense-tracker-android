package com.expensetracker.domain.error

/**
 * Represents the result of an operation that can fail
 */
sealed class ErrorResult<out T> {
    data class Success<T>(val data: T) : ErrorResult<T>()
    data class Error(
        val errorType: ErrorType,
        val message: String,
        val isRecoverable: Boolean = true,
        val retryAction: (() -> Unit)? = null,
        val cause: Throwable? = null
    ) : ErrorResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): ErrorResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    inline fun <R> flatMap(transform: (T) -> ErrorResult<R>): ErrorResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
        }
    }
    
    inline fun onSuccess(action: (T) -> Unit): ErrorResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Error) -> Unit): ErrorResult<T> {
        if (this is Error) action(this)
        return this
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw RuntimeException(message, cause)
    }
}

/**
 * Extension function to convert a nullable result to ErrorResult
 */
fun <T> T?.toErrorResult(errorType: ErrorType, message: String): ErrorResult<T> {
    return if (this != null) {
        ErrorResult.Success(this)
    } else {
        ErrorResult.Error(errorType, message)
    }
}

/**
 * Extension function to safely execute a block and return ErrorResult
 */
inline fun <T> safeCall(
    errorType: ErrorType,
    message: String,
    block: () -> T
): ErrorResult<T> {
    return try {
        ErrorResult.Success(block())
    } catch (e: Exception) {
        ErrorResult.Error(errorType, message, cause = e)
    }
}