package com.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.DataManagementSettings
import com.expensetracker.domain.usecase.settings.GetAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.UpdateAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.ManageDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for data management screen
 */
@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val getAppSettingsUseCase: GetAppSettingsUseCase,
    private val updateAppSettingsUseCase: UpdateAppSettingsUseCase,
    private val manageDataUseCase: ManageDataUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                combine(
                    getAppSettingsUseCase(),
                    manageDataUseCase.getDataManagementSettings()
                ) { appSettings, dataSettings ->
                    _uiState.value = _uiState.value.copy(
                        appSettings = appSettings,
                        dataManagementSettings = dataSettings,
                        isLoading = false,
                        error = null
                    )
                }.collect { }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun updateSmsPermission(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(smsPermissionEnabled = enabled)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateAutoDelete(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.dataManagementSettings.copy(
                    autoDeleteOldTransactions = enabled
                )
                manageDataUseCase.updateDataManagementSettings(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateRetentionPeriod(months: Int) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.dataManagementSettings.copy(
                    retentionPeriodMonths = months
                )
                manageDataUseCase.updateDataManagementSettings(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteAllData() {
        viewModelScope.launch {
            try {
                manageDataUseCase.deleteAllUserData()
                _uiState.value = _uiState.value.copy(
                    message = "All data has been deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

/**
 * UI state for data management screen
 */
data class DataManagementUiState(
    val appSettings: AppSettings = AppSettings(),
    val dataManagementSettings: DataManagementSettings = DataManagementSettings(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)