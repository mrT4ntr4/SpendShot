package com.spendshot.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.data.repository.TransactionRepository
import com.spendshot.android.domain.ReceiptProcessor
import com.spendshot.android.utils.ParsedReceipt
import com.spendshot.android.utils.AppClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import android.net.Uri

import com.spendshot.android.data.CategoryDao
import kotlinx.coroutines.flow.map

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val receiptProcessor: ReceiptProcessor,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val allCategories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _parsedReceipt = MutableStateFlow<ParsedReceipt?>(null)
    val parsedReceipt = _parsedReceipt.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    fun processReceipt(uri: Uri) = viewModelScope.launch {
        // Log.i("MainViewModel", "processReceipt called with uri: $uri")
        _isProcessing.value = true
        try {
            // Log.i("MainViewModel", "Calling receiptProcessor.processReceipt...")
            var receipt = receiptProcessor.processReceipt(uri)
            // Log.i("MainViewModel", "receiptProcessor returned: amount=${receipt.amount}, merchant=${receipt.merchant}")
            
            // Apply OCR correction if we have a learned one for this merchant name
            val originalOcrMerchant = receipt.merchant
            val correctedMerchant = repository.getCorrectedMerchant(originalOcrMerchant)
            if (correctedMerchant != null) {
                receipt = receipt.copy(merchant = correctedMerchant, originalOcrMerchant = originalOcrMerchant)
            } else {
                receipt = receipt.copy(originalOcrMerchant = originalOcrMerchant)
            }
            
            // VALIDATION: Check for 0 amount or Unknown merchant - skip auto-logging
            val isZeroAmount = receipt.amount == 0.0
            val isUnknownMerchant = receipt.merchant.equals("Unknown", ignoreCase = true) || receipt.merchant.isBlank()
            
            if (isZeroAmount || isUnknownMerchant) {
                // Build manual entry reason message
                val reason = when {
                    isZeroAmount && isUnknownMerchant -> "Please enter amount and merchant manually"
                    isZeroAmount -> "Please enter amount manually"
                    else -> "Please enter merchant manually"
                }
                // Log.i("AutoLog", "Skipping auto-log: $reason")
                val icon = if (receipt.category != null) allCategories.value.find { it.name == receipt.category }?.icon else null
                _parsedReceipt.value = receipt.copy(categoryIcon = icon, manualEntryReason = reason)
                return@launch
            }
            
            val result = tryAutoLogTransaction(
                merchantName = receipt.merchant,
                amount = receipt.amount,
                detectedApp = receipt.detectedAppLabel,
                transactionType = receipt.transactionType
            )
            
            when (result) {
                is AutoLogResult.Success -> {
                    val icon = allCategories.value.find { it.name == result.category }?.icon
                    _parsedReceipt.value = receipt.copy(
                        category = result.category,
                        categoryIcon = icon
                    )
                }
                is AutoLogResult.Failed -> {
                    // Auto-log failed (e.g. unknown merchant), show manual edit dialog.
                    val icon = if (receipt.category != null) allCategories.value.find { it.name == receipt.category }?.icon else null
                    _parsedReceipt.value = receipt.copy(categoryIcon = icon)
                }
            }
        } catch (e: Exception) {
            // Log.e("MainViewModel", "processReceipt EXCEPTION: ${e.message}", e)
            e.printStackTrace()
            // Emit error or empty receipt
             _parsedReceipt.value = ParsedReceipt(0.0, "Unknown", detectedAppLabel = "Unknown")
        } finally {
            _isProcessing.value = false
        }
    }

    
    fun clearParsedReceipt() {
        _parsedReceipt.value = null
    }

    val transactions = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val transactionCount = repository.currentMonthTransactionCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun addTransaction(t: TransactionEntity, originalOcrMerchant: String? = null) = viewModelScope.launch {
        repository.insertTransaction(t)
        repository.updateMerchantCategory(t.merchant, t.category)
        
        // Learn OCR correction if merchant was edited from original OCR output
        if (originalOcrMerchant != null && originalOcrMerchant.isNotBlank()) {
            repository.saveMerchantCorrection(originalOcrMerchant, t.merchant)
        }
    }

    fun updateTransaction(t: TransactionEntity) = viewModelScope.launch {
        repository.insertTransaction(t)
        repository.updateMerchantCategory(t.merchant, t.category)
    }

    fun deleteTransaction(t: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(t)
    }

    fun deleteMultipleTransactions(transactionIds: Set<Long>) = viewModelScope.launch {
        repository.deleteTransactions(transactionIds)
    }

    fun getCategoryForMerchant(merchantName: String, onCategoryFound: (String) -> Unit) {
        if (merchantName.isBlank()) return

        viewModelScope.launch {
            repository.getCategoryForMerchant(merchantName)?.let { category ->
                onCategoryFound(category)
            }
        }
    }

    sealed class AutoLogResult {
        data class Success(val category: String) : AutoLogResult()
        object Failed : AutoLogResult()
    }

    suspend fun tryAutoLogTransaction(
        merchantName: String, amount: Double,
        detectedApp: String?,
        transactionType: TransactionType
    ): AutoLogResult {

        val categoryToLog: String? =
            // 1. Check if we have a manually set override for this merchant from before
            repository.getCategoryForMerchant(merchantName)
            // 2. If not, try to classify using our keyword engine
            ?: com.spendshot.android.utils.CategoryClassifier.classify(
                merchant = merchantName,
                detectedApp = detectedApp
            )

        if (categoryToLog != null) {
            val newTransaction = TransactionEntity(
                amount = amount,
                merchant = merchantName,
                note = "Auto-logged transaction",
                category = categoryToLog,
                type = transactionType,
                timestamp = Date(),
                sourceApp = detectedApp,
                isRecurring = false,
                detectedApp = detectedApp
            )
            repository.insertTransaction(newTransaction)
            repository.updateMerchantCategory(merchantName, categoryToLog)

            // Log.i("AutoLog", "Successfully auto-logged transaction for '$merchantName' from app '$detectedApp'. Category: '$categoryToLog'.")
            return AutoLogResult.Success(categoryToLog)
        }

        // Log.i("AutoLog", "Could not auto-log. Merchant '$merchantName' from app '$detectedApp' is new.")
        return AutoLogResult.Failed
    }
}
