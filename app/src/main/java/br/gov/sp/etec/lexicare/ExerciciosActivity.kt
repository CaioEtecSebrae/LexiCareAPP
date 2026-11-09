package br.gov.sp.etec.lexicare

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import kotlin.random.Random

class ExerciciosActivity : BaseActivity() {

    private val silabas = listOf(
        // Famílias silábicas simples
        "BA", "BE", "BI", "BO", "BU", "BÃO",
        "CA", "CE", "CI", "CO", "CÃO",
        "DA", "DE", "DI", "DO", "DU", "DÃO",
        "FA", "FE", "FI", "FO", "FU", "FÃO",
        "GA", "GE", "GI", "GO", "GU", "GÃO",
        "HA", "HE", "HI", "HO", "HU",
        "JA", "JE", "JI", "JO", "JU", "JÃO",
        "LA", "LE", "LI", "LO", "LU", "LÃO",
        "MA", "ME", "MI", "MO", "MU", "MÃO",
        "NA", "NE", "NI", "NO", "NU", "NÃO",
        "PA", "PE", "PI", "PO", "PU", "PÃO",
        "RA", "RE", "RI", "RO", "RU", "RÃO",
        "SA", "SE", "SI", "SO", "SU", "SÃO",
        "TA", "TE", "TI", "TO", "TU", "TÃO",
        "VA", "VE", "VI", "VO", "VU", "VÃO",
        "XA", "XE", "XI", "XO", "XU", "XÃO",
        "ZA", "ZE", "ZI", "ZO", "ZU", "ZÃO",

        // Encontros consonantais com R
        "BRA", "BRE", "BRI", "BRO", "BRU",
        "CRA", "CRE", "CRI", "CRO", "CRU",
        "DRA", "DRE", "DRI", "DRO", "DRU",
        "FRA", "FRE", "FRI", "FRO", "FRU",
        "GRA", "GRE", "GRI", "GRO", "GRU",
        "PRA", "PRE", "PRI", "PRO", "PRU",
        "TRA", "TRE", "TRI", "TRO", "TRU",
        "VRA", "VRE", "VRI", "VRO",

        // Encontros consonantais com L
        "BLA", "BLE", "BLI", "BLO", "BLU",
        "CLA", "CLE", "CLI", "CLO", "CLU",
        "FLA", "FLE", "FLI", "FLO", "FLU",
        "GLA", "GLE", "GLI", "GLO", "GLU",
        "PLA", "PLE", "PLI", "PLO", "PLU",

        // Dígrafos
        "CHA", "CHE", "CHI", "CHO", "CHU",
        "LHA", "LHE", "LHI", "LHO", "LHU",
        "NHA", "NHE", "NHI", "NHO", "NHU",

        // Dígrafos com GU e QU
        "GUA", "GUE", "GUI",
        "QUA", "QUE", "QUI",

        // Ditongos e vogais nasais
        "AI", "AU", "EI", "EU", "OI", "OU", "IU", "UI",
        "ÃE", "ÃO", "ÕE"
    )

    // Nível 2: palavras de no máximo 8 letras (cabem inteiras em qualquer tela), com acentuação correta
    private val palavras = listOf(
        "CASA", "MESA", "BOLA", "GATO", "PATO",
        "SOL", "LUA", "MAR", "FLOR", "NUVEM",
        "AMIGO", "FELIZ", "BONITO", "GRANDE", "PEQUENO",
        "FAMÍLIA", "ESCOLA", "JANELA", "CADERNO", "LIVRO",
        "GIRAFA", "JACARÉ", "ELEFANTE", "OCEANO", "PIPOCA",
        "ABACAXI", "MELANCIA", "TELEFONE", "GUITARRA", "TRABALHO",
        "ESTRADA", "CRIANÇA", "BRASIL", "FLORESTA", "MONTANHA",
        "PALAVRA", "TEXTO", "PROBLEMA", "DIFÍCIL", "INCRÍVEL",
        "PRÓXIMO", "FRACASSO", "EXTRA", "ÁRVORE", "CAVALO",
        "JARDIM", "BANANA", "LARANJA", "PEIXE", "COELHO",
        "CARRO", "LIMÃO", "FOGUETE", "ESTRELA", "PRAIA",
        "CAMINHO", "BRINCAR", "LEITURA", "SORRISO", "AMIZADE",
        "CORAÇÃO", "LÁPIS", "ESCREVER", "AVENTURA", "VERMELHO"
    ).filter { it.length <= 8 }

    private val frases = listOf(
        "O GATO DORME NO SOFÁ",
        "A CRIANÇA BRINCA NO PARQUE",
        "O SOL NASCE TODOS OS DIAS",
        "MEU AMIGO ESTUDA MUITO",
        "A FAMÍLIA JANTA JUNTA",
        "O PROFESSOR EXPLICA A LIÇÃO",
        "O CACHORRO CORRE NO QUINTAL",
        "A CHUVA CAI FORTE LÁ FORA",
        "O LIVRO ESTÁ SOBRE A MESA",
        "NÓS VAMOS À ESCOLA DE MANHÃ",
        "ELA CANTA UMA LINDA CANÇÃO",
        "O BRASIL É UM PAÍS GRANDE",
        "A FELICIDADE ESTÁ NAS PEQUENAS COISAS",
        "O RELÂMPAGO ILUMINOU O CÉU",
        "PRECISAMOS RESOLVER ESSE PROBLEMA"
    )

    private var nivelAtual = 1
    private var palavraAtual = ""
    private var acertos = 0
    private var totalTentativas = 0
    private val maxTentativas = 10

    // Cada exercício vale UMA tentativa: depois da primeira resposta ele fica travado
    // (cliques rápidos não contam de novo) e o progresso nunca passa de 10/10.
    private var respondido = false
    private var parabensAberto = false
    private val usadas = mutableSetOf<String>() // exercícios já mostrados nesta rodada (não repetem)
    private val handler = Handler(Looper.getMainLooper())

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("nivelAtual", nivelAtual)
        outState.putInt("acertos", acertos)
        outState.putInt("totalTentativas", totalTentativas)
        outState.putStringArrayList("usadas", ArrayList(usadas))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercicios)

        // Ao girar a tela ou redimensionar a janela a Activity é recriada: mantém nível e pontuação
        savedInstanceState?.let {
            nivelAtual = it.getInt("nivelAtual", 1)
            acertos = it.getInt("acertos", 0)
            totalTentativas = it.getInt("totalTentativas", 0)
            usadas.addAll(it.getStringArrayList("usadas") ?: emptyList())
        }

        // Girou a tela justamente depois da 10ª resposta: segue direto para os parabéns
        if (totalTentativas >= maxTentativas) {
            irParaParabens()
            return
        }

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val btnNivelFacil = findViewById<Button>(R.id.btnNivelFacil)
        val btnNivelMedio = findViewById<Button>(R.id.btnNivelMedio)
        val btnNivelDificil = findViewById<Button>(R.id.btnNivelDificil)
        val tvPalavra = findViewById<TextView>(R.id.tvPalavra)
        val tvFeedback = findViewById<TextView>(R.id.tvFeedback)
        val tvProgresso = findViewById<TextView>(R.id.tvProgresso)
        val tvNivelAtual = findViewById<TextView>(R.id.tvNivelAtual)
        val btnOpcao1 = findViewById<Button>(R.id.btnOpcao1)
        val btnOpcao2 = findViewById<Button>(R.id.btnOpcao2)
        val btnOpcao3 = findViewById<Button>(R.id.btnOpcao3)
        val btnOpcao4 = findViewById<Button>(R.id.btnOpcao4)

        btnNivelFacil.setOnClickListener {
            nivelAtual = 1
            reiniciarJogo(tvPalavra, tvFeedback, tvProgresso, tvNivelAtual, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
        }

        btnNivelMedio.setOnClickListener {
            nivelAtual = 2
            reiniciarJogo(tvPalavra, tvFeedback, tvProgresso, tvNivelAtual, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
        }

        btnNivelDificil.setOnClickListener {
            nivelAtual = 3
            reiniciarJogo(tvPalavra, tvFeedback, tvProgresso, tvNivelAtual, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
        }

        atualizarNivel(tvNivelAtual)
        carregarNovaPalavra(tvPalavra, tvFeedback, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
        atualizarProgresso(tvProgresso)

        fun verificarResposta(respostaEscolhida: String, button: Button) {
            // Já respondeu este exercício (ou já completou os 10): ignora cliques extras
            if (respondido || totalTentativas >= maxTentativas) return
            respondido = true
            totalTentativas++

            val acertou = respostaEscolhida == palavraAtual
            if (acertou) {
                acertos++
                tvFeedback.text = "Correto!"
                tvFeedback.setTextColor(Color.GREEN)
                button.setBackgroundColor(Color.GREEN)
            } else {
                tvFeedback.text = "Tente novamente!"
                tvFeedback.setTextColor(Color.RED)
                button.setBackgroundColor(Color.RED)
            }
            atualizarProgresso(tvProgresso)

            val foiOUltimo = totalTentativas >= maxTentativas
            handler.postDelayed({
                if (foiOUltimo) {
                    irParaParabens()
                } else {
                    carregarNovaPalavra(tvPalavra, tvFeedback, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
                }
            }, if (acertou) 1000L else 1500L)
        }

        btnVoltar.setOnClickListener { finish() }

        btnOpcao1.setOnClickListener {
            verificarResposta(btnOpcao1.text.toString(), btnOpcao1)
        }
        btnOpcao2.setOnClickListener {
            verificarResposta(btnOpcao2.text.toString(), btnOpcao2)
        }
        btnOpcao3.setOnClickListener {
            verificarResposta(btnOpcao3.text.toString(), btnOpcao3)
        }
        btnOpcao4.setOnClickListener {
            verificarResposta(btnOpcao4.text.toString(), btnOpcao4)
        }
    }

    private fun reiniciarJogo(
        tvPalavra: TextView,
        tvFeedback: TextView,
        tvProgresso: TextView,
        tvNivelAtual: TextView,
        btnOpcao1: Button,
        btnOpcao2: Button,
        btnOpcao3: Button,
        btnOpcao4: Button
    ) {
        handler.removeCallbacksAndMessages(null)
        acertos = 0
        totalTentativas = 0
        usadas.clear()
        respondido = false
        atualizarNivel(tvNivelAtual)
        carregarNovaPalavra(tvPalavra, tvFeedback, btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4)
        atualizarProgresso(tvProgresso)
    }

    private fun atualizarNivel(tvNivelAtual: TextView) {
        val nomeNivel = when (nivelAtual) {
            1 -> "Fácil - Sílabas"
            2 -> "Médio - Palavras"
            3 -> "Difícil - Frases"
            else -> "Desconhecido"
        }
        tvNivelAtual.text = "Nível: $nomeNivel"
    }

    private fun carregarNovaPalavra(
        tvPalavra: TextView,
        tvFeedback: TextView,
        btnOpcao1: Button,
        btnOpcao2: Button,
        btnOpcao3: Button,
        btnOpcao4: Button
    ) {
        // Sorteia entre os exercícios que ainda não apareceram (10 exercícios diferentes)
        val banco = when (nivelAtual) {
            1 -> silabas.distinct()
            2 -> palavras
            3 -> frases
            else -> listOf("A")
        }
        val disponiveis = banco.filter { it !in usadas }.ifEmpty {
            usadas.clear()
            banco
        }
        palavraAtual = disponiveis[Random.nextInt(disponiveis.size)]
        usadas.add(palavraAtual)
        respondido = false

        tvPalavra.text = palavraAtual
        tvFeedback.text = ""
        ajustarTexto(tvPalavra, palavraAtual, resources.getDimension(R.dimen.ts_72), 16f, maxLinhas = 4, usarAltura = true)

        val opcoes = gerarOpcoes(palavraAtual)
        val opcoesEmbaralhadas = opcoes.shuffled()

        btnOpcao1.text = opcoesEmbaralhadas[0]
        btnOpcao2.text = opcoesEmbaralhadas[1]
        btnOpcao3.text = opcoesEmbaralhadas[2]
        btnOpcao4.text = opcoesEmbaralhadas[3]

        listOf(btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4).forEach {
            ajustarTexto(it, it.text.toString(), resources.getDimension(R.dimen.ts_17), 11f, maxLinhas = 3, usarAltura = false)
        }

        listOf(btnOpcao1, btnOpcao2, btnOpcao3, btnOpcao4).forEach {
            it.setBackgroundColor(Color.parseColor("#4CAF50"))
        }
    }

    /**
     * Reduz o tamanho da fonte até que NENHUMA palavra seja cortada no meio:
     * a maior palavra precisa caber inteira na largura disponível (a quebra de linha
     * acontece só nos espaços). Assim "CONSTRUÇÃO" nunca vira "CONSTRUÇÃ" + "O".
     */
    private fun ajustarTexto(
        tv: TextView,
        texto: String,
        maxPx: Float,
        minSp: Float,
        maxLinhas: Int,
        usarAltura: Boolean
    ) {
        tv.post {
            val largura = tv.width - tv.paddingLeft - tv.paddingRight
            if (largura <= 0) return@post
            val altura = tv.height - tv.paddingTop - tv.paddingBottom
            val umSp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
            val minPx = minSp * umSp
            val paint = TextPaint(tv.paint)
            val palavrasTexto = texto.split(" ").filter { it.isNotEmpty() }
            var tamanho = maxPx
            while (tamanho > minPx) {
                paint.textSize = tamanho
                val maior = palavrasTexto.maxOfOrNull { paint.measureText(it) } ?: 0f
                if (maior <= largura) {
                    val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, paint, largura).build()
                    val cabeAltura = !usarAltura || altura <= 0 || layout.height <= altura
                    if (layout.lineCount <= maxLinhas && cabeAltura) break
                }
                tamanho -= umSp
            }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho.coerceAtLeast(minPx))
        }
    }

    private fun gerarOpcoes(palavraCorreta: String): List<String> {
        val opcoes = mutableSetOf<String>()
        opcoes.add(palavraCorreta)

        when (nivelAtual) {
            1 -> {
                while (opcoes.size < 4) {
                    val letra = silabas[Random.nextInt(silabas.size)].toString()
                    if (letra != palavraCorreta) {
                        opcoes.add(letra)
                    }
                }
            }
            2 -> {
                while (opcoes.size < 4) {
                    val palavra = palavras[Random.nextInt(palavras.size)]
                    if (palavra != palavraCorreta) {
                        opcoes.add(palavra)
                    }
                }
            }
            3 -> {
                while (opcoes.size < 4) {
                    val palavra = frases[Random.nextInt(frases.size)]
                    if (palavra != palavraCorreta) {
                        opcoes.add(palavra)
                    }
                }
            }
        }

        return opcoes.toList()
    }

    private fun atualizarProgresso(tvProgresso: TextView) {
        tvProgresso.text = "Progresso: $totalTentativas / $maxTentativas | $acertos acertos"
    }

    private fun irParaParabens() {
        if (parabensAberto) return // garante que a tela de parabéns abre uma única vez
        parabensAberto = true
        Notificacoes.registrarPratica(this) // hoje já praticou: o lembrete diário não aparece
        handler.removeCallbacksAndMessages(null)
        val intent = Intent(this, ParabensActivity::class.java)
        intent.putExtra(ParabensActivity.EXTRA_ACERTOS, acertos)
        intent.putExtra(ParabensActivity.EXTRA_TOTAL, totalTentativas)
        startActivity(intent)
        finish()
    }
}