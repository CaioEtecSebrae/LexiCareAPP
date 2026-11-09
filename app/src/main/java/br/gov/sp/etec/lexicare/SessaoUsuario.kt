package br.gov.sp.etec.lexicare

import android.content.Context
import android.content.SharedPreferences

/**
 * Separa os dados de cada usuário (anotações, tarefas...) pelo e-mail de quem fez login.
 *
 * Antes, todos os e-mails gravavam na mesma chave ("anotacoes", "tarefas") e por isso
 * enxergavam as mesmas coisas. Agora a chave vira "anotacoes_email@dominio.com", etc.
 */
object SessaoUsuario {

    const val PREFS = "LexiCarePrefs"
    private const val EMAIL_KEY = "email_logado"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun email(ctx: Context): String =
        prefs(ctx).getString(EMAIL_KEY, "")?.trim()?.lowercase() ?: ""

    /** Chave do usuário logado para um tipo de dado. Ex.: chave(ctx, "tarefas") -> "tarefas_ana@x.com" */
    fun chave(ctx: Context, base: String): String {
        val email = email(ctx)
        return if (email.isEmpty()) base else "${base}_$email"
    }

    /**
     * Dados salvos pela versão antiga (chave única para todo mundo) são entregues uma única vez
     * ao primeiro usuário que abrir a tela e depois a chave antiga é apagada.
     */
    private fun migrarLegado(ctx: Context, base: String) {
        val sp = prefs(ctx)
        val k = chave(ctx, base)
        if (k != base && !sp.contains(k) && sp.contains(base)) {
            sp.edit()
                .putString(k, sp.getString(base, ""))
                .remove(base)
                .apply()
        }
    }

    fun contem(ctx: Context, base: String): Boolean {
        migrarLegado(ctx, base)
        return prefs(ctx).contains(chave(ctx, base))
    }

    fun ler(ctx: Context, base: String): String? {
        migrarLegado(ctx, base)
        return prefs(ctx).getString(chave(ctx, base), null)
    }

    fun gravar(ctx: Context, base: String, json: String) {
        prefs(ctx).edit().putString(chave(ctx, base), json).apply()
    }

    /** Parte do e-mail segura para nome de arquivo (foto de perfil). */
    fun emailParaArquivo(ctx: Context): String =
        email(ctx).ifEmpty { "sem_login" }.replace(Regex("[^a-z0-9]"), "_")
}
