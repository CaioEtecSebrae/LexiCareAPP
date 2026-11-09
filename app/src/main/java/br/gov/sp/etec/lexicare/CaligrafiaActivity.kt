package br.gov.sp.etec.lexicare

import android.content.Intent // NOVO
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.gov.sp.etec.lexicare.views.CanvasCaligrafia

class CaligrafiaActivity : BaseActivity() {

    private val alfabeto = listOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z'
    )

    private var letrasPraticadas = mutableListOf<Char>()
    private var letraAtual = 'A'
    private var atividadeConcluida = false // NOVO: Evita que o botão "Próxima" seja clicado após a conclusão

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("letra", letraAtual.toString())
        outState.putString("praticadas", letrasPraticadas.joinToString(""))
        outState.putBoolean("concluida", atividadeConcluida) // NOVO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caligrafia)

        val canvas = findViewById<CanvasCaligrafia>(R.id.canvasCaligrafia)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val btnVerificar = findViewById<Button>(R.id.btnVerificar)
        val btnProxima = findViewById<Button>(R.id.btnProxima)
        val tvLetraAtual = findViewById<TextView>(R.id.tvLetraAtual)
        val tvProgresso = findViewById<TextView>(R.id.tvProgresso)
        val tvFeedback = findViewById<TextView>(R.id.tvFeedback)
        val btnLimpar = findViewById<Button>(R.id.btnLimpar)

        if (savedInstanceState != null) {
            letrasPraticadas = (savedInstanceState.getString("praticadas") ?: "").toMutableList()
            letraAtual = savedInstanceState.getString("letra")?.firstOrNull() ?: 'A'
            atividadeConcluida = savedInstanceState.getBoolean("concluida", false) // NOVO
        } else {
            selecionarProximaLetra()
        }

        canvas.setLetra(letraAtual)
        tvLetraAtual.text = letraAtual.toString()
        atualizarProgresso(tvProgresso)

        btnVoltar.setOnClickListener { finish() }

        btnVerificar.setOnClickListener {
            val pontos = canvas.verificarPontuacao()
            tvFeedback.text = "Pontuação: $pontos/100\n${canvas.getDica()}"
            val cor = when {
                pontos >= 70 -> "#4CAF50"
                pontos >= 40 -> "#FF9800"
                else -> "#F44336"
            }
            tvFeedback.setTextColor(android.graphics.Color.parseColor(cor))
            // Volta ao topo para o aluno ver a pontuação e o comentário
            findViewById<ScrollView>(R.id.scrollCaligrafia)?.post { findViewById<ScrollView>(R.id.scrollCaligrafia)?.smoothScrollTo(0, 0) }

            Notificacoes.registrarPratica(this) // hoje já praticou: o lembrete diário não aparece

            if (!letrasPraticadas.contains(letraAtual)) {
                letrasPraticadas.add(letraAtual)
                atualizarProgresso(tvProgresso)

                // NOVO: Verifica se terminou todas as letras após adicionar a atual
                if (letrasPraticadas.size >= alfabeto.size) {
                    atividadeConcluida = true
                }
            }
        }

        // Limpa o desenho e reinicia a pontuação da letra para uma nova tentativa
        btnLimpar.setOnClickListener {
            canvas.limpar()
            tvFeedback.text = ""
            if (!atividadeConcluida && letrasPraticadas.remove(letraAtual)) {
                atualizarProgresso(tvProgresso)
            }
        }

        btnProxima.setOnClickListener {
            // NOVO: Se já concluiu, não faz nada
            if (atividadeConcluida) return@setOnClickListener

            selecionarProximaLetra()
            canvas.setLetra(letraAtual)
            tvLetraAtual.text = letraAtual.toString()
            tvFeedback.text = "" // Limpa o feedback ao mudar de letra
        }
    }
    private fun selecionarProximaLetra() {
        // Removemos a verificação que limpava a lista aqui, pois agora a tela fecha
        val letrasDisponiveis = alfabeto.filter { !letrasPraticadas.contains(it) }
        letraAtual = if (letrasDisponiveis.isNotEmpty()) {
            letrasDisponiveis.random()
        } else {
            alfabeto.random()
        }
    }

    private fun atualizarProgresso(tvProgresso: TextView) {
        tvProgresso.text = "Letras praticadas: ${letrasPraticadas.size} / ${alfabeto.size}"

        if (letrasPraticadas.size >= alfabeto.size) {
            tvProgresso.text = "Todas as letras praticadas! Parabéns!"
        }
    }
}