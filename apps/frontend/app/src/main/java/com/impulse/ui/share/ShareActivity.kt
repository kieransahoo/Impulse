package com.impulse.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.impulse.data.local.SessionManager
import com.impulse.data.repository.ShareRepository
import com.impulse.utils.extractUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity that receives URLs from the Android share sheet
 * (YouTube, Instagram, Chrome, …), silently sends them to the backend,
 * shows a success/failure toast, then finishes immediately.
 *
 * Registered in AndroidManifest with:
 *   <intent-filter>
 *       <action android:name="android.intent.action.SEND" />
 *       <category android:name="android.intent.category.DEFAULT" />
 *       <data android:mimeType="text/plain" />
 *   </intent-filter>
 */
class ShareActivity : ComponentActivity() {

    private val repository = ShareRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Extract the shared text
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText.isNullOrBlank()) {
            toast("No URL found in share")
            finish()
            return
        }

        // 2. Pull out the first URL (YouTube / Instagram may include surrounding text)
        val url = sharedText.extractUrl()

        // 3. Load session (optional — allows unauthenticated shares)
        val sessionManager = SessionManager.getInstance(this)
        val session        = sessionManager.getSession()

        // 4. Send silently in background
        lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.sendUrl(
                url     = url,
                userId  = session?.userId,
                idToken = session?.idToken ?: ""
            )

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        toast("✓ Link saved successfully")
                    },
                    onFailure = { e ->
                        toast("Failed to save link: ${e.message}")
                    }
                )
                finish()
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}
