package com.spendshot.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The intent that started this activity
        val receivedIntent = intent

        // Create a new intent to forward to MainActivity
        val forwardIntent = Intent(this, MainActivity::class.java).apply {
            action = receivedIntent.action
            type = receivedIntent.type

            // --- FIX IS HERE ---
            // Explicitly get the Parcelable (in this case, a Uri) with its type.
            // This resolves the overload ambiguity for putExtra.
            val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            putExtra(Intent.EXTRA_STREAM, imageUri)
        }

        startActivity(forwardIntent)
        finish() // Finish ShareActivity so it's not in the back stack
    }
}
