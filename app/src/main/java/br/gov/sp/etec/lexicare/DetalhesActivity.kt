package br.gov.sp.etec.lexicare

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import br.gov.sp.etec.lexicare.model.Tarefa

class DetalhesActivity : BaseActivity() {

    private lateinit var tarefa: Tarefa

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        tarefa = intent.getSerializableExtra("tarefa") as? Tarefa
            ?: Tarefa(0, "Erro", "Tarefa não encontrada", "", "", "#4CAF50", "", "")

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val btnDeletar = findViewById<Button>(R.id.btnDeletar)
        val btnEditar = findViewById<Button>(R.id.btnEditar)

        preencherDados()

        btnVoltar.setOnClickListener { finish() }

        // Edita a própria tarefa (mesmo formulário de "Adicionar", já preenchido)
        btnEditar.setOnClickListener {
            TarefaHelper.mostrarFormulario(this, tarefa, tarefa.data) { editada ->
                salvarEdicao(editada)
                tarefa = editada
                preencherDados()
                Toast.makeText(this, "Tarefa atualizada!", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeletar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Deletar Tarefa")
                .setMessage("Tem certeza que deseja deletar '${tarefa.titulo}'?")
                .setPositiveButton("Sim") { _, _ ->
                    deletarTarefa()
                    Toast.makeText(this, "Tarefa deletada!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun deletarTarefa() {
        TarefaHelper.cancelarNotificacao(this, tarefa)
        val tarefas = TarefaHelper.carregar(this)
        tarefas.removeAll { it.id == tarefa.id }
        TarefaHelper.salvar(this, tarefas)
    }

    private fun salvarEdicao(editada: Tarefa) {
        val tarefas = TarefaHelper.carregar(this)
        val indice = tarefas.indexOfFirst { it.id == editada.id }
        if (indice != -1) tarefas[indice] = editada else tarefas.add(editada)
        TarefaHelper.salvar(this, tarefas)
    }

    private fun preencherDados() {
        findViewById<TextView>(R.id.tvTituloDetalhe).text = tarefa.titulo
        findViewById<TextView>(R.id.tvDescricaoDetalhe).text = tarefa.descricao
        findViewById<TextView>(R.id.tvDetalhamentoDetalhe).text =
            tarefa.detalhamento.ifEmpty { "Sem detalhamento adicional" }
        findViewById<TextView>(R.id.tvDataDetalhe).text = tarefa.data
        findViewById<TextView>(R.id.tvHorarioDetalhe).text = tarefa.horario

        val dificuldadeEmoji = when (tarefa.dificuldade) {
            "Fácil" -> "Fácil"
            "Médio" -> "Médio"
            "Difícil" -> "Difícil"
            else -> "⭐ ${tarefa.dificuldade}"
        }
        findViewById<TextView>(R.id.tvDificuldadeDetalhe).text = dificuldadeEmoji

        try {
            findViewById<View>(R.id.viewCorDetalhe).setBackgroundColor(
                android.graphics.Color.parseColor(tarefa.cor)
            )
        } catch (e: Exception) {
            findViewById<View>(R.id.viewCorDetalhe).setBackgroundColor(
                android.graphics.Color.parseColor("#4CAF50")
            )
        }
    }
}