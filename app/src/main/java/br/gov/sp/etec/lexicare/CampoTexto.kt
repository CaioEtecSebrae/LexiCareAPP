package br.gov.sp.etec.lexicare

import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText

/**
 * Campos de texto sem limite de caracteres, que quebram a linha sozinhos.
 *
 * Em vez de o texto rolar na horizontal (o que fazia a borda "bugar" depois de uns 33/34
 * caracteres), o campo cresce para baixo e a borda acompanha o novo tamanho.
 *
 * O XML do campo precisa ser multilinha (`textMultiLine`). Aqui a gente só:
 *  - tira a rolagem horizontal e qualquer limite de linhas;
 *  - troca o tipo "cru" do teclado para que o Enter vire "Próximo" (e não pule linha);
 *  - bloqueia quebras de linha digitadas/coladas (nome, série e e-mail são de uma linha lógica só).
 */
object CampoTexto {

    fun quebraAutomatica(campo: EditText, tipoTeclado: Int, semEspacos: Boolean = false) {
        campo.setHorizontallyScrolling(false)
        campo.maxLines = Int.MAX_VALUE
        campo.setRawInputType(tipoTeclado)
        campo.imeOptions = EditorInfo.IME_ACTION_NEXT

        val filtroLinha = InputFilter { origem, inicio, fim, _, _, _ ->
            val trecho = origem.subSequence(inicio, fim)
            val temProibido = trecho.any { it == '\n' || it == '\r' || (semEspacos && it == ' ') }
            if (!temProibido) {
                null
            } else {
                trecho.filter { it != '\n' && it != '\r' && !(semEspacos && it == ' ') }
            }
        }
        // Mantém quaisquer filtros que já existam, sem acrescentar limite de tamanho.
        campo.filters = campo.filters
            .filter { it !is InputFilter.LengthFilter }
            .toTypedArray() + filtroLinha

        // Enter de teclado físico (computador/TV): vai para o próximo campo em vez de pular linha.
        campo.setOnKeyListener { v, keyCode, evento ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && evento.action == KeyEvent.ACTION_DOWN) {
                v.focusSearch(View.FOCUS_DOWN)?.requestFocus()
                true
            } else {
                false
            }
        }
    }

    val TECLADO_NOME = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
            InputType.TYPE_TEXT_FLAG_CAP_WORDS

    val TECLADO_TEXTO = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

    val TECLADO_EMAIL = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
}
