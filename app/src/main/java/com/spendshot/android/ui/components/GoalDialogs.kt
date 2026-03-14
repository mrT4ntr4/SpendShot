package com.spendshot.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spendshot.android.utils.toTitleCase
import com.spendshot.android.data.GoalEntity
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Flag
import androidx.compose.foundation.background
import coil3.request.crossfade

@Composable
fun AddGoalDialog(
    existingGoalNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    
    val nameExists = existingGoalNames.any { it.equals(name.trim(), ignoreCase = true) }

    // Icon detection
    val iconUrl = remember(name) {
        if (name.isNotBlank()) {
            "https://unpkg.com/lucide-static@latest/icons/${name.trim().lowercase()}.svg"
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add Savings Goal")
                if (iconUrl != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        coil3.compose.AsyncImage(
                            model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(iconUrl)
                                .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    label = { Text("Goal Name (e.g., Bike, Car)") },
                    singleLine = true,
                    isError = nameExists,
                    supportingText = if (nameExists) {
                        { Text("A goal with this name already exists", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { 
                        targetAmount = it
                    },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                
                if (iconUrl != null) {
                    Text(
                        text = "Icon will be auto-detected if available online.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    if (name.isNotBlank() && amount != null && !nameExists) {
                        onSave(
                            GoalEntity(
                                name = name.trim().toTitleCase(),
                                targetAmount = amount,
                                savedAmount = 0.0,
                                icon = iconUrl // Save the icon URL
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && targetAmount.toDoubleOrNull() != null && !nameExists
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddFundsDialog(
    goalName: String,
    maxAmount: Double = Double.MAX_VALUE,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val amountDouble = amount.toDoubleOrNull()
    val isValid = amountDouble != null && amountDouble > 0 && amountDouble <= maxAmount
    val exceedsMax = amountDouble != null && amountDouble > maxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Funds to $goalName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.length <= 10) amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = exceedsMax,
                    supportingText = if (exceedsMax) {
                        { Text("Cannot exceed remaining amount", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                if (maxAmount < Double.MAX_VALUE) {
                    Text(
                        text = "Remaining to goal: ${currencyFormat.format(maxAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(amountDouble!!)
                        onDismiss()
                    }
                },
                enabled = isValid
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditGoalDialog(
    goal: GoalEntity,
    savingsHistory: List<Pair<String, Double>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var targetAmount by remember { mutableStateOf(goal.targetAmount.toInt().toString()) }

    // Icon detection logic
    val iconUrl = remember(name) {
         if (name.isNotBlank()) {
             "https://unpkg.com/lucide-static@latest/icons/${name.trim().lowercase()}.svg"
         } else {
             null
         }
    }
    
    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val maxSaving = savingsHistory.maxOfOrNull { it.second } ?: 1.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    label = { Text("Goal Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.length <= 10) targetAmount = it },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                 if (iconUrl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Goal Icon: ", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                             coil3.compose.SubcomposeAsyncImage(
                                model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(iconUrl)
                                    .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                error = {
                                    Icon(imageVector = Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                }
                
                // Monthly Savings History
                if (savingsHistory.isNotEmpty() && savingsHistory.any { it.second > 0 }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Monthly Savings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        savingsHistory.forEach { (month, amount) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val barHeight = if (maxSaving > 0) (amount / maxSaving * 50).dp else 0.dp
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(barHeight.coerceAtLeast(4.dp))
                                        .background(
                                            if (amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    if (name.isNotBlank() && amount != null) {
                        onSave(goal.copy(name = name.trim().toTitleCase(), targetAmount = amount, icon = iconUrl))
                        onDismiss()
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

