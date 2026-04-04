package com.clefie.melodies

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clefie.melodies.ui.*
import com.clefie.melodies.viewmodel.FlowStep
import com.clefie.melodies.viewmodel.FlowViewModel
import com.clefie.melodies.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private var mainVm: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val flowVm: FlowViewModel = viewModel()
            val vm: MainViewModel     = viewModel()
            mainVm = vm

            val step        by flowVm.step.collectAsState()
            val isFullscreen by flowVm.isFullscreen.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    FlowStep.INTRO -> {
                        IntroScreen(
                            step          = step,
                            onCreateSound = { flowVm.onCreateSound() }
                        )
                    }
                    FlowStep.ACTIVATION, FlowStep.MAGIC -> {
                        ActivationScreen(step = step)
                    }
                    FlowStep.DASHBOARD -> {
                        MainScreen(
                            vm     = vm,
                            flowVm = flowVm
                        )
                    }
                }

                // Fullscreen visualizer overlay
                if (isFullscreen) {
                    FullscreenVisualizer(
                        mainVm  = vm,
                        flowVm  = flowVm,
                        onClose = { flowVm.closeFullscreen() }
                    )
                }
            }
        }

        if (hasAudioPermission()) {
            mainVm?.startSensors()
        } else {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAudioPermission()) mainVm?.startSensors()
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.BODY_SENSORS),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) mainVm?.startSensors()
    }
}
