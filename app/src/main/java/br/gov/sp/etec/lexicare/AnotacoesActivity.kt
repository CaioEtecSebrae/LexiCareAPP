package br.gov.sp.etec.lexicare

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.gov.sp.etec.lexicare.adapters.AnotacaoAdapter
import br.gov.sp.etec.lexicare.model.Anotacao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class AnotacoesActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var adapter: AnotacaoAdapter
    private val anotacoes = mutableListOf<Anotacao>()
    private var contadorId = 0
    private var anotacaoEmEdicaoId: Int? = null
    private val REQUEST_CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anotacoes)

        tts = TextToSpeech(this, this)

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val etTitulo = findViewById<EditText>(R.id.etTitulo)
        val etConteudo = findViewById<EditText>(R.id.etConteudo)
        val btnOuvir = findViewById<Button>(R.id.btnOuvir)
        val btnCorrigir = findViewById<Button>(R.id.btnCorrigir)
        val btnSalvar = findViewById<Button>(R.id.btnSalvar)
        val btnMicrofone = findViewById<Button>(R.id.btnMicrofone)
        val rvAnotacoes = findViewById<RecyclerView>(R.id.rvAnotacoes)

        // Configuração do Adapter com as ações
        adapter = AnotacaoAdapter(
            anotacoes,
            onItemClick = { anotacao ->
                anotacaoEmEdicaoId = anotacao.id
                etTitulo.setText(anotacao.titulo)
                etConteudo.setText(anotacao.conteudo)
                btnSalvar.text = "Salvar alterações"
                Toast.makeText(this, "Editando: ${anotacao.titulo}", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { anotacao ->
                mostrarDialogDeletarAnotacao(anotacao)
            },
            onDeleteClick = { anotacao ->
                mostrarDialogDeletarAnotacao(anotacao)
            }
        )

        rvAnotacoes.layoutManager = LinearLayoutManager(this)
        rvAnotacoes.adapter = adapter

        // Carrega as anotações já salvas
        carregarAnotacoes()

        btnVoltar.setOnClickListener { finish() }

        btnMicrofone.setOnClickListener {
            iniciarReconhecimentoVoz()
        }

        btnOuvir.setOnClickListener {
            val texto = etConteudo.text.toString()
            if (texto.isNotEmpty()) {
                tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "Escreva algo para ouvir!", Toast.LENGTH_SHORT).show()
            }
        }

        btnCorrigir.setOnClickListener {
            val texto = etConteudo.text.toString()
            if (texto.isBlank()) {
                Toast.makeText(this, "Escreva algo para corrigir!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Dicionário/corretor de português (API): corrige, tira e adiciona palavras
            val rotulo = btnCorrigir.text
            btnCorrigir.isEnabled = false
            btnCorrigir.text = "Corrigindo…"

            CorretorTexto.corrigir(texto) { resultado ->
                if (isFinishing || isDestroyed) return@corrigir
                btnCorrigir.isEnabled = true
                btnCorrigir.text = rotulo

                // Se o aluno mexeu no texto enquanto corrigia, não sobrescreve o que ele escreveu.
                if (etConteudo.text.toString() != texto) {
                    Toast.makeText(this, "O texto mudou. Toque em Corrigir de novo.", Toast.LENGTH_SHORT).show()
                    return@corrigir
                }

                if (resultado.correcoes > 0) {
                    etConteudo.setText(resultado.texto)
                    etConteudo.setSelection(etConteudo.text.length)
                }

                val mensagem = when {
                    resultado.offline && resultado.correcoes > 0 ->
                        "Sem internet: usei a correção básica (${resultado.correcoes} correções)"
                    resultado.offline ->
                        "Sem internet: só a correção básica ficou disponível"
                    resultado.correcoes == 1 -> "Texto corrigido! (1 correção)"
                    resultado.correcoes > 1 -> "Texto corrigido! (${resultado.correcoes} correções)"
                    else -> "Texto já está correto!"
                }
                Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
            }
        }

        btnSalvar.setOnClickListener {
            val titulo = etTitulo.text.toString()
            val conteudo = etConteudo.text.toString()

            if (titulo.isNotEmpty() && conteudo.isNotEmpty()) {
                val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                val idEditado = anotacaoEmEdicaoId
                if (idEditado != null) {
                    val indice = anotacoes.indexOfFirst { it.id == idEditado }
                    if (indice >= 0) {
                        anotacoes[indice] = anotacoes[indice].copy(
                            titulo = titulo,
                            conteudo = conteudo,
                            data = dataAtual
                        )
                    }
                    anotacaoEmEdicaoId = null
                    btnSalvar.text = "Salvar anotação"
                    Toast.makeText(this, "Alterações salvas!", Toast.LENGTH_SHORT).show()
                } else {
                    anotacoes.add(0, Anotacao(
                        id = contadorId++, titulo = titulo, conteudo = conteudo, data = dataAtual
                    ))
                    Toast.makeText(this, "Anotação salva!", Toast.LENGTH_SHORT).show()
                }
                adapter.atualizarLista(anotacoes)
                salvarAnotacoes()
                etTitulo.text.clear()
                etConteudo.text.clear()
            } else {
                Toast.makeText(this, "Preencha título e conteúdo!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Diálogo unificado para deletar anotação (chamado pelo botão 🗑️ e pelo clique longo)
    private fun mostrarDialogDeletarAnotacao(anotacao: Anotacao) {
        AlertDialog.Builder(this)
            .setTitle("Deletar Anotação")
            .setMessage("Tem certeza que deseja deletar '${anotacao.titulo}'?")
            .setPositiveButton("Sim") { _, _ ->
                anotacoes.remove(anotacao)
                if (anotacaoEmEdicaoId == anotacao.id) {
                    anotacaoEmEdicaoId = null
                    findViewById<EditText>(R.id.etTitulo).text.clear()
                    findViewById<EditText>(R.id.etConteudo).text.clear()
                    findViewById<Button>(R.id.btnSalvar).text = "Salvar anotação"
                }
                adapter.atualizarLista(anotacoes)
                salvarAnotacoes()
                Toast.makeText(this, "Anotação deletada!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun iniciarReconhecimentoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua anotação...")
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Reconhecimento de voz não disponível!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val resultados = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!resultados.isNullOrEmpty()) {
                val textoFalado = resultados[0]
                val etConteudo = findViewById<EditText>(R.id.etConteudo)

                if (etConteudo.text.isNotEmpty()) {
                    etConteudo.append(" ")
                }
                etConteudo.append(textoFalado)

                Toast.makeText(this, "Texto transcrito!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarAnotacoes() {
        // Cada e-mail logado tem a sua própria lista de anotações
        val json = SessaoUsuario.ler(this, "anotacoes")

        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Anotacao>>() {}.type
            val lista = Gson().fromJson<List<Anotacao>>(json, type)
            anotacoes.clear()
            anotacoes.addAll(lista)

            if (anotacoes.isNotEmpty()) {
                contadorId = (anotacoes.maxByOrNull { it.id }?.id ?: 0) + 1
            } else {
                contadorId = 1
            }
        } else {
            adicionarAnotacaoExemplo()
        }
        adapter.atualizarLista(anotacoes)
    }

    private fun salvarAnotacoes() {
        SessaoUsuario.gravar(this, "anotacoes", Gson().toJson(anotacoes))
    }

    private fun adicionarAnotacaoExemplo() {
        val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        anotacoes.add(
            Anotacao(
                id = contadorId++,
                titulo = "Bem-vindo ao LexiCare!",
                conteudo = "Esta é uma anotação de exemplo. Você pode criar, editar e deletar suas próprias anotações. Experimente o botão do microfone para ditar suas anotações! 🎤",
                data = dataAtual
            )
        )

        salvarAnotacoes()
        adapter.atualizarLista(anotacoes)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Idioma não suportado!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS não disponível!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        super.onDestroy()
    }
}