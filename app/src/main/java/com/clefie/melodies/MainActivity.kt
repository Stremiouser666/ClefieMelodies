package com.clefie.melodies

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
    private var flowVm: FlowViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Back press — one step back, do nothing on intro
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                flowVm?.goBack()
            }
        })

        setContent {
            val fvm: FlowViewModel = viewModel()
            val vm: MainViewModel  = viewModel()
            mainVm = vm
            flowVm = fvm

            val step         by fvm.step.collectAsState()
            val isFullscreen by fvm.isFullscreen.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    FlowStep.INTRO -> {
                        IntroScreen(
                            step = step,
                            onCreateSound = {
                                if (hasAudioPermission()) {
                                    mainVm?.startSensors()
                                    fvm.onCreateSound()
                                } else {
                                    requestPermissions()
                                }
                            }
                        )
                    }
                    FlowStep.ACTIVATION, FlowStep.MAGIC -> {
                        ActivationScreen(
                            step    = step,
                            onReady = { fvm.onReadyToCreate() }
                        )
                    }
                    FlowStep.DASHBOARD -> {
                        MainScreen(vm = vm, flowVm = fvm)
                    }
                }

                if (isFullscreen) {
                    FullscreenVisualizer(
                        mainVm  = vm,
                        flowVm  = fvm,
                        onClose = { fvm.closeFullscreen() }
                    )
                }
            }
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
        if (requestCode == 1001) {
            mainVm?.startSensors()
            flowVm?.onCreateSound()
        }
    }
}
