package br.gov.sp.etec.lexicare

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Base de todas as telas. Cuida do que muda de um aparelho para outro:
 *
 * 1. Barras do sistema (status, navegação, notch/recorte, teclado): o app desenha de
 *    ponta a ponta (obrigatório no Android 15+) e aplica os "insets" como padding,
 *    então nada fica escondido atrás de barras em nenhum aparelho.
 * 2. Anel de foco visível para navegar com controle remoto (TV), teclado ou D-pad.
 *
 * Para o cabeçalho colorido ocupar também a área da barra de status, dê a ele
 * o id @+id/headerTopo no XML.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** true = ícones escuros na barra de status (cabeçalho claro); false = ícones brancos (cabeçalho azul). */
    protected open val statusBarClara: Boolean = true

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        configurarBarrasDoSistema()
        configurarAnelDeFoco()
    }

    private fun configurarBarrasDoSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Antes do Android 8 não dá para ter ícones escuros na barra de navegação.
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.BLACK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val parametros = window.attributes
            parametros.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = parametros
        }

        val controle = WindowCompat.getInsetsController(window, window.decorView)
        controle.isAppearanceLightStatusBars = statusBarClara
        controle.isAppearanceLightNavigationBars = true

        val conteudo = findViewById<ViewGroup>(android.R.id.content)
        val raiz = conteudo.getChildAt(0) ?: return
        val cabecalho: View? = findViewById(R.id.headerTopo)

        val padEsq = raiz.paddingLeft
        val padTopo = raiz.paddingTop
        val padDir = raiz.paddingRight
        val padBase = raiz.paddingBottom
        val cabecalhoPadTopo = cabecalho?.paddingTop ?: 0

        (raiz as? ViewGroup)?.clipToPadding = false

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())

            if (cabecalho != null) {
                // O cabeçalho "engole" a barra de status (a cor dele vai até o topo da tela).
                cabecalho.setPadding(
                    cabecalho.paddingLeft, cabecalhoPadTopo + barras.top,
                    cabecalho.paddingRight, cabecalho.paddingBottom
                )
                raiz.setPadding(
                    padEsq + barras.left, padTopo,
                    padDir + barras.right, padBase + max(barras.bottom, teclado.bottom)
                )
            } else {
                raiz.setPadding(
                    padEsq + barras.left, padTopo + barras.top,
                    padDir + barras.right, padBase + max(barras.bottom, teclado.bottom)
                )
            }
            insets
        }
        ViewCompat.requestApplyInsets(raiz)
    }

    /**
     * Anel de foco. Em campos de texto de uma linha (e-mail, senha) o anel NÃO pode ser um
     * "foreground": quando o texto passa da largura do campo ele rola na horizontal e o
     * foreground rola junto, fazendo a borda sair do lugar. O background, ao contrário, é
     * sempre desenhado fixo, então nos EditText o anel entra como uma camada do background.
     */
    private fun configurarAnelDeFoco() {
        val anel = ContextCompat.getDrawable(this, R.drawable.foco_anel) ?: return
        val anelCirculo = ContextCompat.getDrawable(this, R.drawable.foco_anel_circulo) ?: anel
        var anterior: View? = null
        var fundoOriginal: Drawable? = null

        fun removerAnel() {
            val v = anterior ?: return
            if (v is EditText) {
                val l = v.paddingLeft; val t = v.paddingTop; val r = v.paddingRight; val b = v.paddingBottom
                v.background = fundoOriginal
                v.setPadding(l, t, r, b)
            } else {
                v.foreground = null
            }
            anterior = null
            fundoOriginal = null
        }

        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, novo ->
            removerAnel()
            if (novo != null && (novo.isClickable || novo is EditText)) {
                // Itens redondos (tag "circular") recebem um anel redondo, não retangular.
                val modelo = if (novo.tag == "circular") anelCirculo else anel
                val desenhoAnel = modelo.constantState?.newDrawable()?.mutate() ?: modelo
                if (novo is EditText) {
                    val l = novo.paddingLeft; val t = novo.paddingTop; val r = novo.paddingRight; val b = novo.paddingBottom
                    fundoOriginal = novo.background
                    val camadas = fundoOriginal?.let { LayerDrawable(arrayOf(it, desenhoAnel)) }
                        ?: LayerDrawable(arrayOf(desenhoAnel))
                    novo.background = camadas
                    novo.setPadding(l, t, r, b)
                } else {
                    novo.foreground = desenhoAnel
                }
                anterior = novo
            }
        }
    }
}
