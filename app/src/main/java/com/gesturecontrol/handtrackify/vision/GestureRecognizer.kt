package com.gesturecontrol.handtrackify.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

class GestureRecognizer {
    // Constante para definir o quão perto os dedos precisam estar da palma para ser um punho
    private val FIST_THRESHOLD = 0.15f // Aumentamos um pouco a tolerância

    fun recognize(result: HandLandmarkerResult): String {
        // Se nenhuma mão for detectada, não há gesto
        if (result.landmarks().isEmpty()) {
            return "Nenhum"
        }

        // Pega os pontos da primeira mão detectada
        val landmarks = result.landmarks()[0]

        // Verifica se o gesto é um punho fechado
        if (isFist(landmarks)) {
            return "Punho Fechado"
        }

        // Se não for um punho, retorna "Nenhum" por enquanto
        return "Nenhum"
    }

    private fun isFist(landmarks: List<NormalizedLandmark>): Boolean {
        // Pontos de referência (Landmarks)
        val wrist = landmarks[0] // O pulso é o ponto 0
        val indexTip = landmarks[8] // A ponta do indicador é o ponto 8
        val middleTip = landmarks[12] // A ponta do dedo médio é o ponto 12
        val ringTip = landmarks[16] // A ponta do anelar é o ponto 16
        val pinkyTip = landmarks[20] // A ponta do mindinho é o ponto 20

        // Calcula a distância de cada ponta de dedo até o pulso (centro da palma)
        val indexDistance = calculateDistance(indexTip, wrist)
        val middleDistance = calculateDistance(middleTip, wrist)
        val ringDistance = calculateDistance(ringTip, wrist)
        val pinkyDistance = calculateDistance(pinkyTip, wrist)

        // **CORREÇÃO:** Verificamos apenas os 4 dedos principais. O polegar tem uma posição
        // muito variável num punho fechado, então ignorá-lo torna a detecção mais robusta.
        return indexDistance < FIST_THRESHOLD &&
                middleDistance < FIST_THRESHOLD &&
                ringDistance < FIST_THRESHOLD &&
                pinkyDistance < FIST_THRESHOLD
    }

    // Função para calcular a distância 2D entre dois pontos
    private fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        // Converte para Double para a função sqrt e depois de volta para Float
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}

