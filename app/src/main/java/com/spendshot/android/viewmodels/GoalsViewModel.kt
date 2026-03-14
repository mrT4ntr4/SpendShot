package com.spendshot.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendshot.android.data.AppDao
import com.spendshot.android.data.CategoryDao
import com.spendshot.android.data.CategoryEntity
import com.spendshot.android.data.GoalEntity
import com.spendshot.android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val dao: AppDao,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val goals = dao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date filter state
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()
    
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth = _selectedMonth.asStateFlow()
    
    // Monthly savings per goal (goalId -> amount)
    private val _monthlySavings = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val monthlySavings = _monthlySavings.asStateFlow()

    fun updateDate(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        refreshMonthlySavings()
    }
    
    private fun refreshMonthlySavings() {
        viewModelScope.launch {
            val savingsMap = mutableMapOf<Int, Double>()
            val settings = settingsRepository.settings.first()
            val salaryCreditDate = settings?.salaryCreditDate ?: 1
            val (start, end) = getBudgetPeriodRange(_selectedYear.value, _selectedMonth.value, salaryCreditDate)
            
            goals.value.forEach { goal ->
                // Query by category which is now "Goal: {name}"
                val transactions = dao.getTransactionsByMerchantAndDateRange(
                    merchantPattern = goal.name, // merchant is now just the goal name
                    start = start,
                    end = end
                )
                savingsMap[goal.id] = transactions.sumOf { it.amount }
            }
            _monthlySavings.value = savingsMap
        }
    }

    
    // Get savings history for a specific goal (for statistics)
    suspend fun getGoalSavingsHistory(goalName: String): List<Pair<String, Double>> {
        val history = mutableListOf<Pair<String, Double>>()
        val settings = settingsRepository.settings.first()
        val salaryCreditDate = settings?.salaryCreditDate ?: 1
        
        // Get last 6 budget periods
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance().apply {
                // Adjust for salary credit date
                if (get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                    add(Calendar.MONTH, -1)
                }
                add(Calendar.MONTH, -i)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val (start, end) = getBudgetPeriodRange(year, month, salaryCreditDate)
            
            val transactions = dao.getTransactionsByMerchantAndDateRange(
                merchantPattern = goalName, // merchant is now just the goal name
                start = start,
                end = end
            )

            val monthName = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(cal.time)
            history.add(monthName to transactions.sumOf { it.amount })
        }
        return history
    }
    
    private fun getBudgetPeriodRange(year: Int, month: Int, salaryCreditDate: Int): Pair<Long, Long> {
        val budgetStartCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, salaryCreditDate)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Match the MainApp/BudgetViewModel logic
            val today = Calendar.getInstance()
            if (year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                add(Calendar.MONTH, -1)
            }
        }
        
        val budgetEndCal = (budgetStartCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return budgetStartCal.timeInMillis to budgetEndCal.timeInMillis
    }

    fun addGoal(goal: GoalEntity) {
        viewModelScope.launch {
            dao.insertGoal(goal)
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            dao.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            dao.deleteGoal(goal)
        }
    }

    fun deleteMultipleGoals(goalIds: Set<Int>) {
        viewModelScope.launch {
            goalIds.forEach { id ->
                goals.value.find { it.id == id }?.let { goal ->
                    dao.deleteGoal(goal)
                }
            }
        }
    }

    fun updateGoalProgress(id: Int, amount: Double) {
        viewModelScope.launch {
            dao.updateGoalProgress(id, amount)
        }
    }

    fun addSavings(goalId: Int, amountToAdd: Double) {
        val currentGoal = goals.value.find { it.id == goalId }
        if (currentGoal != null) {
            viewModelScope.launch {
                dao.updateGoalProgress(goalId, currentGoal.savedAmount + amountToAdd)
                
                // Use goal name as category (prefixed with 'Goal: ') so each goal appears separately in Categories
                val goalCategoryName = "Goal: ${currentGoal.name}"
                
                // Create or update category entry for this goal (for display in ReportsScreen)
                val goalCategory = CategoryEntity(
                    name = goalCategoryName,
                    color = com.spendshot.android.utils.CategoryColors.getColorForHash(goalId), // Unique color per goal
                    icon = currentGoal.icon, // Use goal's icon URL
                    isDefault = false
                )
                categoryDao.insert(goalCategory)
                
                val transaction = com.spendshot.android.data.TransactionEntity(
                    amount = amountToAdd,
                    merchant = currentGoal.name,
                    note = "Savings",
                    category = goalCategoryName,
                    type = com.spendshot.android.data.TransactionType.EXPENSE,
                    timestamp = java.util.Date(),
                    sourceApp = currentGoal.icon, // Store goal icon URL here for TransactionsScreen
                    detectedApp = null
                )
                dao.insertTransaction(transaction)
                refreshMonthlySavings()
            }
        }
    }
}


