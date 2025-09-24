package com.gesturecontrol.handtrackify.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

class GestureRecognizer {
    // Enum para facilitar a referência aos landmarks dos dedos
    private enum class Finger(val tip: Int, val dip: Int, val mcp: Int, val fingerName: String) {
        INDEX(8, 7, 5, "Indicador"),
        MIDDLE(12, 11, 9, "Médio"),
        RING(16, 15, 13, "Anelar"),
        PINKY(20, 19, 17, "Mínimo")
        // O polegar (THUMB) é ignorado por agora
    }

    fun recognize(result: HandLandmarkerResult): String {
        // Se nenhuma mão for detectada, não há gesto
        if (result.landmarks().isEmpty()) {
            return "Nenhum"
        }

        // Pega os pontos da primeira mão detectada
        val landmarks = result.landmarks()[0]

        // Verifica primeiro se o gesto é um punho fechado
        if (isFist(landmarks)) {
            return "Punho Fechado"
        }

        // Se não for um punho, verifica quais dedos estão levantados
        val fingersUp = mutableListOf<String>()

        for (finger in Finger.values()) {
            if (isFingerUp(landmarks, finger)) {
                fingersUp.add(finger.fingerName)
            }
        }

        // Se a lista de dedos levantados não estiver vazia, retorna os nomes
        if (fingersUp.isNotEmpty()) {
            // Junta os nomes dos dedos com vírgula e espaço
            return "Dedo(s) levantado(s): ${fingersUp.joinToString(", ")}"
        }

        // Se nenhum dedo estiver levantado e não for um punho, retorna "Nenhum"
        return "Nenhum"
    }

    /**
     * Verifica se um dedo específico está levantado.
     * Um dedo é considerado "levantado" se a sua ponta (tip) estiver mais alta (menor valor Y)
     * do que a sua junta do meio (dip). Isso indica que o dedo está esticado.
     */
    private fun isFingerUp(landmarks: List<NormalizedLandmark>, finger: Finger): Boolean {
        val tip = landmarks[finger.tip]
        val dip = landmarks[finger.dip] // Usamos a junta DIP como referência
        return tip.y() < dip.y()
    }

    /**
     * Verifica se a mão está em formato de punho fechado.
     * Um punho é considerado "fechado" se a ponta de todos os quatro dedos
     * estiver mais baixa (maior valor Y) do que a sua junta principal (mcp).
     * Isso indica que os dedos estão dobrados para dentro da palma.
     */
    private fun isFist(landmarks: List<NormalizedLandmark>): Boolean {
        return Finger.values().all { finger ->
            val tip = landmarks[finger.tip]
            val mcp = landmarks[finger.mcp] // Usamos a junta MCP (base do dedo) como referência
            tip.y() > mcp.y()
        }
    }

    // Função para calcular a distância 2D entre dois pontos (não utilizada na lógica atual, mas pode ser útil)
    private fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
