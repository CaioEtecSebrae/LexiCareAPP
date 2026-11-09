package br.gov.sp.etec.lexicare

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tudo sobre notificações do LexiCare num lugar só.
 *
 * Dois tipos, cada um no seu canal (a pessoa pode silenciar um sem perder o outro):
 *  - Tarefas do calendário: na hora marcada (alarme exato, canal de alta importância).
 *  - Lembrete diário dos exercícios: UMA vez por dia, no máximo, num horário fixo, sem som
 *    forte, e só se a pessoa ainda não praticou no dia.
 */
object Notificacoes {

    const val CANAL_TAREFAS = "lembrete_tarefas"       // já existia: mantém o id
    const val CANAL_EXERCICIOS = "lembrete_exercicios"

    /** Horário do lembrete diário (24h). Mude aqui se quiser outro horário. */
    const val HORA_LEMBRETE = 18
    const val MINUTO_LEMBRETE = 0

    const val ID_LEMBRETE_DIARIO = 7001 // id fixo: uma notificação nova substitui a anterior
    private const val CODIGO_ALARME_DIARIO = 7001

    private const val CHAVE_ULTIMO_ENVIO = "lembrete_diario_ultimo_envio"
    private const val CHAVE_ULTIMA_PRATICA = "ultima_pratica"
    private const val CHAVE_PERMISSAO_PEDIDA = "permissao_notificacao_pedida"

    private fun hoje(): String {
        val padrao = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMd")
        return SimpleDateFormat(padrao, Locale.getDefault()).format(Date())
    }

    fun criarCanais(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CANAL_TAREFAS, "Tarefas do calendário", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisa na hora das tarefas que você marcou no calendário"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_EXERCICIOS, "Lembrete dos exercícios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Um lembrete por dia para praticar leitura e caligrafia"
                setShowBadge(false)
            }
        )
    }

    // ------------------------------------------------------------- prática do dia

    /** Chamar quando a pessoa termina um exercício (leitura ou caligrafia). */
    fun registrarPratica(ctx: Context) {
        SessaoUsuario.gravar(ctx, CHAVE_ULTIMA_PRATICA, hoje())
    }

    fun praticouHoje(ctx: Context): Boolean = SessaoUsuario.ler(ctx, CHAVE_ULTIMA_PRATICA) == hoje()

    fun jaEnviouHoje(ctx: Context): Boolean =
        ctx.getSharedPreferences(SessaoUsuario.PREFS, Context.MODE_PRIVATE)
            .getString(CHAVE_ULTIMO_ENVIO, null) == hoje()

    fun marcarEnviadoHoje(ctx: Context) {
        ctx.getSharedPreferences(SessaoUsuario.PREFS, Context.MODE_PRIVATE)
            .edit().putString(CHAVE_ULTIMO_ENVIO, hoje()).apply()
    }

    private fun intentAlarmeDiario(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, CODIGO_ALARME_DIARIO,
            Intent(ctx, LembreteDiarioReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Próximo horário do lembrete: hoje, se ainda não passou; senão, amanhã. */
    private fun proximoHorario(): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HORA_LEMBRETE)
            set(Calendar.MINUTE, MINUTO_LEMBRETE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /**
     * Agenda o próximo lembrete diário. Pode ser chamado quantas vezes quiser: o alarme tem
     * sempre o mesmo código, então só existe um agendado (nunca acumula).
     * Usa alarme "inexato", que não precisa de permissão especial e poupa bateria.
     */
    fun agendarLembreteDiario(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximoHorario(), intentAlarmeDiario(ctx))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Pede a permissão de notificação (Android 13+) uma única vez, ao entrar no Dashboard. */
    fun pedirPermissaoSeNecessario(activity: android.app.Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val sp = activity.getSharedPreferences(SessaoUsuario.PREFS, Context.MODE_PRIVATE)
        if (sp.getBoolean(CHAVE_PERMISSAO_PEDIDA, false)) return
        sp.edit().putBoolean(CHAVE_PERMISSAO_PEDIDA, true).apply()
        activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102)
    }

    // ------------------------------------------------------------- utilidades

    /** Ao tocar na notificação: abre a tela e, ao voltar, cai no Dashboard (não fecha o app). */
    fun abrirComDashboard(ctx: Context, destino: Class<*>, codigo: Int): PendingIntent? =
        TaskStackBuilder.create(ctx)
            .addNextIntent(Intent(ctx, DashboardActivity::class.java))
            .addNextIntent(Intent(ctx, destino))
            .getPendingIntent(codigo, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Ícone colorido do app, usado como imagem grande ao lado do texto da notificação. */
    fun iconeGrande(ctx: Context): Bitmap? = try {
        val d = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)!!
        val tamanho = (ctx.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)
        val bmp = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        d.setBounds(0, 0, tamanho, tamanho)
        d.draw(canvas)
        bmp
    } catch (e: Exception) {
        null
    }
}
