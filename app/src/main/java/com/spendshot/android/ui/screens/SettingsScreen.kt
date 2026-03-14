package com.spendshot.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.Theme
import com.spendshot.android.ui.components.TitledDivider
import com.spendshot.android.viewmodels.SettingsViewModel


// Data class to hold our specific version info
data class AppVersionInfo(val versionName: String?, val versionCode: Long)

@Composable
fun getAppVersionInfo(): AppVersionInfo {
    val context = LocalContext.current
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        AppVersionInfo(versionName, versionCode)
    } catch (e: Exception) {
        // In case of an error, return default values
        AppVersionInfo("N/A", 0L)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsState()

    // Call the composable function to get version info
    val versionInfo = getAppVersionInfo()

    var monthlySalary by remember { mutableStateOf("") }
    var salaryCreditDate by remember { mutableStateOf(1) }
    var biometricAuthEnabled by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf<Theme?>(null) }
    var isDateMenuExpanded by remember { mutableStateOf(false) }
    val systemTheme = if (isSystemInDarkTheme()) Theme.DARK else Theme.LIGHT
    


    LaunchedEffect(settings) {
        settings?.let {
            monthlySalary = if (it.monthlySalary == 0.0) "" else it.monthlySalary.toBigDecimal().toPlainString()
            salaryCreditDate = it.salaryCreditDate
            biometricAuthEnabled = it.biometricAuthEnabled
            selectedTheme = it.theme ?: systemTheme
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = monthlySalary,
                            onValueChange = {
                                if (it.length <= 10 && it.matches(Regex("""^\d*\.?\d*"""))) {
                                    monthlySalary = it
                                }
                            },
                            label = { Text("Monthly Salary") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = isDateMenuExpanded,
                            onExpandedChange = { isDateMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = salaryCreditDate.toString(),
                                onValueChange = { },
                                label = { Text("Salary Credit Date") },
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDateMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = isDateMenuExpanded,
                                onDismissRequest = { isDateMenuExpanded = false }
                            ) {
                                for (day in 1..31) {
                                    DropdownMenuItem(
                                        text = { Text(text = day.toString()) },
                                        onClick = {
                                            salaryCreditDate = day
                                            isDateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("App Lock") },
                        supportingContent = { Text("Use fingerprint, face, or device PIN to open app") },
                        leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = biometricAuthEnabled,
                                onCheckedChange = { biometricAuthEnabled = it }
                            )
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val themes = listOf(Theme.LIGHT, Theme.DARK)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themes.forEachIndexed { index, theme ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                                    onClick = {
                                        selectedTheme = theme
                                        settingsViewModel.updateTheme(theme)
                                    },
                                    selected = selectedTheme == theme
                                ) {
                                    Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val salary = monthlySalary.toDoubleOrNull() ?: 0.0
                        if (salary > 0.0) {
                            settingsViewModel.updateSettings(salary, salaryCreditDate, biometricAuthEnabled)
                            Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Salary must be greater than zero.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Settings")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Version ${versionInfo.versionName} (${versionInfo.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
