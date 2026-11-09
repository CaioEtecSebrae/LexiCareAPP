package br.gov.sp.etec.lexicare

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LoginActivity : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val USUARIOS_KEY = "usuarios"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("LexiCarePrefs", MODE_PRIVATE)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etSenha = findViewById<EditText>(R.id.etSenha)
        val btnEntrar = findViewById<Button>(R.id.btnEntrar)
        val tvCadastrar = findViewById<TextView>(R.id.tvCadastrar)

        btnEntrar.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val senha = etSenha.text.toString()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validarLogin(email, senha)) {
                Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

                // 🔑 Salva o nome do usuário logado para a saudação do Dashboard
                val nomeUsuario = obterNomeUsuario(email)
                sharedPreferences.edit()
                    .putString("perfil_nome", nomeUsuario)
                    .putString("email_logado", email)
                    .apply()

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "E-mail ou senha incorretos!", Toast.LENGTH_SHORT).show()
            }
        }

        tvCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun validarLogin(email: String, senha: String): Boolean {
        val usuarios = carregarUsuarios()
        return usuarios.any { it.email == email && it.senha == senha }
    }

    // Retorna o nome do usuário logado (ou "Usuário" se não achar)
    private fun obterNomeUsuario(email: String): String {
        val usuarios = carregarUsuarios()
        val usuario = usuarios.firstOrNull { it.email == email }
        return usuario?.nome?.takeIf { it.isNotEmpty() } ?: "Usuário"
    }

    private fun carregarUsuarios(): List<CadastroActivity.Usuario> {
        val json = sharedPreferences.getString(USUARIOS_KEY, "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<CadastroActivity.Usuario>>() {}.type
            Gson().fromJson(json, type)
        }
    }
}