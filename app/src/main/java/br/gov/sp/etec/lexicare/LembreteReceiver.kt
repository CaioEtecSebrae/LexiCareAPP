package br.gov.sp.etec.lexicare

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Notificação de uma tarefa do calendário, na hora marcada. */
class LembreteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra("titulo")?.takeIf { it.isNotBlank() } ?: "Sua tarefa"
        val idTarefa = intent.getIntExtra("id", 0)
        val horario = intent.getStringExtra("horario").orEmpty()
        val descricao = intent.getStringExtra("descricao").orEmpty()
        val dificuldade = intent.getStringExtra("dificuldade").orEmpty()
        val corTarefa = intent.getStringExtra("cor")

        Notificacoes.criarCanais(context)

        // A cor da notificação é a cor escolhida para a tarefa (se for válida)
        val cor = try {
            if (corTarefa.isNullOrBlank()) throw IllegalArgumentException()
            Color.parseColor(corTarefa)
        } catch (e: IllegalArgumentException) {
            ContextCompat.getColor(context, R.color.primary)
        }

        val resumo = if (horario.isNotEmpty()) "$titulo • $horario" else titulo
        val detalhes = buildString {
            append(titulo)
            if (descricao.isNotBlank()) append("\n").append(descricao.trim())
            val rodape = listOf(
                if (horario.isNotEmpty()) "⏰ $horario" else "",
                if (dificuldade.isNotBlank()) "Nível $dificuldade" else ""
            ).filter { it.isNotEmpty() }.joinToString("  •  ")
            if (rodape.isNotEmpty()) append("\n").append(rodape)
        }

        // Ao tocar: abre o calendário e, ao voltar, cai no Dashboard
        val toque = Notificacoes.abrirComDashboard(context, CalendarioActivity::class.java, idTarefa)

        val notificacao = NotificationCompat.Builder(context, Notificacoes.CANAL_TAREFAS)
            .setSmallIcon(R.drawable.ic_notificacao_tarefa)
            .setLargeIcon(Notificacoes.iconeGrande(context))
            .setColor(cor)
            .setContentTitle("⏰ Hora da sua tarefa!")
            .setContentText(resumo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalhes))
            .setSubText("Calendário LexiCare")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(toque)
            .addAction(0, "Ver no calendário", toque)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(idTarefa, notificacao)
    }
}
