package com.gesturecontrol.handtrackify.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    val context: Context,
    private val handLandmarkerListener: LandmarkerListener
) {

    private var handLandmarker: HandLandmarker? = null
    // Adiciona uma instância do nosso novo reconhecedor de gestos.
    private val gestureRecognizer = GestureRecognizer()

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
        baseOptionsBuilder.setDelegate(Delegate.GPU)
        baseOptionsBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            handLandmarkerListener.onError("Falha ao inicializar o Hand Landmarker: ${e.message}")
        } catch (e: RuntimeException) {
            handLandmarkerListener.onError("Falha ao inicializar o Hand Landmarker: ${e.message}")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy) {
        if (handLandmarker == null) {
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // Usa o reconhecedor para obter o nome do gesto.
        val gesture = gestureRecognizer.recognize(result)

        handLandmarkerListener.onResults(
            ResultBundle(
                results = result,
                inferenceTime = inferenceTime,
                inputImageHeight = input.height,
                inputImageWidth = input.width,
                gesture = gesture // Adiciona o gesto ao pacote de resultados.
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerListener.onError(error.message ?: "Erro desconhecido")
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }

    data class ResultBundle(
        val results: HandLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val gesture: String // Novo campo para o gesto.
    )

    companion object {
        const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        private const val TAG = "HandLandmarkerHelper"
    }
}

