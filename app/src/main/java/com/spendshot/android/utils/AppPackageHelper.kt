package com.spendshot.android.utils

import android.content.Context
import android.content.pm.PackageManager

object AppPackageHelper {

    private val packageMap = mapOf(
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "swiggy" to "in.swiggy.android",
        "zomato" to "com.application.zomato"
        // Add other direct mappings here for speed and accuracy
    )

    fun findPackageNameForLabel(context: Context, label: String): String? {
        val lowercaseLabel = label.lowercase()

        // 1. Check our predefined map first for a quick and accurate result.
        if (packageMap.containsKey(lowercaseLabel)) {
            return packageMap[lowercaseLabel]
        }

        // 2. If not in the map, search installed applications as a fallback.
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // This search can be slow, but is a good fallback.
        // It looks for a package name that contains the label.
        return installedApps.find { appInfo ->
            appInfo.packageName.contains(lowercaseLabel)
        }?.packageName
    }
}
