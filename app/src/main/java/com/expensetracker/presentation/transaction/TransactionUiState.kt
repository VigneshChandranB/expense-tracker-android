package com.expensetracker.presentation.transaction

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * UI state for transaction management screens
 */
data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // Filtering and search state
    val searchQuery: String = "",
    val selectedAccountIds: List<Long> = emptyList(),
    val selectedCategoryIds: List<Long> = emptyList(),
    val selectedTypes: List<TransactionType> = emptyList(),
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val showFilters: Boolean = false
)

/**
 * Sort order options for transactions
 */
enum class SortOrder {
    DATE_ASC, DATE_DESC, AMOUNT_ASC, AMOUNT_DESC, MERCHANT_ASC, MERCHANT_DESC
}

/**
 * UI state for transaction form (create/edit)
 */
data class TransactionFormUiState(
    val transactionId: Long? = null,
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val merchant: String = "",
    val description: String = "",
    val date: LocalDateTime = LocalDateTime.now(),
    val selectedAccount: Account? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationErrors: TransactionValidationErrors = TransactionValidationErrors()
) {
    val isValid: Boolean
        get() = validationErrors.isEmpty() && 
                amount.isNotBlank() && 
                selectedCategory != null && 
                merchant.isNotBlank() && 
                selectedAccount != null
}

/**
 * UI state for transfer form
 */
data class TransferFormUiState(
    val amount: String = "",
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val description: String = "",
    val date: LocalDateTime = LocalDateTime.now(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationErrors: TransferValidationErrors = TransferValidationErrors()
) {
    val isValid: Boolean
        get() = validationErrors.isEmpty() && 
                amount.isNotBlank() && 
                fromAccount != null && 
                toAccount != null &&
                fromAccount != toAccount
}

/**
 * Validation errors for transaction form
 */
data class TransactionValidationErrors(
    val amount: String? = null,
    val category: String? = null,
    val merchant: String? = null,
    val account: String? = null,
    val date: String? = null
) {
    fun isEmpty(): Boolean = amount == null && category == null && merchant == null && account == null && date == null
}

/**
 * Validation errors for transfer form
 */
data class TransferValidationErrors(
    val amount: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val date: String? = null,
    val sameAccount: String? = null
) {
    fun isEmpty(): Boolean = amount == null && fromAccount == null && toAccount == null && date == null && sameAccount == null
}

/**
 * Events for transaction management
 */
sealed class TransactionEvent {
    object LoadTransactions : TransactionEvent()
    object LoadAccounts : TransactionEvent()
    object LoadCategories : TransactionEvent()
    data class DeleteTransaction(val transactionId: Long) : TransactionEvent()
    object Undo : TransactionEvent()
    object Redo : TransactionEvent()
    data class FilterByAccount(val accountId: Long?) : TransactionEvent()
    // Enhanced filtering and search events
    data class UpdateSearchQuery(val query: String) : TransactionEvent()
    data class ToggleAccountFilter(val accountId: Long) : TransactionEvent()
    data class ToggleCategoryFilter(val categoryId: Long) : TransactionEvent()
    data class ToggleTypeFilter(val type: TransactionType) : TransactionEvent()
    data class UpdateDateRange(val startDate: LocalDateTime?, val endDate: LocalDateTime?) : TransactionEvent()
    data class UpdateSortOrder(val sortOrder: SortOrder) : TransactionEvent()
    object ToggleFilters : TransactionEvent()
    object ClearAllFilters : TransactionEvent()
    object ApplyFilters : TransactionEvent()
}

/**
 * Events for transaction form
 */
sealed class TransactionFormEvent {
    data class LoadTransaction(val transactionId: Long) : TransactionFormEvent()
    data class UpdateAmount(val amount: String) : TransactionFormEvent()
    data class UpdateType(val type: TransactionType) : TransactionFormEvent()
    data class UpdateCategory(val category: Category) : TransactionFormEvent()
    data class UpdateMerchant(val merchant: String) : TransactionFormEvent()
    data class UpdateDescription(val description: String) : TransactionFormEvent()
    data class UpdateDate(val date: LocalDateTime) : TransactionFormEvent()
    data class UpdateAccount(val account: Account) : TransactionFormEvent()
    object SaveTransaction : TransactionFormEvent()
    object LoadFormData : TransactionFormEvent()
}

/**
 * Events for transfer form
 */
sealed class TransferFormEvent {
    data class UpdateAmount(val amount: String) : TransferFormEvent()
    data class UpdateFromAccount(val account: Account) : TransferFormEvent()
    data class UpdateToAccount(val account: Account) : TransferFormEvent()
    data class UpdateDescription(val description: String) : TransferFormEvent()
    data class UpdateDate(val date: LocalDateTime) : TransferFormEvent()
    object SaveTransfer : TransferFormEvent()
    object LoadAccounts : TransferFormEvent()
}