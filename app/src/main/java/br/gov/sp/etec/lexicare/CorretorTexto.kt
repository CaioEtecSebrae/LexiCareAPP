package br.gov.sp.etec.lexicare

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Corretor de português do botão "Corrigir" das Anotações.
 *
 * Usa a API gratuita do LanguageTool (pt-BR), que tem dicionário e regras de gramática do
 * português. Ela devolve, para cada problema, o trecho e a sugestão de troca, e com isso o app:
 *   - CORRIGE palavras erradas e acentuação ("caza" -> "casa", "voce" -> "você");
 *   - TIRA palavras sobrando (palavra repetida: "o o gato" -> "o gato");
 *   - ADICIONA palavras que faltam, quando a regra sugere (ex.: crase/preposição/artigo).
 * Funciona com muitas palavras de uma vez: textos grandes são enviados em partes.
 *
 * Sem internet (ou se a API estiver fora do ar) o app usa a correção básica offline.
 */
object CorretorTexto {

    private const val URL_API = "https://api.languagetool.org/v2/check"
    private const val IDIOMA = "pt-BR"
    private const val TAMANHO_PARTE = 12_000 // limite da API gratuita é 20 mil caracteres por pedido

    class Resultado(
        val texto: String,
        val correcoes: Int,
        val offline: Boolean
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())

    /** Corrige em segundo plano e devolve o resultado na thread principal. */
    fun corrigir(texto: String, aoTerminar: (Resultado) -> Unit) {
        executor.execute {
            val resultado = try {
                corrigirOnline(texto)
            } catch (e: Exception) {
                corrigirOffline(texto)
            }
            principal.post { aoTerminar(resultado) }
        }
    }

    // ---------------------------------------------------------------- ONLINE

    private fun corrigirOnline(texto: String): Resultado {
        var total = 0
        val saida = StringBuilder()
        for (parte in dividir(texto)) {
            val (corrigida, qtd) = corrigirParte(parte)
            saida.append(corrigida)
            total += qtd
        }
        return Resultado(saida.toString(), total, offline = false)
    }

    private fun corrigirParte(parte: String): Pair<String, Int> {
        if (parte.isBlank()) return parte to 0

        val corpo = "language=" + URLEncoder.encode(IDIOMA, "UTF-8") +
                "&text=" + URLEncoder.encode(parte, "UTF-8")

        val conexao = URL(URL_API).openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = "POST"
            conexao.connectTimeout = 8_000
            conexao.readTimeout = 15_000
            conexao.doOutput = true
            conexao.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            conexao.setRequestProperty("Accept", "application/json")
            conexao.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }

            if (conexao.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${conexao.responseCode}")
            }
            val json = conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return aplicar(parte, json)
        } finally {
            conexao.disconnect()
        }
    }

    /** Aplica as sugestões da API de trás para frente, para os índices não se desencontrarem. */
    private fun aplicar(texto: String, json: String): Pair<String, Int> {
        val achados = JsonParser.parseString(json).asJsonObject.getAsJsonArray("matches")
            ?: return texto to 0

        class Troca(val inicio: Int, val fim: Int, val novo: String)

        val trocas = mutableListOf<Troca>()
        for (el in achados) {
            val m = el.asJsonObject
            val tipo = m.getAsJsonObject("rule")?.get("issueType")?.asString ?: ""
            if (tipo == "style") continue // só erros de verdade, não sugestões de estilo

            val sugestoes = m.getAsJsonArray("replacements") ?: continue
            if (sugestoes.size() == 0) continue
            val novo = sugestoes[0].asJsonObject.get("value").asString

            val inicio = m.get("offset").asInt
            val fim = inicio + m.get("length").asInt
            if (inicio < 0 || fim > texto.length || inicio > fim) continue
            if (texto.substring(inicio, fim) == novo) continue
            trocas.add(Troca(inicio, fim, novo))
        }

        val resultado = StringBuilder(texto)
        var aplicadas = 0
        var limite = Int.MAX_VALUE // início da última troca feita (evita trocas sobrepostas)
        for (t in trocas.sortedByDescending { it.inicio }) {
            if (t.fim > limite) continue
            resultado.replace(t.inicio, t.fim, t.novo)
            // Palavra removida: não deixa dois espaços seguidos no lugar dela.
            if (t.novo.isEmpty() && t.inicio > 0 && t.inicio < resultado.length &&
                resultado[t.inicio - 1] == ' ' && resultado[t.inicio] == ' '
            ) {
                resultado.deleteCharAt(t.inicio)
            }
            limite = t.inicio
            aplicadas++
        }
        return resultado.toString() to aplicadas
    }

    /** Divide textos grandes em partes, sempre em quebra de linha ou espaço (nunca no meio de palavra). */
    private fun dividir(texto: String): List<String> {
        if (texto.length <= TAMANHO_PARTE) return listOf(texto)
        val partes = mutableListOf<String>()
        var inicio = 0
        while (inicio < texto.length) {
            var fim = minOf(inicio + TAMANHO_PARTE, texto.length)
            if (fim < texto.length) {
                val corte = maxOf(texto.lastIndexOf('\n', fim), texto.lastIndexOf(' ', fim))
                if (corte > inicio) fim = corte + 1
            }
            partes.add(texto.substring(inicio, fim))
            inicio = fim
        }
        return partes
    }

    private val correcoesBasicas = mapOf(
        "caza" to "casa", "concerteza" to "com certeza", "atraz" to "atrás",
        "extrangeiro" to "estrangeiro", "frustado" to "frustrado",
        "tbm" to "também", "tb" to "também", "vcs" to "vocês", "vc" to "você", "pq" to "porque",
        "voce" to "você", "so" to "só", "nao" to "não", "mae" to "mãe", "pao" to "pão",
        "acao" to "ação", "criacao" to "criação", "informacao" to "informação",
        "educacao" to "educação", "comunicacao" to "comunicação", "atencao" to "atenção",
        "profissao" to "profissão"
    )

    private val letras = "a-zA-ZáàâãéèêíïóôõöúçñÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ"
    private val regexPalavra = Regex("[$letras]+")

    fun corrigirOffline(texto: String): Resultado {
        var total = 0
        val corrigido = regexPalavra.replace(texto) { m ->
            val original = m.value
            val troca = correcoesBasicas[original.lowercase()]
            if (troca == null) {
                original
            } else {
                total++
                if (original[0].isUpperCase()) troca.replaceFirstChar { it.uppercase() } else troca
            }
        }
        return Resultado(corrigido, total, offline = true)
    }
}
