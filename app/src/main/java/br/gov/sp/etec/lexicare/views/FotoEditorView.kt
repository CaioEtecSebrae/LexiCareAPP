package br.gov.sp.etec.lexicare.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Editor simples de foto de perfil: mostra a imagem com uma "janela" circular no centro.
 * - Arrastar com o dedo/mouse move a imagem;
 * - Pinça (dois dedos) ou o controle de zoom mudam a escala;
 * - O tamanho da janela de recorte também pode ser ajustado;
 * - [girar90] gira a imagem; [gerarRecorte] devolve só o que está dentro da janela.
 */
class FotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val ZOOM_MAX = 4f
        const val FRACAO_MIN = 0.40f
        const val FRACAO_MAX = 0.95f
    }

    private var bitmap: Bitmap? = null
    private val matriz = Matrix()
    private var escala = 1f          // pixels da tela por pixel da imagem
    private var centroX = 0f         // onde fica o centro da imagem na tela
    private var centroY = 0f
    private var zoom = 1f            // 1 = a imagem mal cobre a janela; ZOOM_MAX = 4x mais
    private var fracaoRecorte = 0.85f

    /** Avisa a tela quando o zoom muda por gesto de pinça (para mexer no controle de zoom). */
    var aoMudarZoom: ((Float) -> Unit)? = null

    private val densidade = resources.displayMetrics.density
    private val pincelImagem = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val pincelEscuro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA000000.toInt() }
    private val pincelBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * densidade
    }
    private val caminho = Path()

    private val detectorEscala = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            definirZoom(zoom * detector.scaleFactor, notificar = true)
            return true
        }
    })
    private var ultimoX = 0f
    private var ultimoY = 0f
    private var idPonteiro = -1

    fun definirBitmap(b: Bitmap) {
        bitmap = b
        zoom = 1f
        recentralizar()
        invalidate()
    }

    fun definirZoom(novoZoom: Float, notificar: Boolean = false) {
        zoom = novoZoom.coerceIn(1f, ZOOM_MAX)
        aplicarEscala()
        if (notificar) aoMudarZoom?.invoke(zoom)
        invalidate()
    }

    fun definirFracaoRecorte(fracao: Float) {
        fracaoRecorte = fracao.coerceIn(FRACAO_MIN, FRACAO_MAX)
        aplicarEscala()
        invalidate()
    }

    fun girar90() {
        val b = bitmap ?: return
        val m = Matrix().apply { postRotate(90f) }
        bitmap = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        zoom = 1f
        recentralizar()
        aoMudarZoom?.invoke(1f)
        invalidate()
    }

    /** Devolve a parte da imagem que está dentro do círculo, já no tamanho final (quadrado). */
    fun gerarRecorte(tamanhoSaida: Int): Bitmap? {
        val b = bitmap ?: return null
        val lado = ladoRecorte()
        val esquerdaImg = centroX - b.width * escala / 2f
        val topoImg = centroY - b.height * escala / 2f
        val x = ((width / 2f - lado / 2f - esquerdaImg) / escala).roundToInt().coerceIn(0, b.width - 1)
        val y = ((height / 2f - lado / 2f - topoImg) / escala).roundToInt().coerceIn(0, b.height - 1)
        val tam = (lado / escala).roundToInt().coerceAtLeast(1)
        val w = tam.coerceAtMost(b.width - x).coerceAtLeast(1)
        val h = tam.coerceAtMost(b.height - y).coerceAtLeast(1)
        val recorte = Bitmap.createBitmap(b, x, y, w, h)
        return Bitmap.createScaledBitmap(recorte, tamanhoSaida, tamanhoSaida, true)
    }

    private fun ladoRecorte(): Float = min(width, height) * fracaoRecorte

    private fun escalaMinima(): Float {
        val b = bitmap ?: return 1f
        return ladoRecorte() / min(b.width, b.height)
    }

    private fun aplicarEscala() {
        escala = escalaMinima() * zoom
        limitarCentro()
    }

    private fun recentralizar() {
        centroX = width / 2f
        centroY = height / 2f
        aplicarEscala()
    }

    /** Impede de arrastar a imagem para fora: a janela de recorte sempre fica sobre a foto. */
    private fun limitarCentro() {
        val b = bitmap ?: return
        val meioL = b.width * escala / 2f
        val meioA = b.height * escala / 2f
        val metadeJanela = ladoRecorte() / 2f
        val cx = width / 2f
        val cy = height / 2f
        val minX = cx + metadeJanela - meioL
        val maxX = cx - metadeJanela + meioL
        val minY = cy + metadeJanela - meioA
        val maxY = cy - metadeJanela + meioA
        centroX = if (minX > maxX) cx else centroX.coerceIn(minX, maxX)
        centroY = if (minY > maxY) cy else centroY.coerceIn(minY, maxY)
    }

    /**
     * Altura que se adapta ao aparelho: quadrada em telas normais, mas nunca mais que ~45% da
     * altura da janela (celular deitado, janela pequena) e nunca menor que 180dp.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val largura = MeasureSpec.getSize(widthMeasureSpec)
        val dm = resources.displayMetrics
        val maxAltura = (dm.heightPixels * 0.45f).toInt()
        val minAltura = (180 * dm.density).toInt()
        val altura = min(largura, maxAltura).coerceAtLeast(minAltura)
        setMeasuredDimension(resolveSize(largura, widthMeasureSpec), altura)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recentralizar()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = bitmap ?: return
        matriz.reset()
        matriz.postScale(escala, escala)
        matriz.postTranslate(centroX - b.width * escala / 2f, centroY - b.height * escala / 2f)
        canvas.drawBitmap(b, matriz, pincelImagem)

        // Escurece tudo que fica fora do círculo de recorte
        val cx = width / 2f
        val cy = height / 2f
        val raio = ladoRecorte() / 2f
        caminho.reset()
        caminho.fillType = Path.FillType.EVEN_ODD
        caminho.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        caminho.addCircle(cx, cy, raio, Path.Direction.CW)
        canvas.drawPath(caminho, pincelEscuro)
        canvas.drawCircle(cx, cy, raio, pincelBorda)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (bitmap == null) return false
        detectorEscala.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                idPonteiro = e.getPointerId(0)
                ultimoX = e.x
                ultimoY = e.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val i = e.findPointerIndex(idPonteiro)
                if (i >= 0) {
                    if (!detectorEscala.isInProgress) {
                        centroX += e.getX(i) - ultimoX
                        centroY += e.getY(i) - ultimoY
                        limitarCentro()
                        invalidate()
                    }
                    ultimoX = e.getX(i)
                    ultimoY = e.getY(i)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val saiu = e.actionIndex
                if (e.getPointerId(saiu) == idPonteiro) {
                    val novo = if (saiu == 0) 1 else 0
                    idPonteiro = e.getPointerId(novo)
                    ultimoX = e.getX(novo)
                    ultimoY = e.getY(novo)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                idPonteiro = -1
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}
