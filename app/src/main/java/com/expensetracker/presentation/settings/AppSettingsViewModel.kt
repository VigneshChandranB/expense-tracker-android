package com.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.usecase.settings.GetAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.UpdateAppSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for app settings screen
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val getAppSettingsUseCase: GetAppSettingsUseCase,
    private val updateAppSettingsUseCase: UpdateAppSettingsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = getAppSettingsUseCase()
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.settings.copy(themeMode = themeMode)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.settings.copy(currencyCode = currencyCode)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateDateFormat(dateFormat: String) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.settings.copy(dateFormat = dateFormat)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateAutoCategorization(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.settings.copy(autoCategorizationEnabled = enabled)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateBiometricAuth(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.settings.copy(biometricAuthEnabled = enabled)
                updateAppSettingsUseCase(updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}