package com.gesturecontrol.handtrackify.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

class HandLandmarkerHelper(
    val context: Context,
    private val handLandmarkerListener: LandmarkerListener
) {

    private var handLandmarker: HandLandmarker? = null
    private val gestureRecognizer = GestureRecognizer()

    // SOLUÇÃO PARA A LENTIDÃO: Esta trava (ou "porteiro") é a chave.
    // Ela garante que não vamos tentar processar um novo frame enquanto o anterior
    // (potencialmente lento, por não ter mãos) ainda está sendo analisado.
    private val isProcessing = AtomicBoolean(false)

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
        // AQUI ESTÁ A VERIFICAÇÃO:
        // O método .getAndSet(true) verifica se o valor é 'false' e, se for, o define como 'true' em uma única operação.
        // Se o valor já era 'true' (ou seja, estamos ocupados), a condição do 'if' é verdadeira e nós pulamos o frame.
        if (handLandmarker == null || isProcessing.getAndSet(true)) {
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()
        val bitmap = imageProxy.toBitmap()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        imageProxy.close()

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        val gesture = if (result.landmarks().isEmpty()) {
            "Nenhum"
        } else {
            gestureRecognizer.recognize(result)
        }

        handLandmarkerListener.onResults(
            ResultBundle(
                results = result,
                inferenceTime = inferenceTime,
                inputImageHeight = input.height,
                inputImageWidth = input.width,
                gesture = gesture
            )
        )

        // ANÁLISE TERMINADA: Liberamos a trava para o próximo frame poder entrar.
        isProcessing.set(false)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerListener.onError(error.message ?: "Erro desconhecido")
        // EM CASO DE ERRO: Também liberamos a trava para não bloquear o app.
        isProcessing.set(false)
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
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
        val gesture: String
    )

    companion object {
        const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        private const val TAG = "HandLandmarkerHelper"
    }
}

