package br.gov.sp.etec.lexicare

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CadastroActivity : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val USUARIOS_KEY = "usuarios"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        sharedPreferences = getSharedPreferences("LexiCarePrefs", MODE_PRIVATE)

        val etNomeCompleto = findViewById<EditText>(R.id.etNomeCompleto)
        val etIdade = findViewById<EditText>(R.id.etIdade)
        val etEmail = findViewById<EditText>(R.id.etEmailCadastro)
        val etSenha = findViewById<EditText>(R.id.etSenhaCadastro)
        val etSerie = findViewById<EditText>(R.id.etSerie)
        val rgTranstorno = findViewById<RadioGroup>(R.id.rgTranstorno)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val tvFazerLogin = findViewById<TextView>(R.id.tvFazerLogin)
        val cbTermosUso = findViewById<CheckBox>(R.id.cbTermosUso)
        val cbTratamentoDados = findViewById<CheckBox>(R.id.cbTratamentoDados)
        val cbResponsavel = findViewById<CheckBox>(R.id.cbResponsavel)
        val tvLerTermos = findViewById<TextView>(R.id.tvLerTermos)

        tvLerTermos.setOnClickListener { mostrarTermos() }

        // Nome, série e e-mail: sem limite de caracteres; ao chegar na borda o texto quebra
        // para a linha de baixo e o campo cresce (a borda acompanha, sem "bugar").
        CampoTexto.quebraAutomatica(etNomeCompleto, CampoTexto.TECLADO_NOME)
        CampoTexto.quebraAutomatica(etSerie, CampoTexto.TECLADO_TEXTO)
        CampoTexto.quebraAutomatica(etEmail, CampoTexto.TECLADO_EMAIL, semEspacos = true)

        // Ao marcar, o texto que estava em vermelho volta à cor normal
        listOf(cbTermosUso, cbTratamentoDados, cbResponsavel).forEach { cb ->
            cb.setOnCheckedChangeListener { _, marcado ->
                if (marcado) cb.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }

        tvFazerLogin.setOnClickListener {
            finish()
        }

        btnCadastrar.setOnClickListener {
            val nome = etNomeCompleto.text.toString().trim()
            val idade = etIdade.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val senha = etSenha.text.toString()
            val serie = etSerie.text.toString().trim()

            when {
                nome.isEmpty() -> {
                    etNomeCompleto.error = "Digite seu nome completo"
                    etNomeCompleto.requestFocus()
                }

                idade.isEmpty() -> {
                    etIdade.error = "Digite sua idade"
                    etIdade.requestFocus()
                }

                idade.toIntOrNull() == null -> {
                    etIdade.error = "Digite uma idade válida"
                    etIdade.requestFocus()
                }

                idade.toInt() < 4 || idade.toInt() > 120 -> {
                    etIdade.error = "Idade inválida"
                    etIdade.requestFocus()
                }

                email.isEmpty() -> {
                    etEmail.error = "Digite seu e-mail"
                    etEmail.requestFocus()
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "E-mail inválido"
                    etEmail.requestFocus()
                }

                senha.isEmpty() -> {
                    etSenha.error = "Digite uma senha"
                    etSenha.requestFocus()
                }

                senha.length < 6 -> {
                    etSenha.error = "A senha deve ter pelo menos 6 caracteres"
                    etSenha.requestFocus()
                }

                serie.isEmpty() -> {
                    etSerie.error = "Digite sua série/ano"
                    etSerie.requestFocus()
                }

                rgTranstorno.checkedRadioButtonId == -1 -> {
                    Toast.makeText(this, "Selecione o tipo de transtorno", Toast.LENGTH_SHORT).show()
                }
                // Os três termos de consentimento são obrigatórios para cadastrar
                !cbTermosUso.isChecked || !cbTratamentoDados.isChecked || !cbResponsavel.isChecked -> {
                    val cor = ContextCompat.getColor(this, R.color.text_primary)
                    val corErro = ContextCompat.getColor(this, R.color.error)
                    listOf(cbTermosUso, cbTratamentoDados, cbResponsavel).forEach {
                        it.setTextColor(if (it.isChecked) cor else corErro)
                    }
                    Toast.makeText(
                        this,
                        "Aceite todos os termos de consentimento para criar a conta",
                        Toast.LENGTH_LONG
                    ).show()
                    listOf(cbTermosUso, cbTratamentoDados, cbResponsavel)
                        .first { !it.isChecked }
                        .requestFocus()
                }
                usuarioJaExiste(email) -> {
                    etEmail.error = "Este e-mail já está cadastrado"
                    etEmail.requestFocus()
                }
                else -> {
                    val transtornoId = rgTranstorno.checkedRadioButtonId
                    val transtorno = findViewById<RadioButton>(transtornoId).text.toString()

                    salvarUsuario(nome, idade, email, senha, serie, transtorno)

                    Toast.makeText(
                        this,
                        "Cadastro realizado com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
            }
        } // <-- chave que faltava para fechar o setOnClickListener
    } // <-- fecha onCreate

    private fun mostrarTermos() {
        AlertDialog.Builder(this)
            .setTitle("Termos de consentimento")
            .setMessage(
                "1. Termos de Uso e Privacidade\n" +
                        "O LexiCare é um aplicativo de apoio à leitura e à escrita. Você concorda em usá-lo " +
                        "de forma respeitosa e apenas para fins educativos.\n\n" +
                        "2. Tratamento de dados (LGPD)\n" +
                        "Nome, idade, série, e-mail e tipo de transtorno informado são usados somente para " +
                        "personalizar os exercícios e não são compartilhados com terceiros. Você pode " +
                        "atualizar seus dados quando quiser em \"Meus dados\".\n\n" +
                        "3. Menores de idade\n" +
                        "Se o usuário for menor de 18 anos, o cadastro deve ser feito com a autorização " +
                        "e o acompanhamento de um responsável legal."
            )
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun usuarioJaExiste(email: String): Boolean {
        val usuarios = carregarUsuarios()
        return usuarios.any { it.email == email }
    }

    private fun salvarUsuario(
        nome: String,
        idade: String,
        email: String,
        senha: String,
        serie: String,
        transtorno: String
    ) {
        val usuarios = carregarUsuarios().toMutableList()
        val novoUsuario = Usuario(
            id = (usuarios.size + 1),
            nome = nome,
            idade = idade,
            email = email,
            senha = senha,
            serie = serie,
            transtorno = transtorno
        )
        usuarios.add(novoUsuario)

        // Salva a lista completa de usuários
        val json = Gson().toJson(usuarios)

        // Salvamos a lista E também o nome do usuário logado no momento
        sharedPreferences.edit()
            .putString(USUARIOS_KEY, json)
            .putString("nome_usuario_logado", nome)
            .apply()
    }

    private fun carregarUsuarios(): List<Usuario> {
        val json = sharedPreferences.getString(USUARIOS_KEY, "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Usuario>>() {}.type
            Gson().fromJson(json, type)
        }
    }

    data class Usuario(
        val id: Int,
        val nome: String,
        val idade: String,
        val email: String,
        val senha: String,
        val serie: String,
        val transtorno: String
    )
}