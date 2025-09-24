package com.gesturecontrol.handtrackify.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.gesturecontrol.handtrackify.databinding.ActivityMainBinding
import com.gesturecontrol.handtrackify.vision.CameraManager
import com.gesturecontrol.handtrackify.vision.HandLandmarkerHelper

class MainActivity : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissão da câmara é necessária para usar a app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handLandmarkerHelper = HandLandmarkerHelper(this, this)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
            handLandmarkerHelper.detectLiveStream(imageProxy)
        }

        cameraManager = CameraManager(
            this,
            binding.previewView,
            this,
            imageAnalyzer
        )
        cameraManager.startCamera()
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            // Atualiza o tempo de inferência
            binding.textViewInferenceTime.text = "${resultBundle.inferenceTime} ms"

            // Atualiza o texto do gesto reconhecido
            binding.textViewGesture.text = resultBundle.gesture

            // Desenha o esqueleto da mão na tela
            binding.overlay.setResults(
                resultBundle.results,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth
            )
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraManager.isInitialized) {
            cameraManager.stopCamera()
        }
    }
}

