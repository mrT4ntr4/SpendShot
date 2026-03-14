package com.spendshot.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendshot.android.R
import com.spendshot.android.viewmodels.SettingsViewModel
import androidx.compose.ui.unit.IntSize
// No date imports are needed here because the logic is in the ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onOnboardingComplete: () -> Unit
) {
    var salary by remember { mutableStateOf("") }
    var salaryDate by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var biometricAuthEnabled by remember { mutableStateOf(false) }

    // --- Animation State ---
    var animationStarted by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000),
        label = "scaleAnimation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alphaAnimation"
    )

    LaunchedEffect(Unit) {
        animationStarted = true
    }
    // --- End of Animation State ---

    val focusManager = LocalFocusManager.current
    var size by remember { mutableStateOf(IntSize.Zero) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 32.dp)
                    .alpha(alpha)
                    .scale(scale)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size = it }
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F2A1F),
                                    Color(0xFF0042ED)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width.toFloat(), size.height.toFloat())
                            ),
                            shape = RoundedCornerShape(60.dp)
                        ),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(60.dp)
                )  {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize().scale(1.5f) // Add this padding. Decrease it to make the icon larger.
                    )

                }

            }

            Text("Lets Get Started!", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = salary,
                onValueChange = { if (it.length <= 10) salary = it.filter { char -> char.isDigit() } },
                label = { Text("What is your monthly salary?") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }
                ) {
                    OutlinedTextField(
                        value = salaryDate,
                        onValueChange = {},
                        label = { Text("Day of month you receive salary") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        for (day in 1..31) {
                            DropdownMenuItem(
                                text = { Text(text = day.toString()) },
                                onClick = {
                                    salaryDate = day.toString()
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // This is how you use the new component
            SettingsSwitchRow(
                icon = Icons.Outlined.Fingerprint, // A great icon for biometrics
                title = "Enable Biometric Lock",
                isChecked = biometricAuthEnabled,
                onCheckedChange = { isChecked ->
                    biometricAuthEnabled = isChecked
                }
            )
            // --- CORRECTED CODE ---
            Button(
                onClick = {
                    val salaryDouble = salary.toDoubleOrNull() ?: 0.0
                    val dateInt = salaryDate.toIntOrNull() ?: 1

                    // Call the updated ViewModel function.
                    // The ViewModel now contains the logic to calculate the correct period start date
                    // and publish it to the `salaryPeriodStartDate` state flow.
                    // We also pass 'true' for the biometric setting, assuming it defaults to enabled.
                    settingsViewModel.updateSettings(salaryDouble, dateInt, biometricAuthEnabled)

                    // Proceed with completing the onboarding process.
                    onOnboardingComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = salary.isNotBlank() && salaryDate.isNotBlank() && salary.toDoubleOrNull() != 0.0
            ) {
                Text("Get Started")
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$title icon",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
