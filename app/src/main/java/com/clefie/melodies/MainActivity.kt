package com.clefie.melodies

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clefie.melodies.ui.MainScreen
import com.clefie.melodies.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private var vm: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = viewModel()
            vm = viewModel
            MainScreen(viewModel)
        }

        // If permission already granted (2nd+ launch) start immediately
        if (hasAudioPermission()) {
            vm?.startSensors()
        } else {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        // Catches the case where user granted permission in system settings
        // and returned to the app without a fresh launch
        if (hasAudioPermission()) {
            vm?.startSensors()
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BODY_SENSORS
            ),
            REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            // Start sensors regardless — SensorController handles missing mic gracefully
            // If audio was granted it will work, if not mic stays silent
            vm?.startSensors()
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
