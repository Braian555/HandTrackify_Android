package com.gesturecontrol.handtrackify.vision

import com.gesturecontrol.handtrackify.models.MouseGestureHandler
import com.gesturecontrol.handtrackify.models.QuickActionHandler
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

/**
 * Classe principal para reconhecimento de gestos.
 * Atua como um orquestrador que deteta poses básicas e controla a ativação
 * de módulos de gestos complexos, como o MouseGestureHandler e o QuickActionHandler.
 */
class GestureRecognizer {

    private enum class Finger(val tip: Int, val mcp: Int, val dip: Int? = null, val fingerName: String) {
        THUMB(4, 2, 3, "Polegar"),
        INDEX(8, 5, 7, "Indicador"),
        MIDDLE(12, 9, 11, "Médio"),
        RING(16, 13, 15, "Anelar"),
        PINKY(20, 17, 19, "Mínimo")
    }

    private enum class OperatingMode {
        IDLE,
        AWAITING_MODE_SELECTION,
        MOUSE_MODE,
        QUICK_ACTION_MODE // Novo modo para ações rápidas
    }

    private val mouseGestureHandler = MouseGestureHandler()
    private val quickActionHandler = QuickActionHandler() // Novo módulo

    private var operatingMode = OperatingMode.IDLE
    private var poseStartTime: Long = 0L
    private val modeActivationDelayMs = 1000L // Tempo de espera para ativar o modo rato (1 segundo)

    fun recognize(result: HandLandmarkerResult): String {
        if (result.landmarks().isEmpty()) {
            resetAllModes()
            return "Nenhum"
        }

        val landmarks = result.landmarks()[0]
        val currentPose = detectPose(landmarks)
        val currentTime = System.currentTimeMillis()

        // --- Orquestrador Principal ---
        when (operatingMode) {
            OperatingMode.IDLE -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> {
                        operatingMode = OperatingMode.AWAITING_MODE_SELECTION
                        poseStartTime = currentTime
                    }
                    "QUICK_ACTION_PRIMED_POSE" -> {
                        // Ativa o modo de ações rápidas imediatamente
                        operatingMode = OperatingMode.QUICK_ACTION_MODE
                    }
                }
            }
            OperatingMode.AWAITING_MODE_SELECTION -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> {
                        if (currentTime - poseStartTime > modeActivationDelayMs) {
                            operatingMode = OperatingMode.MOUSE_MODE
                        }
                    }
                    "QUICK_ACTION_PRIMED_POSE" -> {
                        // Se o polegar for levantado durante a espera, ativa o modo de ações rápidas.
                        operatingMode = OperatingMode.QUICK_ACTION_MODE
                    }
                    else -> {
                        resetAllModes()
                    }
                }
            }
            OperatingMode.MOUSE_MODE -> {
                mouseGestureHandler.process(currentPose)
                if (mouseGestureHandler.getAction() == null) {
                    resetAllModes()
                }
            }
            OperatingMode.QUICK_ACTION_MODE -> {
                quickActionHandler.process(currentPose)
                if (quickActionHandler.getAction() == null) {
                    resetAllModes()
                }
            }
        }

        // --- Retorno da Ação ---
        return when (operatingMode) {
            OperatingMode.MOUSE_MODE -> mouseGestureHandler.getAction() ?: "Nenhum"
            OperatingMode.QUICK_ACTION_MODE -> quickActionHandler.getAction() ?: "Nenhum"
            OperatingMode.AWAITING_MODE_SELECTION -> "Aguardando..."
            OperatingMode.IDLE -> getIdleActionString(currentPose, landmarks)
        }
    }

    private fun getIdleActionString(currentPose: String, landmarks: List<NormalizedLandmark>): String {
        return when (currentPose) {
            "PUNHO" -> "Punho Fechado"
            "THUMB_UP" -> "DEDAO"
            "OUTRO" -> {
                val fingersUp = getFingersUp(landmarks)
                if (fingersUp.isNotEmpty()) "Dedo(s) levantado(s): ${fingersUp.joinToString(", ")}"
                else "Nenhum"
            }
            else -> "Nenhum"
        }
    }

    private fun resetAllModes() {
        operatingMode = OperatingMode.IDLE
        mouseGestureHandler.reset()
        quickActionHandler.reset()
    }

    private fun detectPose(landmarks: List<NormalizedLandmark>): String {
        val isIndexUp = isFingerUp(landmarks, Finger.INDEX)
        val isMiddleUp = isFingerUp(landmarks, Finger.MIDDLE)
        val isRingUp = isFingerUp(landmarks, Finger.RING)
        val isPinkyUp = isFingerUp(landmarks, Finger.PINKY)
        val isThumbUp = isThumbUp(landmarks)

        // Poses para o Módulo de Ações Rápidas
        if (isIndexUp && isMiddleUp && !isRingUp && !isPinkyUp && isThumbUp) return "QUICK_ACTION_PRIMED_POSE"
        if (isThumbUp && isMiddleUp && !isRingUp && !isPinkyUp && !isIndexUp) return "QUICK_ACTION_INDEX_DOWN_POSE"
        if (isThumbUp && isIndexUp && !isRingUp && !isPinkyUp && !isMiddleUp) return "QUICK_ACTION_MIDDLE_DOWN_POSE"

        // Poses para o Módulo de Rato
        if (isIndexUp && isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "PAZ_E_AMOR"
        if (!isIndexUp && isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "RIGHT_CLICK_TRIGGER_POSE"
        if (isIndexUp && !isMiddleUp && !isRingUp && !isPinkyUp && !isThumbUp) return "LEFT_CLICK_TRIGGER_POSE"

        // Gestos básicos do modo IDLE
        if (isFist(landmarks)) return "PUNHO"
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

    private fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
