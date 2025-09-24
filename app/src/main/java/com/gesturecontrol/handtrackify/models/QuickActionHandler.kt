package com.gesturecontrol.handtrackify.models

/**
 * Módulo dedicado a gerir a lógica e o estado das ações rápidas (atalhos)
 * ativadas com o gesto de três dedos (polegar, indicador e médio).
 */
class QuickActionHandler {

    /**
     * Enum para gerir o estado interno das ações rápidas.
     */
    private enum class State {
        IDLE,               // Módulo inativo.
        PRIMED,             // Modo de 3 dedos detetado, pronto para uma ação.
        THUMB_DOWN,         // Polegar abaixado, aguardando para acionar "Voltar".
        INDEX_DOWN,         // Indicador abaixado, aguardando por "Multitarefas".
        MIDDLE_DOWN,        // Médio abaixado, aguardando por "Alt+Tab".
        ACTION_TRIGGERED,   // Ação rápida (Voltar/Multitarefas) foi concluída.
        ALT_TAB_ACTIVE,     // Estado de espera do Alt+Tab.
        ALT_TAB_SELECTED    // Estado para quando o Alt+Tab é confirmado.
    }

    // --- Variáveis de Estado ---
    private var state: State = State.IDLE
    private var fingerDownStartTime: Long = 0L
    private var actionTriggeredTime: Long = 0L
    private var lastActionString: String = ""
    private var altTabTimeout: Long = 0L

    // --- Constantes de Tempo ---
    private val clickResetDelayMs = 500L
    private val quickActionMaxTimeMs = 1400L
    private val altTabSelectionDelayMs = 1400L

    /**
     * Processa a pose atual da mão para atualizar a máquina de estados das ações rápidas.
     * @param currentPose A string que representa o gesto atual da mão.
     */
    fun process(currentPose: String) {
        val currentTime = System.currentTimeMillis()

        // Lógica para resetar o estado após uma ação ter sido exibida.
        if ((state == State.ACTION_TRIGGERED || state == State.ALT_TAB_SELECTED) && currentTime > actionTriggeredTime + clickResetDelayMs) {
            state = if (currentPose == "QUICK_ACTION_PRIMED_POSE") State.PRIMED else State.IDLE
        }

        // Lógica de timeout para a seleção do Alt+Tab.
        if (state == State.ALT_TAB_ACTIVE && currentTime > altTabTimeout) {
            state = State.ALT_TAB_SELECTED
            actionTriggeredTime = currentTime
            lastActionString = "Selecionado"
        }

        when (state) {
            State.IDLE -> {
                if (currentPose == "QUICK_ACTION_PRIMED_POSE") state = State.PRIMED
            }
            State.PRIMED -> {
                when (currentPose) {
                    "PAZ_E_AMOR" -> { // Polegar para baixo
                        state = State.THUMB_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "QUICK_ACTION_INDEX_DOWN_POSE" -> {
                        state = State.INDEX_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "QUICK_ACTION_MIDDLE_DOWN_POSE" -> {
                        state = State.MIDDLE_DOWN
                        fingerDownStartTime = currentTime
                    }
                    "QUICK_ACTION_PRIMED_POSE" -> { /* Mantém-se preparado */ }
                    else -> state = State.IDLE // Sai do modo se o gesto for desfeito
                }
            }
            State.THUMB_DOWN -> if (currentPose == "QUICK_ACTION_PRIMED_POSE" && currentTime - fingerDownStartTime < quickActionMaxTimeMs) {
                triggerAction("Voltar", currentTime)
            } else if (currentPose != "PAZ_E_AMOR") {
                state = State.PRIMED // Cancela se o gesto mudar
            }
            State.INDEX_DOWN -> if (currentPose == "QUICK_ACTION_PRIMED_POSE" && currentTime - fingerDownStartTime < quickActionMaxTimeMs) {
                triggerAction("Multitarefas", currentTime)
            } else if (currentPose != "QUICK_ACTION_INDEX_DOWN_POSE") {
                state = State.PRIMED
            }
            State.MIDDLE_DOWN -> if (currentPose == "QUICK_ACTION_PRIMED_POSE" && currentTime - fingerDownStartTime < quickActionMaxTimeMs) {
                state = State.ALT_TAB_ACTIVE
                lastActionString = "Alt + Tab"
                altTabTimeout = currentTime + altTabSelectionDelayMs
            } else if (currentPose != "QUICK_ACTION_MIDDLE_DOWN_POSE") {
                state = State.PRIMED
            }
            State.ALT_TAB_ACTIVE -> if (currentPose == "QUICK_ACTION_MIDDLE_DOWN_POSE") {
                // Permite acionar o próximo "Alt+Tab"
                state = State.MIDDLE_DOWN
                fingerDownStartTime = currentTime
            }
            else -> { /* Não faz nada nos estados finais até o reset */ }
        }
    }

    /**
     * Função auxiliar para definir o estado de uma ação acionada.
     */
    private fun triggerAction(action: String, currentTime: Long) {
        state = State.ACTION_TRIGGERED
        lastActionString = action
        actionTriggeredTime = currentTime
    }

    /**
     * Retorna a ação atual do módulo como uma String.
     * @return A String da ação ou null se o módulo estiver inativo.
     */
    fun getAction(): String? {
        return when (state) {
            State.ACTION_TRIGGERED, State.ALT_TAB_SELECTED -> lastActionString
            State.ALT_TAB_ACTIVE -> lastActionString
            State.PRIMED, State.THUMB_DOWN, State.INDEX_DOWN, State.MIDDLE_DOWN -> "Preparado (Ações)"
            State.IDLE -> null // Retorna null para indicar inatividade
        }
    }

    /**
     * Reinicia o estado do módulo.
     */
    fun reset() {
        state = State.IDLE
    }
}

