package br.gov.sp.etec.lexicare

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Lembrete diário dos exercícios (estilo "Duolingo", mas sem encher o saco).
 *
 * Regras para não poluir:
 *  - no máximo UMA notificação por dia (id fixo + trava por data);
 *  - não avisa se a pessoa já praticou hoje, nem se ninguém estiver logado;
 *  - importância padrão (sem pop-up/barulho forte) e some sozinha antes do próximo lembrete;
 *  - o texto é positivo e varia a cada dia, sem culpa.
 */
class LembreteDiarioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1) Sempre agenda o de amanhã primeiro, aconteça o que acontecer abaixo.
        Notificacoes.agendarLembreteDiario(context)

        // 2) Condições para realmente mostrar hoje
        if (SessaoUsuario.email(context).isEmpty()) return
        if (Notificacoes.praticouHoje(context)) return
        if (Notificacoes.jaEnviouHoje(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        Notificacoes.criarCanais(context)

        val nome = context.getSharedPreferences(SessaoUsuario.PREFS, Context.MODE_PRIVATE)
            .getString("perfil_nome", "")
            ?.trim()?.split(" ")?.firstOrNull().orEmpty()
        val (titulo, texto) = mensagemDoDia(nome)

        val toque = Notificacoes.abrirComDashboard(context, ExerciciosActivity::class.java, Notificacoes.ID_LEMBRETE_DIARIO)

        val notificacao = NotificationCompat.Builder(context, Notificacoes.CANAL_EXERCICIOS)
            .setSmallIcon(R.drawable.ic_notificacao_livro)
            .setLargeIcon(Notificacoes.iconeGrande(context))
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setSubText("LexiCare")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setTimeoutAfter(TimeUnit.HOURS.toMillis(20)) // some antes do lembrete do dia seguinte
            .setContentIntent(toque)
            .addAction(0, "Praticar agora", toque)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(Notificacoes.ID_LEMBRETE_DIARIO, notificacao)
        Notificacoes.marcarEnviadoHoje(context)
    }

    private fun mensagemDoDia(nome: String): Pair<String, String> {
        val oi = if (nome.isNotEmpty()) ", $nome" else ""
        val opcoes = listOf(
            "📖 Hora de praticar$oi!" to "Poucos minutinhos de leitura já fazem diferença. Vamos lá?",
            "✏️ Seu momento de prática chegou" to "Um exercício rapidinho hoje e você segue evoluindo$oi.",
            "🌟 Pequenos passos, grandes conquistas" to "Faça um exercício hoje e continue sua sequência de aprendizado.",
            "🧠 Vamos exercitar a mente$oi?" to "Um exercício de leitura leva poucos minutos. Topa?",
            "🚀 Que tal praticar um pouquinho?" to "Seu progresso de hoje começa com um toque. Estamos te esperando!",
            "🎯 Missão do dia$oi" to "Complete um exercício e deixe o dia ainda melhor."
        )
        val dia = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return opcoes[dia % opcoes.size]
    }
}
