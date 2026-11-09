package br.gov.sp.etec.lexicare

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.view.View
import android.widget.ImageView
import java.io.File
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import br.gov.sp.etec.lexicare.model.Tarefa
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : BaseActivity() {

    override val statusBarClara: Boolean = false

    private lateinit var tvTarefasHoje: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvSaudacao: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvTarefasHoje = findViewById(R.id.tvTarefasHoje)
        tvSaudacao = findViewById(R.id.tvSaudacao)
        sharedPreferences = getSharedPreferences("LexiCarePrefs", MODE_PRIVATE)
        val btnConfiguracoes = findViewById<ImageView>(R.id.btnConfiguracoes)

        // CORREÇÃO: Só cria as tarefas de exemplo se a chave "tarefas" NUNCA foi criada
        criarTarefasExemploSeNecessario()

        atualizarContadorTarefas()
        configurarSaudacao()

        btnConfiguracoes.setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }
        atualizarFotoPerfil()

        // Lembrete diário dos exercícios (uma única notificação por dia)
        Notificacoes.criarCanais(this)
        Notificacoes.pedirPermissaoSeNecessario(this)
        Notificacoes.agendarLembreteDiario(this)

        findViewById<Button>(R.id.btnCalendario).setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
        }

        findViewById<Button>(R.id.btnExercicios).setOnClickListener {
            startActivity(Intent(this, ExerciciosActivity::class.java))
        }

        findViewById<Button>(R.id.btnCaligrafia).setOnClickListener {
            startActivity(Intent(this, CaligrafiaActivity::class.java))
        }

        findViewById<Button>(R.id.btnAnotacoes).setOnClickListener {
            startActivity(Intent(this, AnotacoesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarContadorTarefas()
        configurarSaudacao()
        atualizarFotoPerfil()
    }

    private fun atualizarFotoPerfil() {
        val emailSeguro = SessaoUsuario.emailParaArquivo(this)
        val arquivo = File(File(filesDir, "fotos"), "perfil_${emailSeguro}.jpg")
        val imagem = findViewById<ImageView>(R.id.btnConfiguracoes)

        // Recorte redondo garantido: o contorno da view é um círculo, então a foto
        // (e o anel de foco) nunca aparece quadrada, em nenhum tamanho de tela.
        imagem.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        imagem.clipToOutline = true

        val bitmap = if (arquivo.exists()) BitmapFactory.decodeFile(arquivo.path) else null
        if (bitmap != null) {
            // Corta um quadrado central, para o círculo nunca ficar achatado.
            val lado = minOf(bitmap.width, bitmap.height)
            val quadrado = Bitmap.createBitmap(
                bitmap, (bitmap.width - lado) / 2, (bitmap.height - lado) / 2, lado, lado
            )
            imagem.setImageBitmap(quadrado)
            imagem.scaleType = ImageView.ScaleType.CENTER_CROP
            imagem.setPadding(0, 0, 0, 0)
        } else {
            val densidade = resources.displayMetrics.density
            val pad = (8 * densidade).toInt()
            imagem.setImageResource(android.R.drawable.ic_menu_myplaces)
            imagem.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imagem.setPadding(pad, pad, pad, pad)
        }
    }

    private fun configurarSaudacao() {
        val nomeCompleto = sharedPreferences.getString("perfil_nome", "Usuário") ?: "Usuário"
        val primeiroNome = nomeCompleto.trim().split(" ").firstOrNull()?.ifEmpty { "Usuário" } ?: "Usuário"
        tvSaudacao.text = "Olá, $primeiroNome"
    }


    // CORREÇÃO AQUI: Evita sobrescrever as tarefas do usuário
    private fun criarTarefasExemploSeNecessario() {
        // As tarefas de exemplo são criadas uma vez para CADA usuário (chave própria do e-mail)
        if (!TarefaHelper.existeLista(this)) {
            val dataAtual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val tarefas = listOf(
                Tarefa(
                    id = System.currentTimeMillis().toInt(), // ID único
                    titulo = "Estudar Capítulo 3",
                    descricao = "Ler o capítulo 3 do livro de Português",
                    data = dataAtual,
                    horario = "14:00",
                    cor = "#2196F3",
                    dificuldade = "Médio",
                    detalhamento = "Ler as páginas 45 a 60."
                ),
                Tarefa(
                    id = System.currentTimeMillis().toInt() + 1, // ID único
                    titulo = "Exercícios de Caligrafia",
                    descricao = "Fazer os exercícios da página 15",
                    data = dataAtual,
                    horario = "16:30",
                    cor = "#4CAF50",
                    dificuldade = "Fácil",
                    detalhamento = "Praticar a escrita das letras."
                )
            )

            TarefaHelper.salvar(this, tarefas)
        }
    }

    private fun atualizarContadorTarefas() {
        val tarefas = carregarTarefas()
        val dataAtual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val tarefasHoje = tarefas.filter { it.data == dataAtual }

        tvTarefasHoje.text = "Tarefas de hoje: ${tarefasHoje.size}"
    }

    private fun carregarTarefas(): List<Tarefa> = TarefaHelper.carregar(this)
}
