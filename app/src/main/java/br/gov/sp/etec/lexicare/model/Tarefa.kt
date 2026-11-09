package br.gov.sp.etec.lexicare.model

import java.io.Serializable

data class Tarefa(
    val id: Int,
    var titulo: String = "",
    var descricao: String = "",
    var detalhamento: String = "",
    var data: String = "",
    var horario: String = "",
    var cor: String = "#4CAF50",
    var dificuldade: String = "Médio",
    var notificacao: Boolean = false
): Serializable