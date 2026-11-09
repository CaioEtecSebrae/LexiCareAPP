package br.gov.sp.etec.lexicare

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import br.gov.sp.etec.lexicare.model.Tarefa
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Tudo o que é compartilhado entre o Calendário (criar tarefa) e os Detalhes (editar tarefa):
 * leitura/gravação das tarefas DO USUÁRIO LOGADO, o formulário de tarefa e o lembrete.
 */
object TarefaHelper {

    private const val CHAVE = "tarefas"

    // ---------- armazenamento (por usuário) ----------

    fun carregar(ctx: Context): MutableList<Tarefa> {
        return try {
            val json = SessaoUsuario.ler(ctx, CHAVE).orEmpty()
            if (json.isEmpty() || json == "[]") {
                mutableListOf()
            } else {
                val type = object : TypeToken<List<Tarefa>>() {}.type
                val lista: List<Tarefa>? = Gson().fromJson(json, type)
                lista?.toMutableList() ?: mutableListOf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun salvar(ctx: Context, tarefas: List<Tarefa>) {
        try {
            SessaoUsuario.gravar(ctx, CHAVE, Gson().toJson(tarefas))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun existeLista(ctx: Context): Boolean = SessaoUsuario.contem(ctx, CHAVE)

    // ---------- formulário (criar / editar) ----------

    private val nomesCores = mapOf(
        "#F44336" to "Vermelho",
        "#2196F3" to "Azul",
        "#4CAF50" to "Verde",
        "#FF9800" to "Laranja",
        "#9C27B0" to "Roxo"
    )

    /**
     * Abre o formulário de tarefa.
     * @param tarefaExistente null = criar uma tarefa nova; preenchida = editar essa tarefa.
     * @param dataPadrao data (dd/MM/aaaa) sugerida ao criar.
     * @param aoSalvar recebe a tarefa pronta (nova ou já com as alterações).
     */
    fun mostrarFormulario(
        activity: Activity,
        tarefaExistente: Tarefa?,
        dataPadrao: String,
        aoSalvar: (Tarefa) -> Unit
    ) {
        try {
            val editando = tarefaExistente != null
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_adicionar_tarefa, null)
            val dialog = AlertDialog.Builder(activity).setView(dialogView).create()

            val tvTituloDialogo = dialogView.findViewById<TextView>(R.id.tvTituloDialogo)
            val etTitulo = dialogView.findViewById<EditText>(R.id.etTituloTarefa)
            val etDescricao = dialogView.findViewById<EditText>(R.id.etDescricaoTarefa)
            val etDetalhamento = dialogView.findViewById<EditText>(R.id.etDetalhamentoTarefa)
            val etData = dialogView.findViewById<EditText>(R.id.etDataTarefa)
            val etHorario = dialogView.findViewById<EditText>(R.id.etHorarioTarefa)
            val rgDificuldade = dialogView.findViewById<RadioGroup>(R.id.rgDificuldade)
            val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
            val btnSalvar = dialogView.findViewById<Button>(R.id.btnSalvarTarefa)
            val tvCorSelecionada = dialogView.findViewById<TextView>(R.id.tvCorSelecionada)

            var corSelecionada = "#4CAF50"
            var dificuldadeSelecionada = "Médio"

            if (tarefaExistente != null) {
                tvTituloDialogo.text = "Editar tarefa"
                btnSalvar.text = "Salvar"
                etTitulo.setText(tarefaExistente.titulo)
                etDescricao.setText(
                    if (tarefaExistente.descricao == "Sem descrição") "" else tarefaExistente.descricao
                )
                etDetalhamento.setText(tarefaExistente.detalhamento)
                etData.setText(tarefaExistente.data)
                etHorario.setText(tarefaExistente.horario)
                dificuldadeSelecionada = tarefaExistente.dificuldade.ifEmpty { "Médio" }
                rgDificuldade.check(
                    when (dificuldadeSelecionada) {
                        "Fácil" -> R.id.rbFacil
                        "Difícil" -> R.id.rbDificil
                        else -> R.id.rbMedio
                    }
                )
                corSelecionada = tarefaExistente.cor.ifEmpty { corSelecionada }
                tvCorSelecionada.text = "Cor selecionada: ${nomesCores[corSelecionada.uppercase()] ?: "Personalizada"}"
            } else {
                etData.setText(dataPadrao)
            }

            etData.isFocusable = false
            etData.isClickable = true
            etData.isCursorVisible = false
            etData.setOnClickListener { abrirDatePicker(activity, etData) }

            etHorario.isFocusable = false
            etHorario.isClickable = true
            etHorario.isCursorVisible = false
            etHorario.setOnClickListener { abrirTimePicker(activity, etHorario) }

            rgDificuldade.setOnCheckedChangeListener { _, checkedId ->
                dificuldadeSelecionada = when (checkedId) {
                    R.id.rbFacil -> "Fácil"
                    R.id.rbMedio -> "Médio"
                    R.id.rbDificil -> "Difícil"
                    else -> "Médio"
                }
            }

            fun escolherCor(hex: String) {
                corSelecionada = hex
                tvCorSelecionada.text = "Cor selecionada: ${nomesCores[hex] ?: ""}"
            }
            dialogView.findViewById<View>(R.id.colorRed).setOnClickListener { escolherCor("#F44336") }
            dialogView.findViewById<View>(R.id.colorBlue).setOnClickListener { escolherCor("#2196F3") }
            dialogView.findViewById<View>(R.id.colorGreen).setOnClickListener { escolherCor("#4CAF50") }
            dialogView.findViewById<View>(R.id.colorOrange).setOnClickListener { escolherCor("#FF9800") }
            dialogView.findViewById<View>(R.id.colorPurple).setOnClickListener { escolherCor("#9C27B0") }

            btnCancelar.setOnClickListener { dialog.dismiss() }

            btnSalvar.setOnClickListener {
                val tituloDigitado = etTitulo.text.toString().trim()
                val descricaoDigitada = etDescricao.text.toString().trim()
                val detalhamentoDigitado = etDetalhamento.text.toString().trim()
                val dataEscolhida = etData.text.toString().trim()
                val horarioEscolhido = etHorario.text.toString().trim()

                if (tituloDigitado.isEmpty()) {
                    Toast.makeText(activity, "Digite um título!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (dataEscolhida.isEmpty()) {
                    Toast.makeText(activity, "Selecione uma data!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!validarData(dataEscolhida)) {
                    Toast.makeText(activity, "Data inválida! Use DD/MM/AAAA", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (horarioEscolhido.isNotEmpty() && !validarHorario(horarioEscolhido)) {
                    Toast.makeText(activity, "Horário inválido! Use HH:MM", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Cancela o lembrete antigo antes de alterar data/horário da tarefa.
                if (editando && tarefaExistente != null) cancelarNotificacao(activity, tarefaExistente)
                val tarefa = tarefaExistente?.copy() ?: Tarefa(id = System.currentTimeMillis().toInt())
                tarefa.titulo = tituloDigitado
                tarefa.descricao = descricaoDigitada.ifEmpty { "Sem descrição" }
                tarefa.data = dataEscolhida
                tarefa.horario = horarioEscolhido.ifEmpty { "00:00" }
                tarefa.cor = corSelecionada
                tarefa.dificuldade = dificuldadeSelecionada
                tarefa.detalhamento = detalhamentoDigitado

                // Agenda novamente com os dados atualizados.
                agendarNotificacao(activity, tarefa)

                aoSalvar(tarefa)
                dialog.dismiss()
            }

            dialog.show()
            val larguraTela = activity.resources.displayMetrics.widthPixels
            val densidade = activity.resources.displayMetrics.density
            val larguraMax = (900 * densidade).toInt()
            dialog.window?.setLayout(
                minOf((larguraTela * 0.85).toInt(), larguraMax)
                    .coerceAtLeast(minOf(larguraTela, (320 * densidade).toInt())),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirDatePicker(activity: Activity, etData: EditText) {
        val calendario = Calendar.getInstance()
        try {
            val atual = etData.text.toString()
            if (atual.isNotEmpty()) {
                val parseada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(atual)
                if (parseada != null) calendario.time = parseada
            }
        } catch (e: ParseException) { }

        DatePickerDialog(
            activity,
            { _, ano, mes, dia ->
                etData.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes + 1, ano))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun abrirTimePicker(activity: Activity, etHorario: EditText) {
        var hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        var minuto = Calendar.getInstance().get(Calendar.MINUTE)
        val atual = etHorario.text.toString()
        if (validarHorario(atual)) {
            val p = atual.split(":")
            hora = p[0].toInt()
            minuto = p[1].toInt()
        }
        TimePickerDialog(
            activity,
            { _, h, m -> etHorario.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)) },
            hora, minuto, true
        ).show()
    }

    // ---------- lembrete ----------

    private fun intentLembrete(ctx: Context, tarefa: Tarefa): PendingIntent {
        val intent = Intent(ctx, LembreteReceiver::class.java)
        intent.putExtra("titulo", tarefa.titulo)
        intent.putExtra("id", tarefa.id)
        intent.putExtra("horario", tarefa.horario)
        intent.putExtra("descricao", tarefa.descricao)
        intent.putExtra("dificuldade", tarefa.dificuldade)
        intent.putExtra("cor", tarefa.cor)
        return PendingIntent.getBroadcast(
            ctx, tarefa.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelarNotificacao(ctx: Context, tarefa: Tarefa) {
        try {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(intentLembrete(ctx, tarefa))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun agendarNotificacao(activity: Activity, tarefa: Tarefa) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dataHora = sdf.parse("${tarefa.data} ${tarefa.horario}") ?: return

            // Se a data já passou, não agenda
            if (dataHora.time < System.currentTimeMillis()) return

            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Alarmes exatos precisam de permissão no Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = android.net.Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
                Toast.makeText(activity, "Ative os alarmes exatos para receber notificações", Toast.LENGTH_LONG).show()
                return
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dataHora.time, intentLembrete(activity, tarefa))
            Toast.makeText(activity, "Notificação agendada para ${tarefa.horario}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Erro ao agendar notificação", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- validação ----------

    private fun validarData(data: String): Boolean = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }.parse(data)
        true
    } catch (e: ParseException) {
        false
    }

    private fun validarHorario(horario: String): Boolean = try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply { isLenient = false }.parse(horario)
        true
    } catch (e: ParseException) {
        false
    }
}
