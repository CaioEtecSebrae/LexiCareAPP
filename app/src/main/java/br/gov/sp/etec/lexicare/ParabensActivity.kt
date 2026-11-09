package br.gov.sp.etec.lexicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class ParabensActivity : BaseActivity() {

    companion object {
        const val EXTRA_ACERTOS = "acertos"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_ACTIVITY_EXERCICIO = "activity_exercicio"
        const val EXTRA_EXTRAS_EXERCICIO = "extras_exercicio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parabens)

        val acertos = intent.getIntExtra(EXTRA_ACERTOS, 0)
        val total = intent.getIntExtra(EXTRA_TOTAL, 10)

        findViewById<TextView>(R.id.tvAcertos).text = acertos.toString()
        findViewById<TextView>(R.id.tvTotal).text = total.toString()

        val tvMensagem = findViewById<TextView>(R.id.tvMensagem)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloResultado)
        val tvEmoji = findViewById<TextView>(R.id.tvEmojiResultado)
        val percentual = if (total > 0) acertos.toFloat() / total else 0f
        when {
            percentual >= 0.8f -> {
                tvTitulo.text = "Parabéns!"
                tvEmoji.text = "🎉"
                tvMensagem.text = if (percentual >= 0.9f)
                    "Excelente resultado! Você está mandando muito bem!"
                else
                    "Muito bem! Continue praticando para avançar ainda mais."
            }
            percentual >= 0.5f -> {
                tvTitulo.text = "Bom trabalho!"
                tvEmoji.text = "⭐"
                tvMensagem.text = "Você está progredindo. Continue praticando e tente melhorar sua pontuação!"
            }
            else -> {
                tvTitulo.text = "Tente novamente"
                tvEmoji.text = "💪"
                tvMensagem.text = "Tudo bem errar! Revise as questões e tente mais uma vez. Você consegue aprender com a prática."
            }
        }

        // ✅ BOTÃO REFAZER EXERCÍCIO
        val btnRefazer = findViewById<Button>(R.id.btnRefazerExercicio)
        val nomeActivityExercicio = intent.getStringExtra(EXTRA_ACTIVITY_EXERCICIO)

        if (nomeActivityExercicio.isNullOrEmpty()) {
            btnRefazer.visibility = View.GONE
        } else {
            btnRefazer.setOnClickListener {
                try {
                    val classeExercicio = Class.forName(nomeActivityExercicio)
                    val intentExercicio = Intent(this, classeExercicio)

                    @Suppress("DEPRECATION")
                    intent.getBundleExtra(EXTRA_EXTRAS_EXERCICIO)?.let { bundle ->
                        intentExercicio.putExtras(bundle)
                    }

                    intentExercicio.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intentExercicio)
                    finish()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this,
                        "Não foi possível refazer o exercício.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // BOTÃO VOLTAR AO INÍCIO (Dashboard)
        findViewById<Button>(R.id.btnVoltarDashboard).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}