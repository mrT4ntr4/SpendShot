package com.spendshot.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.SettingsEntity
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.data.CategoryEntity
import com.spendshot.android.utils.AppClassifier.Classification
import com.spendshot.android.utils.ParsedReceipt
import com.spendshot.android.viewmodels.MainViewModel
import com.spendshot.android.viewmodels.SettingsViewModel
import com.spendshot.android.ui.components.ProcessingDialog
import com.spendshot.android.ui.components.AddGoalDialog
import com.spendshot.android.ui.components.AddBudgetDialog
import com.spendshot.android.ui.components.AddCategoryDialog
import com.spendshot.android.ui.components.EditCategoryDialog
import com.spendshot.android.ui.components.AddTransactionDialog
import com.spendshot.android.ui.components.UpdateAvailableDialog
import com.spendshot.android.ui.composables.SuccessAnimation
import com.spendshot.android.ui.screens.DashboardScreen
import com.spendshot.android.ui.screens.ReportsScreen
import com.spendshot.android.ui.screens.TransactionsScreen
import com.spendshot.android.ui.screens.GoalsScreen
import com.spendshot.android.viewmodels.BudgetViewModel
import com.spendshot.android.viewmodels.GoalsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    budgetViewModel: BudgetViewModel,
    goalsViewModel: GoalsViewModel,
    isShareFlow: Boolean,
    onNavigateToSettings: () -> Unit,
    onShareFlowComplete: () -> Unit,
    onRequestAuthentication: (onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    val transactions by viewModel.transactions.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = null)
    val parsedReceipt by viewModel.parsedReceipt.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    
    val pagerState = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()

    var receiptForDialog by remember { mutableStateOf<ParsedReceipt?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var successReceipt by remember { mutableStateOf<ParsedReceipt?>(null) }
    
    // UI State for Goals Screen
    var goalsTabSelected by remember { mutableStateOf(0) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var categoryForBudget by remember { mutableStateOf<CategoryEntity?>(null) } // Used for both Budget and Edit

    // Explicit visibility state to decouple from data clearing
    var isTransactionDialogOpen by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isTransactionDialogOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "FAB Rotation"
    )

    val onDismiss = {
        isTransactionDialogOpen = false
        viewModel.clearParsedReceipt()
        if (isShareFlow) {
            onShareFlowComplete()
        }
    }

    val isCheckingUpdate by settingsViewModel.isCheckingUpdate.collectAsState()
    val updateInfo by settingsViewModel.updateInfo.collectAsState()

    if (isProcessing) {
        ProcessingDialog(message = "Processing Screenshot...")
    }

    if (isCheckingUpdate) {
        ProcessingDialog(message = "Checking for Updates...")
    }
    
    // GitHub update available dialog
    if (updateInfo != null) {
        UpdateAvailableDialog(
            latestVersion = updateInfo!!.latestVersion,
            releaseNotes = updateInfo!!.releaseNotes,
            downloadUrl = updateInfo!!.downloadUrl,
            onDismiss = { settingsViewModel.dismissUpdateDialog() }
        )
    }

    LaunchedEffect(Unit) {
        settingsViewModel.updateMessage.collect { message: String ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(parsedReceipt) {
        if (parsedReceipt != null) {
            // Show toast if manual entry is required (zero amount or unknown merchant)
            parsedReceipt?.manualEntryReason?.let { reason ->
                android.widget.Toast.makeText(context, reason, android.widget.Toast.LENGTH_SHORT).show()
            }
            
            val hasCategory = parsedReceipt?.category != null && parsedReceipt?.category != "Other"
            if (parsedReceipt?.category != null) {
                successReceipt = parsedReceipt
                viewModel.clearParsedReceipt()
            } else {
                receiptForDialog = parsedReceipt
                editingTransaction = null // Ensure we aren't in edit mode
                isTransactionDialogOpen = true
                viewModel.clearParsedReceipt()
            }
        }
    }


    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val isAddScreen = pagerState.currentPage == 3
            val fabIcon = if (isAddScreen) Icons.Default.Add else Icons.Default.Add
            val fabContentDescription = if (isAddScreen) "Add Goal" else "Add Transaction"
            
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 3) { // Goals Screen
                        showAddGoalDialog = true
                    } else {
                        if (isTransactionDialogOpen) {
                            onDismiss()
                        } else {
                            receiptForDialog = ParsedReceipt(
                                amount = 0.0,
                                merchant = "",
                                transactionType = TransactionType.EXPENSE,
                                category = "Food", // Default string
                                detectedAppLabel = null
                            )
                            editingTransaction = null
                            isTransactionDialogOpen = true
                        }
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    fabIcon,
                    contentDescription = fabContentDescription,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.rotate(if (isAddScreen) 0f else rotation)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.scrollToPage(0) } },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.scrollToPage(1) } },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Categories") },
                    label = { Text("Categories") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.scrollToPage(2) } },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Transactions") },
                    label = { Text("Transactions") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = { scope.launch { pagerState.scrollToPage(3) } },
                    icon = { Icon(Icons.Default.Flag, contentDescription = "Goals") },
                    label = { Text("Goals") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { padding ->
        if (!isShareFlow) {
            MainScaffold(
                transactions = transactions,
                settings = settings,
                pagerState = pagerState,
                settingsViewModel = settingsViewModel,
                mainViewModel = viewModel,
                budgetViewModel = budgetViewModel,
                goalsViewModel = goalsViewModel,
                onTransactionClick = { transaction ->
                    editingTransaction = transaction
                    receiptForDialog = null // Ensure receipt is clear
                    isTransactionDialogOpen = true
                },
                modifier = Modifier.padding(padding),
                onNavigateToSettings = onNavigateToSettings,
                onCategoryClick = { category ->
                    categoryForBudget = category
                    showEditCategoryDialog = true
                }
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {}
        }
    }

    AnimatedVisibility(
        visible = isTransactionDialogOpen,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 168.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = isTransactionDialogOpen,
            enter = expandIn(
                expandFrom = Alignment.BottomEnd,
                animationSpec = tween(400)
            ),
            exit = shrinkOut(
                shrinkTowards = Alignment.BottomEnd,
                animationSpec = tween(400)
            )
        ) {
            val allCategories by budgetViewModel.allCategories.collectAsState(initial = emptyList())
            AddTransactionDialog(
                transactionToEdit = editingTransaction,
                initialReceipt = receiptForDialog,
                viewModel = viewModel,
                availableCategories = allCategories,
                onAddCategoryClick = { showAddCategoryDialog = true },
                onEditCategory = { category ->
                    categoryForBudget = category
                    showEditCategoryDialog = true
                },
                onDeleteCategory = { category ->
                     budgetViewModel.deleteCategory(category)
                },
                onDismiss = onDismiss,
                onSave = {
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(it)
                    } else {
                        // Pass original OCR merchant to learn corrections when user edits
                        viewModel.addTransaction(it, receiptForDialog?.originalOcrMerchant)
                    }

                    successReceipt = ParsedReceipt(
                        amount = it.amount,
                        merchant = it.merchant,
                        transactionType = it.type,
                        category = it.category,
                        detectedAppLabel = it.detectedApp
                    )
                }
            )
        }
    }

    if (successReceipt != null) {
        SuccessAnimation(
            receipt = successReceipt!!,
            onAnimationFinish = {
                successReceipt = null
                onDismiss() // Closes dialog too
            }
        )
    }
    if (showAddGoalDialog) {
        val existingGoalNames = goalsViewModel.goals.collectAsState().value.map { it.name }
        AddGoalDialog(
            existingGoalNames = existingGoalNames,
            onDismiss = { showAddGoalDialog = false },
            onSave = { 
                goalsViewModel.addGoal(it) 
                showAddGoalDialog = false
            }
        )
    }

    if (showAddBudgetDialog && categoryForBudget != null) {
        val categories by budgetViewModel.budgets.collectAsState()
        val categoryEntities = categories.map { it.category }
        val totalIncome by budgetViewModel.totalIncome.collectAsState(initial = 0.0)
        
        AddBudgetDialog(
            initialCategory = categoryForBudget,
            categories = categoryEntities,
            maxLimit = totalIncome,
            onDismiss = { showAddBudgetDialog = false },
            onSave = { 
                budgetViewModel.addBudget(it)
                showAddBudgetDialog = false
            }
        )
    }
    
    if (showAddCategoryDialog) {
        val existingCategories = budgetViewModel.allCategories.collectAsState().value
        AddCategoryDialog(
            existingCategories = existingCategories,
            onDismiss = { showAddCategoryDialog = false },
            onSave = {
                budgetViewModel.addCategory(it)
                showAddCategoryDialog = false
            }
        )
    }
    
    if (showEditCategoryDialog && categoryForBudget != null) {
        EditCategoryDialog(
            category = categoryForBudget!!,
            onDismiss = { showEditCategoryDialog = false },
            onSave = {
                budgetViewModel.renameCategory(categoryForBudget!!.name, it.name)
                showEditCategoryDialog = false
            },
            onDelete = {
                budgetViewModel.deleteCategory(categoryForBudget!!)
                showEditCategoryDialog = false
            },
            onSetBudget = {
                showEditCategoryDialog = false
                showAddBudgetDialog = true
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    transactions: List<TransactionEntity>?,
    settings: SettingsEntity?,
    pagerState: PagerState,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    budgetViewModel: BudgetViewModel,
    goalsViewModel: GoalsViewModel,
    onTransactionClick: (TransactionEntity) -> Unit,
    onNavigateToSettings: () -> Unit,
    onCategoryClick: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // Initialize selectedYear/Month accounting for salary credit date
    // If today is before the salary credit date, we're actually in the previous budget period
    val initialCalendar = remember(settings?.salaryCreditDate) {
        Calendar.getInstance().apply {
            val salaryCreditDate = settings?.salaryCreditDate ?: 1
            if (get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                add(Calendar.MONTH, -1)
            }
        }
    }
    var selectedYear by remember(initialCalendar) { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember(initialCalendar) { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }

    var selectedTransactionIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedGoalIds by remember { mutableStateOf(emptySet<Int>()) }
    val isInTransactionSelectionMode = selectedTransactionIds.isNotEmpty()
    val isInGoalSelectionMode = selectedGoalIds.isNotEmpty()
    val isInSelectionMode = isInTransactionSelectionMode || isInGoalSelectionMode

    BackHandler(enabled = isInSelectionMode) {
        selectedTransactionIds = emptySet()
        selectedGoalIds = emptySet()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 3) {
            // Reset date or logic
        }
    }

    LaunchedEffect(selectedYear, selectedMonth) {
        budgetViewModel.updateDate(selectedYear, selectedMonth)
        goalsViewModel.updateDate(selectedYear, selectedMonth)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isInSelectionMode) {
                val selectionCount = if (isInTransactionSelectionMode) selectedTransactionIds.size else selectedGoalIds.size
                val selectionType = if (isInTransactionSelectionMode) "transaction" else "goal"
                TopAppBar(
                    title = { Text("$selectionCount $selectionType${if (selectionCount > 1) "s" else ""} selected") },
                    navigationIcon = {
                        IconButton(onClick = { 
                            selectedTransactionIds = emptySet()
                            selectedGoalIds = emptySet()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (isInTransactionSelectionMode) {
                                mainViewModel.deleteMultipleTransactions(selectedTransactionIds)
                                selectedTransactionIds = emptySet()
                            } else if (isInGoalSelectionMode) {
                                goalsViewModel.deleteMultipleGoals(selectedGoalIds)
                                selectedGoalIds = emptySet()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        if (pagerState.currentPage in 0..3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val calendar = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, 1)
                                        add(Calendar.MONTH, -1)
                                    }
                                    selectedYear = calendar.get(Calendar.YEAR)
                                    selectedMonth = calendar.get(Calendar.MONTH)
                                }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Budget Period")
                                }

                                DateFilter(
                                    selectedYear = selectedYear,
                                    selectedMonth = selectedMonth,
                                    salaryCreditDate = settings?.salaryCreditDate ?: 1
                                )

                                // Determine the current budget period accounting for salary credit date
                                val salaryCreditDate = settings?.salaryCreditDate ?: 1
                                val currentBudgetPeriod = Calendar.getInstance().apply {
                                    if (get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                                        add(Calendar.MONTH, -1)
                                    }
                                }
                                val isCurrentOrFutureMonth = selectedYear > currentBudgetPeriod.get(Calendar.YEAR) ||
                                        (selectedYear == currentBudgetPeriod.get(Calendar.YEAR) && selectedMonth >= currentBudgetPeriod.get(Calendar.MONTH))

                                IconButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            set(selectedYear, selectedMonth, 1)
                                            add(Calendar.MONTH, 1)
                                        }
                                        selectedYear = calendar.get(Calendar.YEAR)
                                        selectedMonth = calendar.get(Calendar.MONTH)
                                    },
                                    enabled = !isCurrentOrFutureMonth
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Budget Period")
                                }
                            }
                        }

                    },
                    actions = {
                            var buttonSize by remember { mutableStateOf(IntSize.Zero) }
                            Box(modifier = Modifier.onSizeChanged { buttonSize = it }) {
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                if (showMenu) {
                                    Popup(
                                        alignment = Alignment.TopEnd,
                                        offset = IntOffset(0, buttonSize.height),
                                        onDismissRequest = { showMenu = false },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Surface(
                                            modifier = Modifier.width(220.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            shadowElevation = 3.dp
                                        ) {
                                            Column {
                                                DropdownMenuItem(
                                                    text = { Text("Check for Updates", style = MaterialTheme.typography.bodyMedium) },
                                                    leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                                    onClick = { 
                                                        showMenu = false
                                                        settingsViewModel.checkForUpdate()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Settings", style = MaterialTheme.typography.bodyMedium) },
                                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                                    onClick = { 
                                                        showMenu = false
                                                        onNavigateToSettings()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },

    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues),
            verticalAlignment = Alignment.Top
        ) { page ->
            val filteredTransactions = remember(transactions, selectedYear, selectedMonth, settings) {
                transactions?.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { time = transaction.timestamp }
                    val budgetStartCal = Calendar.getInstance().apply {
                        time = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }.time
                        set(Calendar.DAY_OF_MONTH, settings?.salaryCreditDate ?: 1)
                        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < (settings?.salaryCreditDate ?: 1) &&
                            get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                            add(Calendar.MONTH, -1)
                        }
                    }
                    val budgetEndCal = (budgetStartCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                        add(Calendar.DAY_OF_MONTH, -1)
                    }
                    !transactionCal.before(budgetStartCal) && !transactionCal.after(budgetEndCal)
                }
            }

            // Collect categories for icon mapping
            val allCategories by budgetViewModel.allCategories.collectAsState(initial = emptyList())
            val categoryIcons = remember(allCategories) {
                allCategories.associate { it.name to it.icon }
            }

            when (page) {
                0 -> DashboardScreen(
                    transactions = filteredTransactions,
                    settings = settings,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth
                )
                1 -> ReportsScreen(
                    transactions = filteredTransactions,
                    settings = settings,
                    pagerState = pagerState,
                    budgetViewModel = budgetViewModel,
                    onCategoryClick = onCategoryClick
                )
                2 -> TransactionsScreen(
                    transactions = filteredTransactions,
                    settings = settings,
                    selectedTransactionIds = selectedTransactionIds,
                    onSelectionChange = { selectedTransactionIds = it },
                    onTransactionClick = onTransactionClick,
                    categoryIcons = categoryIcons
                )
                3 -> GoalsScreen(
                    goalsViewModel = goalsViewModel,
                    selectedGoalIds = selectedGoalIds,
                    onSelectionChange = { selectedGoalIds = it }
                )
            }
        }
    }
}

@Composable
fun DateFilter(
    selectedYear: Int,
    selectedMonth: Int,
    salaryCreditDate: Int
) {
    val (startCalendar, endCalendar) = remember(selectedYear, selectedMonth, salaryCreditDate) {
        val budgetStartCal = Calendar.getInstance().apply {
            time = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }.time
            set(Calendar.DAY_OF_MONTH, salaryCreditDate)

            val today = Calendar.getInstance()
            if (selectedYear == today.get(Calendar.YEAR) &&
                selectedMonth == today.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                add(Calendar.MONTH, -1)
            }
        }

        val budgetEndCal = (budgetStartCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        Pair(budgetStartCal, budgetEndCal)
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val displayRange = "${dateFormat.format(startCalendar.time)} - ${dateFormat.format(endCalendar.time)}"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = displayRange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}


fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
