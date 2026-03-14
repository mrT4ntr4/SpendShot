package com.spendshot.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendshot.android.data.AppDao
import com.spendshot.android.data.CategoryBudget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import androidx.compose.ui.graphics.Color
// Enum removed

import com.spendshot.android.data.CategoryDao
import com.spendshot.android.data.CategoryEntity
import com.spendshot.android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val dao: AppDao,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _transactions = dao.getAllTransactions()
    private val _budgets = dao.getAllBudgets()
    private val _categories = categoryDao.getAllCategories()
    val allCategories = _categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())

    fun updateDate(year: Int, month: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _selectedDate.value = cal
    }

    private fun isTransactionInBudgetWindow(
        tDate: java.util.Date,
        selectedDate: Calendar,
        salaryCreditDate: Int
    ): Boolean {
        val selectedYear = selectedDate.get(Calendar.YEAR)
        val selectedMonth = selectedDate.get(Calendar.MONTH)

        val budgetStartCal = Calendar.getInstance().apply {
            time = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }.time
            set(Calendar.DAY_OF_MONTH, salaryCreditDate)
            
            // Logic matching MainApp.dateFilter and filteredTransactions
            // If the "selected" month is the current calendar month, we check if today is before salary date.
            // If so, we are actually viewing the *previous* budget cycle relative to calendar month?
            // Wait, MainApp passes `selectedYear` and `selectedMonth` which are STATE.
            // MainApp *updates* selectedYear/Month via arrows.
            // The special "today" logic in MainApp seems to be for *initial* state or dynamic adjustment?
            // Actually MainApp logic:
            /*
            if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < (salaryCreditDate) &&
                 get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                 add(Calendar.MONTH, -1)
            }
             */
             // This logic shifts the "start date" back a month if we are "early" in the current month.
             // But wait, `filteredTransactions` in MainApp applies this transformation to the resulting bounds
             // based on the `selectedYear/Month`.
             
             // If I assume `selectedDate` passed to this VM is strictly the UI's selected Year/Month,
             // I should replicate the logic EXACTLY.
             
             val today = Calendar.getInstance()
             // We must compare against the *selected* date vs *today*?
             // MainApp uses `selectedYear` (state) and `Calendar.getInstance()` (today).
             
             if (selectedYear == today.get(Calendar.YEAR) &&
                 selectedMonth == today.get(Calendar.MONTH) &&
                 today.get(Calendar.DAY_OF_MONTH) < salaryCreditDate) {
                 add(Calendar.MONTH, -1)
             }
        }
        
        val budgetEndCal = (budgetStartCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            // End of the day
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        // Reset start cal to beginning of day
        budgetStartCal.set(Calendar.HOUR_OF_DAY, 0)
        budgetStartCal.set(Calendar.MINUTE, 0)
        budgetStartCal.set(Calendar.SECOND, 0)

        val tCal = Calendar.getInstance().apply { time = tDate }
        
        return !tCal.before(budgetStartCal) && !tCal.after(budgetEndCal)
    }

    val budgets = combine(_budgets, _transactions, _selectedDate, _categories, settingsRepository.settings) { budgets, transactions, date, categories, settings ->
        val salaryDate = settings?.salaryCreditDate ?: 1
        
        categories.map { category ->
            val budget = budgets.find { it.category == category.name }
            
            // Calculate spent amount
            val spent = transactions
                .filter { transaction ->
                    transaction.category == category.name && 
                    transaction.type == com.spendshot.android.data.TransactionType.EXPENSE &&
                    isTransactionInBudgetWindow(transaction.timestamp, date, salaryDate)
                }
                .sumOf { it.amount }

            val limit = budget?.limitAmount ?: 0.0
            
            BudgetUiState(
                category = category,
                limitAmount = limit,
                spentAmount = spent,
                progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f,
                isExceeded = limit > 0 && spent > limit,
                hasBudget = budget != null,
                color = Color(category.color)
            )
        }
         .sortedByDescending { it.spentAmount } // Sort by spending
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome = combine(_transactions, _selectedDate, settingsRepository.settings) { transactions, date, settings ->
        val salaryDate = settings?.salaryCreditDate ?: 1
        transactions
            .filter { transaction ->
                transaction.type == com.spendshot.android.data.TransactionType.INCOME &&
                isTransactionInBudgetWindow(transaction.timestamp, date, salaryDate)
            }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun addBudget(budget: CategoryBudget) {
        viewModelScope.launch {
            // Check if a budget already exists for this category and month
            val existingBudget = dao.getBudgetByCategoryAndMonth(budget.category, budget.monthYear)
            val budgetToSave = if (existingBudget != null) {
                // Reuse the existing ID to update rather than insert
                budget.copy(id = existingBudget.id)
            } else {
                budget
            }
            dao.insertBudget(budgetToSave)
        }
    }

    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.insert(category)
        }
    }
    
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.delete(category)
            // Also clean up budgets? Handled by db consistency ideally or manual cleanup if needed
        }
    }
    
    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            categoryDao.renameCategory(oldName, newName)
        }
    }
}

data class BudgetUiState(
    val category: CategoryEntity, // Changed from Category enum to Entity
    val limitAmount: Double,
    val spentAmount: Double,
    val progress: Float,
    val isExceeded: Boolean,
    val hasBudget: Boolean,
    val color: Color
)