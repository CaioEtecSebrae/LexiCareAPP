package br.gov.sp.etec.lexicare.model

import java.io.Serializable

data class Anotacao(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val data: String
) : Serializable