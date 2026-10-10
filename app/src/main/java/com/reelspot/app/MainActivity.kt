package com.reelspot.app

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

        handleNotificationIntent()

        setContent {
            ReelspotTheme {
                MainScreen(viewModel = viewModel)
            }
        }
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
}
