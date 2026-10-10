package com.reelspot.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.reelspot.app.ui.MainScreen
import com.reelspot.app.ui.ReelSpotViewModel
import com.reelspot.app.ui.theme.ReelspotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReelSpotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle notification deep-link
        handleNotificationIntent()

        // Handle share intent (when app is cold-started via share)
        handleShareIntent(intent)

        setContent {
            ReelspotTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    /**
     * Called when the activity is already running (launchMode=singleTask)
     * and the user shares a new URL while the app is in the foreground/background.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLocationUpdates()
    }

    private fun handleNotificationIntent() {
        val placeId = intent?.getLongExtra("EXTRA_PLACE_ID", -1L) ?: -1L
        if (placeId != -1L) {
            viewModel.selectPlaceById(placeId)
        }
    }

    /**
     * Extracts the shared text/URL from an ACTION_SEND intent and
     * forwards it to the ViewModel, which surfaces the ShareReel confirmation UI.
     */
    private fun handleShareIntent(receivedIntent: Intent?) {
        if (receivedIntent?.action != Intent.ACTION_SEND) return
        if (receivedIntent.type != "text/plain") return
        val sharedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            viewModel.handleSharedIntent(sharedText)
        }
    }
}
