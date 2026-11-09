package br.gov.sp.etec.lexicare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarmes se perdem quando o celular reinicia ou o app é atualizado: reagenda o lembrete diário. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notificacoes.agendarLembreteDiario(context)
    }
}
