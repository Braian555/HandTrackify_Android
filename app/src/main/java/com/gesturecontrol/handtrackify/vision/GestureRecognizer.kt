package com.gesturecontrol.handtrackify.vision

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

class GestureRecognizer {
    // Enum para facilitar a referência aos landmarks dos dedos
    private enum class Finger(val tip: Int, val mcp: Int, val dip: Int? = null, val fingerName: String) {
        THUMB(4, 2, 3, "Polegar"),
        INDEX(8, 5, 7, "Indicador"),
        MIDDLE(12, 9, 11, "Médio"),
        RING(16, 13, 15, "Anelar"),
        PINKY(20, 17, 19, "Mínimo")
    }

    // Enum para gerir o estado complexo dos gestos de clique
    private enum class ClickState {
        IDLE,               // Estado neutro
        PRIMED,             // "Preparado", pronto para iniciar um clique
        INDEX_DOWN,         // Dedo indicador para baixo, aguardando para clique direito
        MIDDLE_DOWN,        // Dedo médio para baixo, aguardando para clique esquerdo
        RIGHT_CLICKED,      // Clique direito acionado
        LEFT_CLICKED        // Clique esquerdo acionado
    }

    // --- Variáveis de Estado ---
    private var lastDetectedPose: String = "Nenhum"
    private var fingerDownStartTime: Long = 0L // para medir a duração do clique
    private var clickState: ClickState = ClickState.IDLE
    private var clickResetTime: Long = 0L

    // --- Constantes de Tempo ---
    private val clickMaxTimeMs = 1400L     // 1.4 segundos como tempo máximo para um clique direito válido
    private val leftClickMaxTimeMs = 1000L // 1.0 segundo como tempo máximo para um clique esquerdo válido
    private val clickResetDelayMs = 500L   // 0.5 segundos para o estado de clique ser visível

    fun recognize(result: HandLandmarkerResult): String {
        // Se a mão não for detetada, reinicia tudo.
        if (result.landmarks().isEmpty()) {
            resetState()
            return "Nenhum"
        }

        val landmarks = result.landmarks()[0]
        val currentPose = detectPose(landmarks)

        updateClickState(currentPose, landmarks)

        lastDetectedPose = currentPose
        return getActionString(currentPose, landmarks)
    }

    private fun updateClickState(currentPose: String, landmarks: List<NormalizedLandmark>) {
        val currentTime = System.currentTimeMillis()

        // Lógica de expiração para os estados de clique
        if ((clickState == ClickState.RIGHT_CLICKED || clickState == ClickState.LEFT_CLICKED) && currentTime > clickResetTime) {
            clickState = if (currentPose == "PAZ_E_AMOR") ClickState.PRIMED else ClickState.IDLE
        }

        when (clickState) {
            ClickState.IDLE -> {
                if (currentPose == "PAZ_E_AMOR") clickState = ClickState.PRIMED
            }
            ClickState.PRIMED -> {
                when (currentPose) {
                    "RIGHT_CLICK_TRIGGER_POSE" -> {
                        clickState = ClickState.INDEX_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "LEFT_CLICK_TRIGGER_POSE" -> {
                        clickState = ClickState.MIDDLE_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "PAZ_E_AMOR" -> { /* Mantém-se preparado */ }
                    else -> clickState = ClickState.IDLE
                }
            }
            ClickState.INDEX_DOWN -> {
                when (currentPose) {
                    "RIGHT_CLICK_TRIGGER_POSE" -> { /* Aguarda o dedo levantar */ }
                    "PAZ_E_AMOR" -> {
                        if (currentTime - fingerDownStartTime < clickMaxTimeMs) {
                            clickState = ClickState.RIGHT_CLICKED
                            clickResetTime = currentTime + clickResetDelayMs
                        } else {
                            clickState = ClickState.PRIMED
                        }
                    }
                    else -> clickState = ClickState.IDLE
                }
            }
            ClickState.MIDDLE_DOWN -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> {
                        if (currentTime - fingerDownStartTime < leftClickMaxTimeMs) {
                            clickState = ClickState.LEFT_CLICKED
                            clickResetTime = currentTime + clickResetDelayMs
                        } else {
                            clickState = ClickState.PRIMED
                        }
                    }
                    "LEFT_CLICK_TRIGGER_POSE" -> { /* Aguardando */ }
                    else -> clickState = ClickState.IDLE
                }
            }
            ClickState.RIGHT_CLICKED, ClickState.LEFT_CLICKED -> { /* Lógica de timeout tratada no início */ }
        }
    }

    private fun getActionString(currentPose: String, landmarks: List<NormalizedLandmark>): String {
        return when (clickState) {
            ClickState.RIGHT_CLICKED -> "Clique Direito"
            ClickState.LEFT_CLICKED -> "Clique Esquerdo"
            ClickState.PRIMED, ClickState.INDEX_DOWN, ClickState.MIDDLE_DOWN -> "Preparado"
            ClickState.IDLE -> when (currentPose) {
                "PUNHO" -> "Punho Fechado"
                "THUMB_UP" -> "Polegar Levantado"
                "OUTRO" -> {
                    val fingersUp = getFingersUp(landmarks)
                    if (fingersUp.isNotEmpty()) "Dedo(s) levantado(s): ${fingersUp.joinToString(", ")}"
                    else "Nenhum"
                }
                else -> "Nenhum"
            }
        }
    }

    private fun detectPose(landmarks: List<NormalizedLandmark>): String {
        val isIndexUp = isFingerUp(landmarks, Finger.INDEX)
        val isMiddleUp = isFingerUp(landmarks, Finger.MIDDLE)
        val isRingUp = isFingerUp(landmarks, Finger.RING)
        val isPinkyUp = isFingerUp(landmarks, Finger.PINKY)
        val isThumbUp = isThumbUp(landmarks)

        if (isFist(landmarks)) return "PUNHO"
        if (isIndexUp && isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "PAZ_E_AMOR"
        if (!isIndexUp && isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "RIGHT_CLICK_TRIGGER_POSE"
        if (isIndexUp && !isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "LEFT_CLICK_TRIGGER_POSE"
        if (isThumbUp && !isIndexUp && !isMiddleUp && !isRingUp && !isPinkyUp) return "THUMB_UP"
        if (isIndexUp || isMiddleUp || isRingUp || isPinkyUp || isThumbUp) return "OUTRO"
        return "Nenhum"
    }

    private fun getFingersUp(landmarks: List<NormalizedLandmark>): List<String> {
        val fingers = mutableListOf<String>()
        Finger.entries.filter { it != Finger.THUMB }.forEach { finger ->
            if (isFingerUp(landmarks, finger)) fingers.add(finger.fingerName)
        }
        if (isThumbUp(landmarks)) fingers.add(Finger.THUMB.fingerName)
        return fingers
    }

    private fun isThumbUp(landmarks: List<NormalizedLandmark>): Boolean {
        val thumbTip = landmarks[Finger.THUMB.tip]
        val thumbMcp = landmarks[Finger.THUMB.mcp]
        val indexMcp = landmarks[Finger.INDEX.mcp]
        val tipDist = calculateDistance(thumbTip, indexMcp)
        val mcpDist = calculateDistance(thumbMcp, indexMcp)
        return tipDist > (mcpDist * 0.9f)
    }

    private fun isFingerUp(landmarks: List<NormalizedLandmark>, finger: Finger): Boolean {
        if (finger == Finger.THUMB || finger.dip == null) return false
        val tip = landmarks[finger.tip]
        val dip = landmarks[finger.dip]
        return tip.y() < dip.y()
    }

    private fun isFist(landmarks: List<NormalizedLandmark>): Boolean {
        val fourFingersDown = Finger.entries.filter { it != Finger.THUMB }.all { finger ->
            val tip = landmarks[finger.tip]
            val mcp = landmarks[finger.mcp]
            tip.y() > mcp.y()
        }
        return fourFingersDown && !isThumbUp(landmarks)
    }

    private fun resetState() {
        lastDetectedPose = "Nenhum"
        fingerDownStartTime = 0L
        clickState = ClickState.IDLE
        clickResetTime = 0L
    }

    private fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}

