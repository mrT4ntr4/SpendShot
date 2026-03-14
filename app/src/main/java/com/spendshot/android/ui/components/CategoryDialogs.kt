package com.spendshot.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.CategoryBudget
import com.spendshot.android.data.CategoryEntity
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import coil3.request.crossfade
import com.spendshot.android.utils.toTitleCase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    initialCategory: CategoryEntity? = null,
    categories: List<CategoryEntity>,
    maxLimit: Double,
    onDismiss: () -> Unit,
    onSave: (CategoryBudget) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var limitAmount by remember { mutableStateOf("") }
    var sliderValue by remember { mutableStateOf(0f) }
    var expanded by remember { mutableStateOf(false) }

    // Ensure slider range isn't 0
    val sliderRangeEnd = maxOf(maxLimit.toFloat(), 5000f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name?.removePrefix("Goal: ")?.replace("_", " ") ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (category.icon != null) {
                                            coil3.compose.SubcomposeAsyncImage(
                                                model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                    .data(category.icon)
                                                    .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                        Text(category.name.removePrefix("Goal: ").replace("_", " "))
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { 
                        if (it.length <= 10) {
                            limitAmount = it
                            // Update slider if valid number and within range
                            val num = it.toDoubleOrNull()?.toFloat() ?: 0f
                            sliderValue = num.coerceIn(0f, sliderRangeEnd)
                        }
                    },
                    label = { Text("Monthly Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                // Slider Value Display
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Adjust Limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN")).format(sliderValue),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { 
                        sliderValue = it
                        limitAmount = it.toInt().toString()
                    },
                    valueRange = 0f..sliderRangeEnd,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = limitAmount.toDoubleOrNull()
                    if (selectedCategory != null && amount != null) {
                        val calendar = java.util.Calendar.getInstance()
                        val month = calendar.get(java.util.Calendar.MONTH)
                        val year = calendar.get(java.util.Calendar.YEAR)
                        val monthYear = "${month.toString().padStart(2, '0')}-$year" // Saved as 00-2023 (0-indexed month)? 
                        // Wait, previous code used month+1. Let's stick to what was there or consistent format.
                        // Checked older code: `val month = calendar.get(java.util.Calendar.MONTH) + 1` was used in lines 130.
                        // I will preserve that logic in the replacement if I am replacing that block, but I am only replacing the text field part?
                        // Ah, I am replacing the text block AND confirm button block because they are adjacent?
                        // No, I can replace the `text = { ... }` content.
                        
                        // Let's replace the whole `text = { ... }` block to insert the slider.
                        // I need to add `var sliderValue by remember { mutableStateOf(0f) }` at top of function.
                        
                        onSave(
                            CategoryBudget(
                                category = selectedCategory!!.name, // Store name string
                                limitAmount = amount,
                                monthYear = "${(month + 1).toString().padStart(2, '0')}-$year"
                            )
                        )
                        onDismiss()
                    }
                }
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
fun AddCategoryDialog(
    existingCategories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    // Get next available distinct color
    val usedColors = remember(existingCategories) { 
        existingCategories.map { it.color }.toSet() 
    }
    val color = remember(usedColors) { 
        com.spendshot.android.utils.CategoryColors.getNextAvailableColor(usedColors)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                CategoryIconPreview(name = name)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val iconUrl = "https://unpkg.com/lucide-static@latest/icons/${name.trim().lowercase()}.svg"
                        onSave(CategoryEntity(name = name.trim().toTitleCase(), color = color, icon = iconUrl))
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

@Composable
fun CategoryIconPreview(name: String) {
    if (name.isNotBlank()) {
         val iconUrl = "https://unpkg.com/lucide-static@latest/icons/${name.trim().lowercase()}.svg"
         Row(verticalAlignment = Alignment.CenterVertically) {
             Text("Preview: ", style = MaterialTheme.typography.bodySmall)
             Spacer(modifier = Modifier.width(8.dp))
             Box(
                 modifier = Modifier.size(24.dp),
                 contentAlignment = Alignment.Center
             ) {
                 coil3.compose.SubcomposeAsyncImage(
                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(iconUrl)
                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    error = {
                        // Fallback to a dot or something
                         Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape))
                    },
                    loading = {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                    }
                )
             }
         }
    }
}

@Composable
fun EditCategoryDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit, // Rename
    onDelete: () -> Unit,
    onSetBudget: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Edit Category")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Preview
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(16.dp)
                ) {
                    CategoryIconPreview(name = name)
                }
                
                Spacer(Modifier.height(16.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Additional Actions
                OutlinedButton(
                    onClick = onSetBudget,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set Budget Limit")
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Category")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && name != category.name) {
                        onSave(category.copy(name = name.trim().toTitleCase()))
                    }
                    onDismiss()
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
