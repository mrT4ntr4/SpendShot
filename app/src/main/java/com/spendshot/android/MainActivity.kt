package com.spendshot.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.spendshot.android.ui.navigation.AppNavigation
import com.spendshot.android.ui.theme.SpendShotTheme
import com.spendshot.android.utils.BiometricAuthenticator
import com.spendshot.android.viewmodels.MainViewModel
import com.spendshot.android.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var biometricAuthenticator: BiometricAuthenticator
    // Use MutableState to trigger recomposition in AppNavigation
    private var isAuthenticated = androidx.compose.runtime.mutableStateOf(false)
    private var isUiReady = false
    private var isShareFlow = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Biometrics
        biometricAuthenticator = BiometricAuthenticator(this)

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            imageUri?.let {
                mainViewModel.processReceipt(it)
                isShareFlow.value = true
                isAuthenticated.value = true // Skip auth for share flow
            }
        }

        // Splash Screen Condition
        // We hold the splash screen until:
        // 1. Data is loaded (settingsViewModel.isLoading)
        // 2. UI is ready (isUiReady)
        // 3. User is Authenticated (isAuthenticated) OR it's a Share Flow (auth skipped)
        splashScreen.setKeepOnScreenCondition { 
            settingsViewModel.isLoading.value || 
            !isUiReady || 
            (!isAuthenticated.value && !isShareFlow.value) 
        }

        if (!isShareFlow.value) {
             lifecycleScope.launch {
                 val settings = settingsViewModel.settings.first { it != null }
                 if (settingsViewModel.isOnboardingComplete()) {
                      val biometricEnabled = settings?.biometricAuthEnabled ?: false
                      if (biometricEnabled) {
                          biometricAuthenticator.promptForAuthentication(
                              onSuccess = { 
                                  // Ensure main thread for state update
                                  runOnUiThread {
                                      isAuthenticated.value = true
                                  }
                              },
                              onFailure = { finish() },
                              onError = { _, _ -> finish() }
                          )
                      } else {
                          isAuthenticated.value = true
                      }
                 } else {
                      isAuthenticated.value = true // No auth for onboarding
                 }
             }
        }

        setContent {
            val settings = settingsViewModel.settings.collectAsState(initial = null)
            SpendShotTheme(appTheme = settings.value?.theme) {
                 // We pass isAuthenticated state.
                
                val view = androidx.compose.ui.platform.LocalView.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { 
                            // Post with delay to ensure WindowManager has time to apply Status Bar colors
                            view.postDelayed({ isUiReady = true }, 100)
                        }
                ) {
                    AppNavigation(
                        isShareFlow = isShareFlow.value,
                        isAuthenticated = isAuthenticated.value,
                        onFinish = { finish() },
                        onRequestAuthentication = { onSuccess ->
                            val isAuthEnabled = settings.value?.biometricAuthEnabled == true
                            if (isAuthEnabled) {
                                biometricAuthenticator.promptForAuthentication(
                                    onSuccess = {
                                        // Unlock app by disabling share flow mode
                                        isShareFlow.value = false
                                        onSuccess()
                                    },
                                    onFailure = { 
                                         // Optional: Toast "Authentication failed"
                                    },
                                    onError = { _, _ -> }
                                )
                            } else {
                                // Auth disabled, just proceed
                                isShareFlow.value = false
                                onSuccess()
                            }
                        }
                    )
                }
            }
        }
    }
}
