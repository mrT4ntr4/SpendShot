package com.spendshot.android.utils

import androidx.compose.ui.graphics.Color

/**
 * Color palette for category assignments
 * Provides 20 vibrant, distinguishable colors for categories
 */
object CategoryColors {
    val palette = listOf(
        0xFFE91E63.toInt(), // Pink
        0xFF9C27B0.toInt(), // Purple
        0xFF673AB7.toInt(), // Deep Purple
        0xFF3F51B5.toInt(), // Indigo
        0xFF2196F3.toInt(), // Blue
        0xFF00BCD4.toInt(), // Cyan
        0xFF009688.toInt(), // Teal
        0xFF4CAF50.toInt(), // Green
        0xFF8BC34A.toInt(), // Light Green
        0xFFFFC107.toInt(), // Amber
        0xFFFF9800.toInt(), // Orange
        0xFFFF5722.toInt(), // Deep Orange
        0xFFE53935.toInt(), // Red
        0xFF5E35B1.toInt(), // Deep Purple Variant
        0xFF1E88E5.toInt(), // Blue Variant
        0xFF00897B.toInt(), // Teal Variant
        0xFFAB47BC.toInt(), // Purple Variant
        0xFF26A69A.toInt(), // Teal Variant 2
        0xFF7CB342.toInt(), // Light Green Variant
        0xFFFFB300.toInt()  // Amber Variant
    )
    
    /**
     * Get a color from the palette based on a hash value
     * Ensures consistent color for the same input
     */
    fun getColorForHash(hash: Int): Int {
        val index = Math.abs(hash) % palette.size
        return palette[index]
    }
    
    /**
     * Get next available color that's not in the used set
     * Falls back to hash-based selection if all colors are used
     */
    fun getNextAvailableColor(usedColors: Set<Int>): Int {
        // Find first color not in use
        val availableColor = palette.firstOrNull { it !in usedColors }
        return availableColor ?: palette.random()
    }
}
