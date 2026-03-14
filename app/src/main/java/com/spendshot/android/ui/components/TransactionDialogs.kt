package com.spendshot.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.data.CategoryEntity
import com.spendshot.android.utils.ParsedReceipt
import com.spendshot.android.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddTransactionDialog(
    transactionToEdit: TransactionEntity?,
    initialReceipt: ParsedReceipt?,
    viewModel: MainViewModel,
    availableCategories: List<CategoryEntity>,
    onAddCategoryClick: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    // Simplified: Hardcode the old list as strings for now to fix compile, dynamic fetching requires passing list.
    // Let's pass common categories strings.
    // val commonCategories = listOf("Food", "Travel", "Bills", "Shopping", "Entertainment", "Health", "Groceries", "Other")
    
    var amount by remember {

        mutableStateOf(
            transactionToEdit?.amount?.toString()
                ?: initialReceipt?.amount?.takeIf { it > 0 }?.toString() ?: ""
        )
    }
    var merchant by remember {
        mutableStateOf(transactionToEdit?.merchant ?: initialReceipt?.merchant ?: "")
    }
    var note by remember {
        mutableStateOf(transactionToEdit?.note ?: initialReceipt?.note ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(transactionToEdit?.category ?: initialReceipt?.category ?: "Other")
    }
    var selectedType by remember {
        mutableStateOf(transactionToEdit?.type ?: initialReceipt?.transactionType ?: TransactionType.EXPENSE)
    }
    var selectedDate by remember {
        mutableStateOf(transactionToEdit?.timestamp ?: Date())
    }
    
    // App selection state - user can change if classifier was wrong
    var selectedApp by remember {
        mutableStateOf(transactionToEdit?.detectedApp ?: initialReceipt?.detectedAppLabel)
    }
    var showAppSelector by remember { mutableStateOf(false) }
    val availableApps = listOf("GPay", "Swiggy", "Zomato", "PhonePe")

    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(note.isNotBlank()) }

    LaunchedEffect(Unit) {
        if (merchant.isNotBlank() && transactionToEdit == null) {
            viewModel.getCategoryForMerchant(merchant) { category ->
                selectedCategory = category // category is now String
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onTertiary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.tertiary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onTertiary
                )
            )
        }
    }

    val dialogTitle = if (transactionToEdit != null) "Edit Transaction" else "Add Transaction"

    Surface(
        modifier = Modifier.width(340.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tappable App Icon with dropdown selector - styled as interactive chip
                Box {
                    Surface(
                        onClick = { showAppSelector = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AppIcon(
                                label = selectedApp ?: "Unknown",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select App",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showAppSelector,
                        onDismissRequest = { showAppSelector = false }
                    ) {
                        availableApps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app) },
                                onClick = {
                                    selectedApp = app
                                    showAppSelector = false
                                },
                                leadingIcon = {
                                    AppIcon(
                                        label = app,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        // Option to clear/set to unknown
                        DropdownMenuItem(
                            text = { Text("None / Other") },
                            onClick = {
                                selectedApp = null
                                showAppSelector = false
                            },
                            leadingIcon = {
                                AppIcon(
                                    label = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(dialogTitle, style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { if (it.length <= 25) merchant = it },
                        label = { Text("Merchant / Title") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.length <= 10) amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = showNoteField) {
                    Column {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                if (!showNoteField) {
                    TextButton(
                        onClick = { showNoteField = true },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Note")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.EditCalendar, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        leadingIcon = if (selectedType == TransactionType.EXPENSE) {
                            { Icon(Icons.Default.Check, null) }
                        } else null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        leadingIcon = if (selectedType == TransactionType.INCOME) {
                            { Icon(Icons.Default.Check, null) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Category:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val haptics = LocalHapticFeedback.current
                    availableCategories.forEach { category ->
                        key(category.name) {
                            var expanded by remember { mutableStateOf(false) }
                            val interactionSource = remember { MutableInteractionSource() }
                            
                            Box {
                                FilterChip(
                                    selected = selectedCategory == category.name,
                                    onClick = {}, // Handled by overlay
                                    label = { Text(category.name.replace("_", " ")) },
                                    modifier = Modifier.padding(end = 6.dp, bottom = 0.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                                    ),
                                    leadingIcon = if (selectedCategory == category.name) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    interactionSource = interactionSource
                                )
                                
                                // Overlay for gesture handling
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(end = 6.dp) // Match margin of chip
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null, // FilterChip handles indication
                                            onClick = { selectedCategory = category.name },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                expanded = true
                                            }
                                        )
                                )

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            expanded = false
                                            onEditCategory(category)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            expanded = false
                                            onDeleteCategory(category)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                    FilterChip(
                        selected = false,
                        onClick = onAddCategoryClick,
                        label = { Text("Add") },
                        modifier = Modifier.padding(end = 6.dp, bottom = 0.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add Category", modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val finalAmount = amount.toDoubleOrNull()
                        if (finalAmount != null && merchant.isNotBlank()) {
                            val newTransaction = TransactionEntity(
                                id = transactionToEdit?.id ?: 0,
                                amount = finalAmount,
                                merchant = merchant,
                                note = note,
                                category = selectedCategory,
                                type = selectedType,
                                timestamp = selectedDate,
                                sourceApp = selectedApp ?: transactionToEdit?.sourceApp,
                                isRecurring = false,
                                detectedApp = selectedApp
                            )
                            onSave(newTransaction)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save")
                }
            }
        }
    }
}
