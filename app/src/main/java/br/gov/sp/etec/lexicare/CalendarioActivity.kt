package br.gov.sp.etec.lexicare

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.gov.sp.etec.lexicare.adapters.TarefaAdapter
import br.gov.sp.etec.lexicare.model.Tarefa
import com.google.gson.Gson
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CalendarioActivity : BaseActivity() {

    private lateinit var adapter: TarefaAdapter
    private val tarefas = mutableListOf<Tarefa>()
    private var dataSelecionada = ""
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario)

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        calendarView = findViewById(R.id.calendarView)
        val rvTarefas = findViewById<RecyclerView>(R.id.rvTarefas)
        val btnAdicionarTarefa = findViewById<Button>(R.id.btnAdicionarTarefa)

        adapter = TarefaAdapter(
            emptyList(),
            onItemClick = { tarefa ->
                TarefaHelper.mostrarFormulario(this, tarefa, tarefa.data) { tarefaEditada ->
                    val indice = tarefas.indexOfFirst { it.id == tarefa.id }
                    if (indice >= 0) tarefas[indice] = tarefaEditada
                    salvarTarefas()
                    dataSelecionada = tarefaEditada.data
                    val partes = dataSelecionada.split("/")
                    if (partes.size == 3) {
                        val cal = Calendar.getInstance()
                        cal.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt())
                        calendarView.date = cal.timeInMillis
                    }
                    carregarTarefasPorData(dataSelecionada)
                    Toast.makeText(this, "Tarefa atualizada!", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { tarefa ->
                mostrarDialogDeletarTarefa(tarefa)
            }
        )
        rvTarefas.layoutManager = LinearLayoutManager(this)
        rvTarefas.adapter = adapter

        carregarTarefas()

        val dataAtual = Calendar.getInstance()
        dataSelecionada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataAtual.time)
        carregarTarefasPorData(dataSelecionada)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val data = Calendar.getInstance()
            data.set(year, month, dayOfMonth)
            dataSelecionada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data.time)
            carregarTarefasPorData(dataSelecionada)
        }

        btnVoltar.setOnClickListener {
            finish()
        }

        btnAdicionarTarefa.setOnClickListener {
            mostrarDialogAdicionarTarefa()
        }

        // PEDIR PERMISSÃO DE NOTIFICAÇÃO (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun salvarTarefas() {
        TarefaHelper.salvar(this, tarefas)
    }

    // Lê só as tarefas do usuário logado
    private fun carregarTarefas() {
        tarefas.clear()
        tarefas.addAll(TarefaHelper.carregar(this))
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized && dataSelecionada.isNotEmpty()) {
            carregarTarefas()
            carregarTarefasPorData(dataSelecionada)
        }
    }

    private fun carregarTarefasPorData(data: String) {
        try {
            val tarefasFiltradas = tarefas.filter { it.data == data }
            adapter.atualizarLista(tarefasFiltradas)

            val tvTarefasDoDia = findViewById<TextView>(R.id.tvTarefasDoDia)
            tvTarefasDoDia.text = "Tarefas do dia ${data}: ${tarefasFiltradas.size}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mostrarDialogDeletarTarefa(tarefa: Tarefa) {
        AlertDialog.Builder(this)
            .setTitle("Deletar tarefa")
            .setMessage("Tem certeza que deseja deletar a tarefa \"${tarefa.titulo}\"?")
            .setPositiveButton("Sim, deletar") { _, _ ->
                TarefaHelper.cancelarNotificacao(this, tarefa)
                tarefas.removeAll { it.id == tarefa.id }
                salvarTarefas()
                carregarTarefasPorData(dataSelecionada)
                Toast.makeText(this, "Tarefa deletada!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogAdicionarTarefa() {
        TarefaHelper.mostrarFormulario(this, null, dataSelecionada) { novaTarefa ->
            tarefas.add(novaTarefa)
            salvarTarefas()

            if (novaTarefa.data != dataSelecionada) {
                dataSelecionada = novaTarefa.data
                val partes = novaTarefa.data.split("/")
                if (partes.size == 3) {
                    val cal = Calendar.getInstance()
                    cal.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt())
                    calendarView.date = cal.timeInMillis
                }
            }

            carregarTarefasPorData(dataSelecionada)
            Toast.makeText(this, "Tarefa adicionada!", Toast.LENGTH_SHORT).show()
        }
    }
}
