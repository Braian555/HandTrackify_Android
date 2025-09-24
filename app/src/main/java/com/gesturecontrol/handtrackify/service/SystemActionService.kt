package com.gesturecontrol.handtrackify.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class SystemActionService : AccessibilityService() {

    companion object {
        private var instance: SystemActionService? = null

        // Função que a nossa MainActivity vai chamar para executar a ação
        fun performBackAction() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("SystemActionService", "Ação de 'Voltar' executada.")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Não precisamos de reagir a eventos do sistema, apenas de executar ações
    }

    override fun onInterrupt() {
        // Chamado quando o serviço é interrompido
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("SystemActionService", "Serviço de Acessibilidade conectado.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.d("SystemActionService", "Serviço de Acessibilidade desconectado.")
        return super.onUnbind(intent)
    }
}
