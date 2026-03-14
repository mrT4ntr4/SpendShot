package com.spendshot.android.data.repository

import com.spendshot.android.data.AppDao
import com.spendshot.android.data.MerchantCategory
import com.spendshot.android.data.MerchantCategoryDao
import com.spendshot.android.data.MerchantCorrection
import com.spendshot.android.data.MerchantCorrectionDao
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val appDao: AppDao,
    private val merchantCategoryDao: MerchantCategoryDao,
    private val merchantCorrectionDao: MerchantCorrectionDao,
    private val settingsRepository: SettingsRepository
) {

    val allTransactions: Flow<List<TransactionEntity>> = appDao.getAllTransactions()

    // Monthly Limit Logic (Based on Salary Credit Date)
    private fun getSalaryMonthRange(salaryCreditDate: Int): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        // Reset to start of day
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Logic to determine "Current Salary Month":
        // If today is >= salaryDate, we are in the month starting from this month's salaryDate.
        // If today < salaryDate, we are in the month that started last month's salaryDate.
        
        if (currentDay >= salaryCreditDate) {
            calendar.set(java.util.Calendar.DAY_OF_MONTH, salaryCreditDate)
        } else {
            calendar.add(java.util.Calendar.MONTH, -1)
            // Handle edge case where previous month might not have that date (e.g. 31st) -> Calendar handles this usually by rolling over, 
            // but for "salary credit date" logic, usually it snaps to last day if missing? 
            // For simplicity, let's trust Calendar or just set day.
            // A safer generic logic might be needed for day > 28, but let's stick to standard behavior.
            
            // To be precise: If salary date is 31st, and prev month is Feb (28 days).
            // Calendar.set(DAY, 31) in Feb -> March 3rd. That's wrong.
            // We should use Math.min(salaryCreditDate, actualLastDayOfMonth)
            val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, Math.min(salaryCreditDate, maxDay))
        }
        val start = calendar.timeInMillis

        // End date is exactly 1 month after start
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        
        return Pair(start, end)
    }

    // Combine with Settings to get dynamic limit based on user's salary date preference
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val currentMonthTransactionCount: Flow<Int> = settingsRepository.settings
        .flatMapLatest { settings ->
            // Default to 1st if settings not loaded or set
            val salaryDate = settings?.salaryCreditDate ?: 1
            val (start, end) = getSalaryMonthRange(salaryDate)
            appDao.getTransactionCountInRange(start, end)
        }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        appDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val merchantName = transaction.merchant
        appDao.deleteTransaction(transaction)
        // Clean up merchant mapping if this was the last transaction for this merchant
        val remainingCount = appDao.countTransactionsByMerchant(merchantName)
        if (remainingCount == 0) {
            merchantCategoryDao.deleteMerchantMapping(merchantName)
        }
    }

    suspend fun deleteTransactions(ids: Set<Long>) {
        // Get merchants before deleting so we can check if cleanup is needed
        val affectedMerchants = appDao.getMerchantsByIds(ids)
        appDao.deleteTransactionsByIds(ids)
        // Clean up merchant mappings for merchants with no remaining transactions
        for (merchant in affectedMerchants) {
            val remainingCount = appDao.countTransactionsByMerchant(merchant)
            if (remainingCount == 0) {
                merchantCategoryDao.deleteMerchantMapping(merchant)
            }
        }
    }

    // Merchant Category Logic
    suspend fun getCategoryForMerchant(merchantName: String): String? {
        return merchantCategoryDao.getCategoryForMerchant(merchantName)
    }

    suspend fun updateMerchantCategory(merchantName: String, category: String) {
        merchantCategoryDao.insert(MerchantCategory(merchantName, category))
    }

    // OCR Correction Learning - auto-correct merchant names based on user edits
    suspend fun getCorrectedMerchant(ocrOutput: String): String? {
        // Normalize to lowercase for consistent matching
        return merchantCorrectionDao.getCorrectedName(ocrOutput.lowercase().trim())
    }

    suspend fun saveMerchantCorrection(ocrOutput: String, correctedName: String) {
        // Only save if the names are actually different
        val normalizedOcr = ocrOutput.lowercase().trim()
        if (normalizedOcr != correctedName.lowercase().trim()) {
            merchantCorrectionDao.insertCorrection(
                MerchantCorrection(ocrOutput = normalizedOcr, correctedName = correctedName)
            )
        }
    }
}
