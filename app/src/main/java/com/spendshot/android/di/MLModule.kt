package com.spendshot.android.di

import android.content.Context
import com.spendshot.android.utils.AppClassifier
import com.spendshot.android.utils.YoloDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideAppClassifier(@ApplicationContext context: Context): AppClassifier {
        // Paytm is included for detection/extraction but filtered from UI
        val appLabels = arrayOf("GPay", "Swiggy", "Zomato", "Unknown", "PhonePe", "Paytm")
        return AppClassifier(context, "model_classifier.tflite", appLabels)
    }

    @Provides
    @Singleton
    fun provideYoloDetector(@ApplicationContext context: Context): YoloDetector {
        val labels = arrayOf("Amount", "Merchant")
        return YoloDetector(context, "model_extractor.tflite", labels)
    }
}
