package com.spendshot.android.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Date

enum class TransactionType { INCOME, EXPENSE }
enum class Theme { LIGHT, DARK }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String, // e.g., "Food", "Travel"
    val color: Int, // ColorInt
    val icon: String? = null, // Optional emoji or icon name
    val isDefault: Boolean = false // Protected from deletion if true
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)
    
    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateTransactionCategories(oldName: String, newName: String)

    @Query("UPDATE category_budgets SET category = :newName WHERE category = :oldName")
    suspend fun updateBudgetCategories(oldName: String, newName: String)
    
    @Transaction
    suspend fun renameCategory(oldName: String, newName: String) {
        val oldCategory = getCategory(oldName) ?: return
        val newCategory = oldCategory.copy(name = newName)
        insert(newCategory)
        updateTransactionCategories(oldName, newName)
        updateBudgetCategories(oldName, newName)
        delete(oldCategory)
    }

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategory(name: String): CategoryEntity?
}


@Entity(tableName = "merchant_category_map")
data class MerchantCategory(
    @PrimaryKey
    val merchantName: String,
    val category: String
)

@Dao
interface MerchantCategoryDao {
    @Query("SELECT category FROM merchant_category_map WHERE merchantName = :merchantName")
    suspend fun getCategoryForMerchant(merchantName: String): String?

    @Upsert
    suspend fun insert(merchantCategory: MerchantCategory)
    
    @Query("DELETE FROM merchant_category_map WHERE merchantName = :merchantName")
    suspend fun deleteMerchantMapping(merchantName: String)
}

// OCR Correction Learning - maps OCR output to user-corrected merchant names
@Entity(tableName = "merchant_corrections")
data class MerchantCorrection(
    @PrimaryKey val ocrOutput: String,  // What OCR detected (normalized to lowercase for matching)
    val correctedName: String           // What user corrected it to
)

@Dao
interface MerchantCorrectionDao {
    @Query("SELECT correctedName FROM merchant_corrections WHERE ocrOutput = :ocrOutput")
    suspend fun getCorrectedName(ocrOutput: String): String?

    @Upsert
    suspend fun insertCorrection(correction: MerchantCorrection)
    
    @Query("DELETE FROM merchant_corrections WHERE ocrOutput = :ocrOutput")
    suspend fun deleteCorrection(ocrOutput: String)
}


@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val note: String,
    val category: String,
    val type: TransactionType,
    val timestamp: Date = Date(),
    val sourceApp: String? = null,
    val isRecurring: Boolean = false,
    val detectedApp: String?
)

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val limitAmount: Double,
    val monthYear: String // Format: "MM-YYYY"
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: Long? = null,
    val color: Int? = null,
    val icon: String? = null // URL or icon identifier
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val isOwedToMe: Boolean,
    val isSettled: Boolean = false
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monthlySalary: Double,
    val salaryCreditDate: Int, // Day of the month
    val biometricAuthEnabled: Boolean = false,
    val theme: Theme?
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Dao
interface AppDao {
    // Categories (Proxy to CategoryDao for simplicity if needed, but best to separate)
    // Keeping everything in AppDao for now to minimize refactor friction, or separating? 
    // Let's Separate.

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTotalTransactionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :start AND timestamp <= :end")
    fun getTransactionCountInRange(start: Long, end: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id IN (:transactionIds)")
    suspend fun deleteTransactionsByIds(transactionIds: Set<Long>)
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE :merchantPattern AND timestamp >= :start AND timestamp <= :end")
    suspend fun getTransactionsByMerchantAndDateRange(merchantPattern: String, start: Long, end: Long): List<TransactionEntity>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE merchant = :merchantName")
    suspend fun countTransactionsByMerchant(merchantName: String): Int
    
    @Query("SELECT DISTINCT merchant FROM transactions WHERE id IN (:transactionIds)")
    suspend fun getMerchantsByIds(transactionIds: Set<Long>): List<String>

    @Query("SELECT * FROM category_budgets")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE category = :category AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(category: String, monthYear: String): CategoryBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)
    
    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteBudget(id: Int)

    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET savedAmount = :amount WHERE id = :id")
    suspend fun updateGoalProgress(id: Int, amount: Double)

    @Query("SELECT * FROM loans")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Query("UPDATE loans SET isSettled = :settled WHERE id = :id")
    suspend fun updateLoanStatus(id: Int, settled: Boolean)

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryBudget::class,
        GoalEntity::class,
        LoanEntity::class,
        SettingsEntity::class,
        MerchantCategory::class,
        CategoryEntity::class,
        MerchantCorrection::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract fun merchantCorrectionDao(): MerchantCorrectionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_expense.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate Default Categories
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultCategories = listOf(
                                CategoryEntity("Food", 0xFF4CAF50.toInt(), isDefault = true),
                                CategoryEntity("Travel", 0xFF2196F3.toInt(), isDefault = true),
                                CategoryEntity("Bills", 0xFFFFC107.toInt(), isDefault = true),
                                CategoryEntity("Shopping", 0xFFE91E63.toInt(), isDefault = true),
                                CategoryEntity("Entertainment", 0xFF9C27B0.toInt(), isDefault = true),
                                CategoryEntity("Health", 0xFFFF5722.toInt(), isDefault = true),
                                CategoryEntity("Groceries", 0xFF00BCD4.toInt(), isDefault = true),
                            CategoryEntity("Other", 0xFF9E9E9E.toInt(), isDefault = true)
                            )

                            val dao = getDatabase(context).categoryDao()
                            defaultCategories.forEach { dao.insert(it) }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
