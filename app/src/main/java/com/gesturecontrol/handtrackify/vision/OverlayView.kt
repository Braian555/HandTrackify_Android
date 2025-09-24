package com.gesturecontrol.handtrackify.vision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: HandLandmarkerResult? = null
    private var pointPaint: Paint
    private var linePaint: Paint

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        // Inicializa os "pincéis" para desenhar
        pointPaint = Paint().apply {
            color = Color.YELLOW // Cor dos pontos
            style = Paint.Style.FILL
            strokeWidth = 8f // Tamanho dos pontos
        }

        linePaint = Paint().apply {
            color = Color.WHITE // Cor das linhas
            style = Paint.Style.STROKE
            strokeWidth = 4f // Espessura das linhas
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // Se houver resultados, desenha os pontos e linhas
        results?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                // Desenha os pontos (landmarks)
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Desenha as conexões entre os pontos
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        handLandmarkerResult.landmarks()[0][it.start()].x() * imageWidth * scaleFactor,
                        handLandmarkerResult.landmarks()[0][it.start()].y() * imageHeight * scaleFactor,
                        handLandmarkerResult.landmarks()[0][it.end()].x() * imageWidth * scaleFactor,
                        handLandmarkerResult.landmarks()[0][it.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }
        }
    }

    // Esta é a função que a MainActivity estava tentando chamar
    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = handLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        // Invalida a view, forçando a chamada ao método onDraw para redesenhar
        invalidate()
    }
}

