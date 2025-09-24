package com.gesturecontrol.handtrackify.models

/**
 * Classe dedicada a gerir a lógica e o estado da simulação de gestos de rato.
 * Não possui nenhuma lógica de tempo para sua ativação; ele apenas processa
 * as poses quando o GestureRecognizer o invoca.
 */
class MouseGestureHandler {

    /**
     * Enum para gerir o estado interno e as transições dos gestos de clique.
     */
    private enum class ClickState {
        IDLE,           // Nenhum gesto de rato ativo, o módulo está inativo.
        PRIMED,         // "Preparado", pronto para iniciar um clique (gesto "paz e amor").
        INDEX_DOWN,     // Dedo indicador para baixo, aguardando para clique esquerdo.
        MIDDLE_DOWN,    // Dedo médio para baixo, aguardando para clique direito.
        RIGHT_CLICKED,  // Ação de clique direito foi acionada.
        LEFT_CLICKED    // Ação de clique esquerdo foi acionada.
    }

    // --- Variáveis de Estado ---
    private var fingerDownStartTime: Long = 0L
    private var clickState: ClickState = ClickState.IDLE
    private var clickResetTime: Long = 0L

    // --- Constantes de Tempo ---
    private val rightClickMaxTimeMs = 1400L
    private val leftClickMaxTimeMs = 1000L
    private val clickResetDelayMs = 500L

    /**
     * Processa a pose atual da mão para atualizar a máquina de estados do rato.
     * @param currentPose A string que representa o gesto atual da mão.
     */
    fun process(currentPose: String) {
        val currentTime = System.currentTimeMillis()

        if ((clickState == ClickState.RIGHT_CLICKED || clickState == ClickState.LEFT_CLICKED) && currentTime > clickResetTime) {
            clickState = if (currentPose == "PAZ_E_AMOR") ClickState.PRIMED else ClickState.IDLE
        }

        when (clickState) {
            ClickState.IDLE -> {
                if (currentPose == "PAZ_E_AMOR") clickState = ClickState.PRIMED
            }
            ClickState.PRIMED -> {
                when (currentPose) {
                    "LEFT_CLICK_TRIGGER_POSE" -> { // Indicador para baixo
                        clickState = ClickState.INDEX_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "RIGHT_CLICK_TRIGGER_POSE" -> { // Médio para baixo
                        clickState = ClickState.MIDDLE_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "PAZ_E_AMOR" -> { /* Mantém-se preparado */ }
                    else -> clickState = ClickState.IDLE
                }
            }
            ClickState.INDEX_DOWN -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> {
                        if (currentTime - fingerDownStartTime < leftClickMaxTimeMs) {
                            clickState = ClickState.LEFT_CLICKED
                            clickResetTime = currentTime + clickResetDelayMs
                        } else {
                            clickState = ClickState.PRIMED
                        }
                    }
                    "LEFT_CLICK_TRIGGER_POSE" -> { /* Aguarda o dedo levantar */ }
                    else -> clickState = ClickState.PRIMED
                }
            }
            ClickState.MIDDLE_DOWN -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> {
                        if (currentTime - fingerDownStartTime < rightClickMaxTimeMs) {
                            clickState = ClickState.RIGHT_CLICKED
                            clickResetTime = currentTime + clickResetDelayMs
                        } else {
                            clickState = ClickState.PRIMED
                        }
                    }
                    "RIGHT_CLICK_TRIGGER_POSE" -> { /* Aguardando */ }
                    else -> clickState = ClickState.PRIMED
                }
            }
            ClickState.RIGHT_CLICKED, ClickState.LEFT_CLICKED -> { /* Nenhuma ação necessária aqui */ }
        }
    }

    /**
     * Retorna a ação de rato atual como uma String.
     * @return Uma String com a ação ou null se o módulo estiver inativo.
     */
    fun getAction(): String? {
        return when (clickState) {
            ClickState.LEFT_CLICKED -> "Clique Esquerdo"
            ClickState.RIGHT_CLICKED -> "Clique Direito"
            ClickState.PRIMED, ClickState.INDEX_DOWN, ClickState.MIDDLE_DOWN -> "Preparado"
            ClickState.IDLE -> null
        }
    }

    /**
     * Reinicia o estado da máquina de estados do rato.
     */
    fun reset() {
        clickState = ClickState.IDLE
        fingerDownStartTime = 0L
        clickResetTime = 0L
    }
}

