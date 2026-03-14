package com.spendshot.android.di

import android.content.Context
import com.spendshot.android.data.AppDatabase
import com.spendshot.android.data.AppDao
import com.spendshot.android.data.MerchantCategoryDao
import com.spendshot.android.data.MerchantCorrectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao {
        return database.dao()
    }

    @Provides
    fun provideMerchantCategoryDao(database: AppDatabase): MerchantCategoryDao {
        return database.merchantCategoryDao()
    }
    
    @Provides
    fun provideMerchantCorrectionDao(database: AppDatabase): MerchantCorrectionDao {
        return database.merchantCorrectionDao()
    }
    
    @Provides
    fun provideCategoryDao(database: AppDatabase): com.spendshot.android.data.CategoryDao {
        return database.categoryDao()
    }
}
