package com.spendshot.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spendshot.android.data.SettingsEntity
import com.spendshot.android.data.Theme
import com.spendshot.android.data.repository.SettingsRepository
import com.spendshot.android.utils.GitHubUpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: SettingsRepository
) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow<SettingsEntity?>(null)
    val settings = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    private val _updateMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val updateMessage = _updateMessage.asSharedFlow()

    private val _updateInfo = MutableStateFlow<GitHubUpdateChecker.UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    fun checkForUpdate() {
        _isCheckingUpdate.value = true
        viewModelScope.launch {
            try {
                val info = GitHubUpdateChecker.checkForUpdate(getApplication())
                if (info != null) {
                    _updateInfo.value = info
                } else {
                    _updateMessage.emit("App is up to date")
                }
            } catch (e: Exception) {
                _updateMessage.emit("Failed to check for updates")
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    init {
        viewModelScope.launch {
            repository.settings.collect { settingsEntity ->
                _settings.value = settingsEntity ?: SettingsEntity(
                    monthlySalary = 0.0,
                    salaryCreditDate = 1,
                    biometricAuthEnabled = false,
                    theme = null // The original theme is null (System) for a new user
                )
                _isLoading.value = false
            }
        }
    }

    fun updateSettings(
        monthlySalary: Double,
        salaryCreditDate: Int,
        biometricAuthEnabled: Boolean
    ) {
        viewModelScope.launch {
            val currentSettings = settings.first() ?: SettingsEntity(
                monthlySalary = 0.0,
                salaryCreditDate = 1,
                biometricAuthEnabled = false,
                theme = null
            )

            val newSettings = currentSettings.copy(
                monthlySalary = monthlySalary,
                salaryCreditDate = salaryCreditDate,
                biometricAuthEnabled = biometricAuthEnabled
            )
            repository.updateSettings(newSettings)
        }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            val currentSettings = settings.first() ?: return@launch
            val newSettings = currentSettings.copy(theme = theme)
            repository.updateSettings(newSettings)
        }
    }
    
    fun isOnboardingComplete(): Boolean {
        return repository.isOnboardingComplete()
    }
    
    fun setOnboardingComplete(complete: Boolean) {
        repository.setOnboardingComplete(complete)
    }
}
