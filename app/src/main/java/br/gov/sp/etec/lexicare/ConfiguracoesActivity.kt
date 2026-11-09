package br.gov.sp.etec.lexicare

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import br.gov.sp.etec.lexicare.views.FotoEditorView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class ConfiguracoesActivity : BaseActivity() {

    override val statusBarClara: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences
    private val USUARIOS_KEY = "usuarios"

    private lateinit var etNome: EditText
    private lateinit var etIdade: EditText
    private lateinit var etSerie: EditText
    private lateinit var etSenhaAtual: EditText
    private lateinit var etNovaSenha: EditText
    private lateinit var etConfirmarSenha: EditText
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var tvAvatarPlaceholder: TextView
    private var uriCamera: Uri? = null

    // Foto tirada na hora com a câmera (grava em uriCamera)
    private val tirarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) uriCamera?.let { abrirEditorDeFoto(it) }
    }

    // Foto escolhida da galeria / arquivos do aparelho
    private val escolherDaGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { abrirEditorDeFoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes)

        sharedPreferences = getSharedPreferences("LexiCarePrefs", MODE_PRIVATE)

        etNome = findViewById(R.id.etNome)
        etIdade = findViewById(R.id.etIdade)
        etSerie = findViewById(R.id.etSerie)
        etNovaSenha = findViewById(R.id.etNovaSenha)
        etConfirmarSenha = findViewById(R.id.etConfirmarSenha)
        val btnSalvar = findViewById<Button>(R.id.btnSalvarPerfil)
        val btnVoltarConfig = findViewById<Button>(R.id.btnVoltarConfig)
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        tvAvatarPlaceholder = findViewById(R.id.tvAvatarPlaceholder)

        // Deixa a foto redonda e abre as opções ao tocar na foto ou no botão
        findViewById<FrameLayout>(R.id.flAvatar).apply {
            clipToOutline = true
            setOnClickListener { mostrarOpcoesDeFoto() }
        }
        findViewById<Button>(R.id.btnAlterarFoto).setOnClickListener { mostrarOpcoesDeFoto() }
        mostrarFotoSalva()

        // Botão voltar
        btnVoltarConfig.setOnClickListener {
            finish()
        }

        // Carrega dados já salvos (se houver)
        carregarPerfil()

        // Botão salvar
        btnSalvar.setOnClickListener {
            salvarPerfil()
        }

        // --- LOGOUT ---
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sair da conta")
                .setMessage("Tem certeza que deseja sair?")
                .setPositiveButton("Sim, sair") { _, _ ->
                    // 1. Limpa o e-mail do usuário logado
                    sharedPreferences.edit().remove("email_logado").apply()

                    // 2. Redireciona para a tela de Login
                    // ATENÇÃO: Substitua "LoginActivity" pelo nome real da sua tela de login
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // ---------------- Foto de perfil ----------------

    /** Cada usuário (e-mail) tem a sua própria foto. */
    private fun arquivoDaFoto(): File {
        val pasta = File(filesDir, "fotos")
        pasta.mkdirs()
        return File(pasta, "perfil_${SessaoUsuario.emailParaArquivo(this)}.jpg")
    }

    private fun mostrarFotoSalva() {
        val arquivo = arquivoDaFoto()
        val bitmap = if (arquivo.exists()) BitmapFactory.decodeFile(arquivo.path) else null
        if (bitmap != null) {
            ivFotoPerfil.setImageBitmap(bitmap)
            ivFotoPerfil.visibility = View.VISIBLE
            tvAvatarPlaceholder.visibility = View.GONE
        } else {
            ivFotoPerfil.setImageDrawable(null)
            ivFotoPerfil.visibility = View.GONE
            tvAvatarPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun mostrarOpcoesDeFoto() {
        val temFoto = arquivoDaFoto().exists()
        val opcoes = mutableListOf("Tirar foto agora", "Escolher do aparelho")
        if (temFoto) opcoes.add("Remover foto")

        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(opcoes.toTypedArray()) { _, qual ->
                when (qual) {
                    0 -> iniciarCamera()
                    1 -> escolherDaGaleria.launch("image/*")
                    2 -> {
                        arquivoDaFoto().delete()
                        mostrarFotoSalva()
                        Toast.makeText(this, "Foto removida", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarCamera() {
        try {
            val pasta = File(cacheDir, "fotos")
            pasta.mkdirs()
            val temporario = File(pasta, "camera.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", temporario)
            uriCamera = uri
            tirarFoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Não há app de câmera neste aparelho. Escolha uma foto do aparelho.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show()
        }
    }

    /** Lê a imagem já reduzida (para não estourar a memória) e na posição certa (EXIF). */
    private fun carregarBitmap(uri: Uri, ladoMaximo: Int = 1800): Bitmap? {
        return try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var amostra = 1
            while (max(limites.outWidth, limites.outHeight) / amostra > ladoMaximo) amostra *= 2

            val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
                ?: return null

            val graus = try {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    when (ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            if (graus != 0f) {
                val m = Matrix().apply { postRotate(graus) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Tela para ajustar a foto: arrastar, zoom (escala), tamanho do recorte e girar. */
    private fun abrirEditorDeFoto(uri: Uri) {
        val bitmap = carregarBitmap(uri)
        if (bitmap == null) {
            Toast.makeText(this, "Não foi possível abrir essa imagem", Toast.LENGTH_SHORT).show()
            return
        }

        val visao = layoutInflater.inflate(R.layout.dialog_editar_foto, null)
        val editor = visao.findViewById<FotoEditorView>(R.id.fotoEditor)
        val sbZoom = visao.findViewById<SeekBar>(R.id.sbZoom)
        val sbRecorte = visao.findViewById<SeekBar>(R.id.sbRecorte)
        val dialogo = AlertDialog.Builder(this).setView(visao).create()

        editor.definirBitmap(bitmap)
        editor.definirFracaoRecorte(FotoEditorView.FRACAO_MIN + sbRecorte.progress / 100f)

        sbZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progresso: Int, doUsuario: Boolean) {
                if (doUsuario) editor.definirZoom(1f + progresso / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        sbRecorte.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progresso: Int, doUsuario: Boolean) {
                if (doUsuario) editor.definirFracaoRecorte(FotoEditorView.FRACAO_MIN + progresso / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        // Pinça com dois dedos também move o controle de zoom
        editor.aoMudarZoom = { z -> sbZoom.progress = ((z - 1f) * 100f).roundToInt() }

        visao.findViewById<Button>(R.id.btnGirarFoto).setOnClickListener { editor.girar90() }
        visao.findViewById<Button>(R.id.btnCancelarFoto).setOnClickListener { dialogo.dismiss() }
        visao.findViewById<Button>(R.id.btnSalvarFoto).setOnClickListener {
            val recorte = editor.gerarRecorte(512)
            if (recorte == null) {
                Toast.makeText(this, "Não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                arquivoDaFoto().outputStream().use { recorte.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                mostrarFotoSalva()
                Toast.makeText(this, "Foto atualizada!", Toast.LENGTH_SHORT).show()
                dialogo.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
            }
        }

        dialogo.show()
        val larguraTela = resources.displayMetrics.widthPixels
        val densidade = resources.displayMetrics.density
        dialogo.window?.setLayout(
            minOf((larguraTela * 0.92).toInt(), (560 * densidade).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun carregarPerfil() {
        val emailLogado = sharedPreferences.getString("email_logado", "") ?: ""
        val usuarios = carregarUsuarios()
        val usuario = usuarios.firstOrNull { it.email == emailLogado }

        if (usuario != null) {
            etNome.setText(usuario.nome)
            etIdade.setText(usuario.idade)
            etSerie.setText(usuario.serie) // <-- AGORA CARREGA A SÉRIE TAMBÉM
        } else {
            // Fallback: se não achar na lista, carrega das chaves antigas
            etNome.setText(sharedPreferences.getString("perfil_nome", ""))
            etIdade.setText(sharedPreferences.getString("perfil_idade", ""))
            etSerie.setText(sharedPreferences.getString("perfil_serie", ""))
        }
    }

    private fun salvarPerfil() {
        val nome = etNome.text.toString().trim()
        val idade = etIdade.text.toString().trim()
        val serie = etSerie.text.toString().trim()

        val senhaAtual = etSenhaAtual.text.toString()
        val senhaNova = etNovaSenha.text.toString()
        val senhaConfirmada = etConfirmarSenha.text.toString()

        if (nome.isEmpty()) {
            etNome.error = "Digite seu nome"
            etNome.requestFocus()
            return
        }

        // A senha só é alterada quando algum dos campos de senha é preenchido.
        val desejaAlterarSenha = senhaAtual.isNotEmpty() ||
                senhaNova.isNotEmpty() ||
                senhaConfirmada.isNotEmpty()

        if (desejaAlterarSenha) {
            val emailLogado =
                sharedPreferences.getString("email_logado", "") ?: ""

            val usuarios = carregarUsuarios().toMutableList()
            val indice = usuarios.indexOfFirst { it.email == emailLogado }

            when {
                indice == -1 -> {
                    Toast.makeText(
                        this,
                        "Não foi possível localizar sua conta.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                senhaAtual.isEmpty() -> {
                    etSenhaAtual.error = "Digite sua senha atual"
                    etSenhaAtual.requestFocus()
                    return
                }

                senhaAtual != usuarios[indice].senha -> {
                    etSenhaAtual.error = "A senha atual não confere"
                    etSenhaAtual.requestFocus()
                    return
                }

                senhaNova.length < 6 -> {
                    etNovaSenha.error = "Use pelo menos 6 caracteres"
                    etNovaSenha.requestFocus()
                    return
                }

                senhaNova.length > 15 -> {
                    etNovaSenha.error = "Use no máximo 15 caracteres"
                    etNovaSenha.requestFocus()
                    return
                }

                senhaConfirmada.isEmpty() -> {
                    etConfirmarSenha.error = "Confirme a nova senha"
                    etConfirmarSenha.requestFocus()
                    return
                }

                senhaNova != senhaConfirmada -> {
                    etConfirmarSenha.error = "As senhas não coincidem"
                    etConfirmarSenha.requestFocus()
                    return
                }

                senhaNova == senhaAtual -> {
                    etNovaSenha.error = "Escolha uma senha diferente da atual"
                    etNovaSenha.requestFocus()
                    return
                }

                else -> {
                    usuarios[indice] = usuarios[indice].copy(senha = senhaNova)
                    sharedPreferences.edit()
                        .putString(USUARIOS_KEY, Gson().toJson(usuarios))
                        .apply()
                }
            }
        }

        // Salva os dados do perfil nas chaves usadas pelo Dashboard.
        sharedPreferences.edit()
            .putString("perfil_nome", nome)
            .putString("perfil_idade", idade)
            .putString("perfil_serie", serie)
            .apply()

        // Atualiza nome, idade e série sem perder a senha recém-alterada.
        atualizarUsuarioNaLista(nome, idade, serie)

        Toast.makeText(
            this,
            if (desejaAlterarSenha) {
                "Perfil e senha atualizados com sucesso!"
            } else {
                "Perfil salvo com sucesso!"
            },
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun atualizarUsuarioNaLista(novoNome: String, novaIdade: String, novaSerie: String) {
        val emailLogado = sharedPreferences.getString("email_logado", "") ?: ""
        val usuarios = carregarUsuarios().toMutableList()
        val index = usuarios.indexOfFirst { it.email == emailLogado }

        if (index != -1) {
            // Atualiza o usuário na lista (mantém email, senha e transtorno)
            usuarios[index] = usuarios[index].copy(
                nome = novoNome,
                idade = novaIdade,
                serie = novaSerie
            )

            // Salva a lista atualizada de volta no SharedPreferences
            val json = Gson().toJson(usuarios)
            sharedPreferences.edit().putString(USUARIOS_KEY, json).apply()
        }
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