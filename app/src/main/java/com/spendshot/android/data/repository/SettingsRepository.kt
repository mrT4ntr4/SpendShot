package com.spendshot.android.data.repository

import android.content.SharedPreferences
import com.spendshot.android.data.AppDao
import com.spendshot.android.data.SettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val appDao: AppDao,
    private val prefs: SharedPreferences
) {

    val settings: Flow<SettingsEntity?> = appDao.getSettings()

    suspend fun updateSettings(settings: SettingsEntity) {
        appDao.insertSettings(settings)
    }

    // Onboarding state
    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean("onboarding_complete", false)
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
    }
}
