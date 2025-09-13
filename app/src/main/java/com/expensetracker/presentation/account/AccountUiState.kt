package com.expensetracker.presentation.account

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import java.math.BigDecimal

/**
 * UI state for account management screens
 */
data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showActiveOnly: Boolean = true,
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val totalCreditCardBalance: BigDecimal = BigDecimal.ZERO
)

data class AccountFormUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val accountId: Long? = null,
    val bankName: String = "",
    val accountType: AccountType = AccountType.CHECKING,
    val accountNumber: String = "",
    val nickname: String = "",
    val currentBalance: String = "0.00",
    val isActive: Boolean = true,
    val error: String? = null,
    val validationErrors: AccountValidationErrors = AccountValidationErrors(),
    val isSaveEnabled: Boolean = false
)

data class AccountValidationErrors(
    val bankNameError: String? = null,
    val accountNumberError: String? = null,
    val nicknameError: String? = null,
    val balanceError: String? = null
)

data class AccountDetailUiState(
    val account: Account? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentTransactions: List<Any> = emptyList(), // Will be Transaction objects when implemented
    val monthlySpending: BigDecimal = BigDecimal.ZERO,
    val monthlyIncome: BigDecimal = BigDecimal.ZERO
)

sealed class AccountUiEvent {
    object LoadAccounts : AccountUiEvent()
    object ToggleActiveFilter : AccountUiEvent()
    data class SelectAccount(val accountId: Long) : AccountUiEvent()
    data class DeactivateAccount(val accountId: Long) : AccountUiEvent()
    data class ReactivateAccount(val accountId: Long) : AccountUiEvent()
    data class DeleteAccount(val accountId: Long) : AccountUiEvent()
    object CreateNewAccount : AccountUiEvent()
    data class EditAccount(val accountId: Long) : AccountUiEvent()
    object ClearError : AccountUiEvent()
}

sealed class AccountFormEvent {
    data class BankNameChanged(val bankName: String) : AccountFormEvent()
    data class AccountTypeChanged(val accountType: AccountType) : AccountFormEvent()
    data class AccountNumberChanged(val accountNumber: String) : AccountFormEvent()
    data class NicknameChanged(val nickname: String) : AccountFormEvent()
    data class BalanceChanged(val balance: String) : AccountFormEvent()
    object SaveAccount : AccountFormEvent()
    object CancelEdit : AccountFormEvent()
    object ClearError : AccountFormEvent()
}