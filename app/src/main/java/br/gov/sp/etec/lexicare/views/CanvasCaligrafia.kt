package br.gov.sp.etec.lexicare.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CanvasCaligrafia(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Pincel do usuário
    private var pathUsuario = Path()
    private val paintUsuario = Paint().apply {
        color = Color.parseColor("#1976D2") // Azul bonito
        style = Paint.Style.STROKE
        strokeWidth = 18f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Pincel do modelo (fundo cinza)
    private val paintModelo = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 18f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // O Path invisível que guarda o desenho exato da letra para comparação
    private var pathModelo = Path()

    private var letraAtual: Char = 'A'
    private var pontos = 0
    private var escala = 100f // "tamanho" da letra atual, usado para calibrar a tolerância da pontuação
    private var ultimaPrecisao = 0f
    private var ultimaCobertura = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        // Desenha o modelo cinza
        canvas.drawPath(pathModelo, paintModelo)

        // Desenha o traço do usuário por cima
        canvas.drawPath(pathUsuario, paintUsuario)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gerarPathModelo()
    }

    private fun gerarPathModelo() {
        pathModelo.reset()
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f
        val centerY = h / 2f
        // Limita pela altura também, para a letra nunca ser cortada (ex.: tablets/paisagem)
        val tamanho = min(w * 0.25f, h * 0.32f)
        escala = tamanho

        // Criamos o desenho da letra usando linhas e curvas no Path
        when (letraAtual.uppercaseChar()) {
            'A' -> {
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho * 0.6f, centerY + tamanho * 0.3f)
                pathModelo.lineTo(centerX + tamanho * 0.6f, centerY + tamanho * 0.3f)
            }
            'B' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 0.9f, centerX + tamanho * 0.6f, centerY + tamanho * 0.1f, -90f, 180f)
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 0.1f, centerX + tamanho * 0.8f, centerY + tamanho * 1.2f, -90f, 180f)
            }
            'C' -> {
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 1.2f, centerX + tamanho, centerY + tamanho * 1.2f, 30f, 300f)
            }
            'D' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 1.2f, centerX + tamanho * 0.8f, centerY + tamanho * 1.2f, -90f, 180f)
            }
            'E' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY)
                pathModelo.lineTo(centerX + tamanho * 0.6f, centerY)
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
            'F' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY)
                pathModelo.lineTo(centerX + tamanho * 0.6f, centerY)
            }
            'G' -> {
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 1.2f, centerX + tamanho, centerY + tamanho * 1.2f, 30f, 300f)
                pathModelo.moveTo(centerX, centerY + tamanho * 0.3f)
                pathModelo.lineTo(centerX + tamanho * 0.6f, centerY + tamanho * 0.3f)
            }
            'H' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY)
                pathModelo.lineTo(centerX + tamanho, centerY)
            }
            'I' -> {
                pathModelo.moveTo(centerX - tamanho * 0.8f, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho * 0.8f, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho * 0.8f, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho * 0.8f, centerY + tamanho * 1.2f)
            }
            'J' -> {
                pathModelo.moveTo(centerX + tamanho * 0.8f, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho * 0.8f, centerY + tamanho * 0.6f)
                pathModelo.addArc(centerX - tamanho * 0.5f, centerY + tamanho * 0.4f, centerX + tamanho * 0.8f, centerY + tamanho * 1.2f, 0f, 180f)
            }
            'K' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
            'L' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
            'M' -> {
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY + tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
            'N' -> {
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
            }
            'O' -> {
                pathModelo.addCircle(centerX, centerY, tamanho * 0.9f, Path.Direction.CW)
            }
            'P' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 1.2f, centerX + tamanho * 0.8f, centerY + tamanho * 0.2f, -90f, 180f)
            }
            'Q' -> {
                pathModelo.addCircle(centerX, centerY, tamanho * 0.9f, Path.Direction.CW)
                pathModelo.moveTo(centerX + tamanho * 0.4f, centerY + tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho * 0.9f, centerY + tamanho * 0.9f)
            }
            'R' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.addArc(centerX - tamanho, centerY - tamanho * 1.2f, centerX + tamanho * 0.8f, centerY + tamanho * 0.2f, -90f, 180f)
                pathModelo.moveTo(centerX - tamanho, centerY + tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
            'S' -> {
                // "S" desenhado com curvas suaves (simétrico em 180°): curva superior aberta
                // para a direita, diagonal central e curva inferior aberta para a esquerda.
                val t = tamanho
                val k = 0.9f // achata um pouco na vertical para ter a mesma altura das outras letras
                fun px(x: Float) = centerX + x * t
                fun py(y: Float) = centerY + y * t * k
                pathModelo.moveTo(px(0.80f), py(-0.75f))
                pathModelo.cubicTo(px(0.50f), py(-1.25f), px(-0.85f), py(-1.35f), px(-0.85f), py(-0.60f))
                pathModelo.cubicTo(px(-0.85f), py(-0.15f), px(-0.45f), py(-0.12f), px(0.00f), py(0.00f))
                pathModelo.cubicTo(px(0.45f), py(0.12f), px(0.85f), py(0.15f), px(0.85f), py(0.60f))
                pathModelo.cubicTo(px(0.85f), py(1.35f), px(-0.50f), py(1.25f), px(-0.80f), py(0.75f))
            }
            'T' -> {
                pathModelo.moveTo(centerX - tamanho * 0.8f, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho * 0.8f, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY + tamanho * 1.2f)
            }
            'U' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 0.6f)
                pathModelo.addArc(centerX - tamanho, centerY + tamanho * 0.4f, centerX + tamanho, centerY + tamanho * 1.2f, 0f, 180f)
                pathModelo.moveTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 0.6f)
            }
            'V' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
            }
            'W' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho * 0.5f, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY - tamanho * 0.4f)
                pathModelo.lineTo(centerX + tamanho * 0.5f, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
            }
            'X' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
                pathModelo.moveTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
            }
            'Y' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX, centerY)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.moveTo(centerX, centerY)
                pathModelo.lineTo(centerX, centerY + tamanho * 1.2f)
            }
            'Z' -> {
                pathModelo.moveTo(centerX - tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY - tamanho * 1.2f)
                pathModelo.lineTo(centerX - tamanho, centerY + tamanho * 1.2f)
                pathModelo.lineTo(centerX + tamanho, centerY + tamanho * 1.2f)
            }
        }
    }

    fun setLetra(letra: Char) {
        letraAtual = letra.uppercaseChar()
        limpar()
        gerarPathModelo()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: return false
        val y = event?.y ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Evita que o ScrollView roube o gesto enquanto a criança desenha
                parent?.requestDisallowInterceptTouchEvent(true)
                pathUsuario.moveTo(x, y)
                pathUsuario.lineTo(x + 0.1f, y) // permite desenhar um ponto
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                pathUsuario.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Pontuação de 0 a 100 comparando o traço do usuário com o modelo, nos DOIS sentidos:
     *  - precisão: quanto do que o usuário desenhou está perto do modelo (penaliza rabiscos fora da letra);
     *  - cobertura: quanto do modelo foi realmente traçado (penaliza desenhar só um pedaço da letra).
     * A nota final combina as duas (média harmônica / F1), a proximidade média do traço e uma
     * penalidade para excesso de tinta. A tolerância é proporcional ao tamanho da letra,
     * então vale igual em celular, tablet ou TV.
     */
    fun verificarPontuacao(): Int {
        pontos = 0
        ultimaPrecisao = 0f
        ultimaCobertura = 0f
        if (pathUsuario.isEmpty) return pontos

        val passo = 4f
        val (usuario, compUsuario) = extrairPontos(pathUsuario, passo)
        val (modelo, compModelo) = extrairPontos(pathModelo, passo)
        if (usuario.isEmpty() || modelo.isEmpty() || compModelo <= 0f) return pontos

        // Traço muito curto perto do tamanho da letra = não tentou de verdade
        if (compUsuario < compModelo * 0.25f) return pontos

        val tolerancia = max(escala * 0.30f, paintUsuario.strokeWidth * 2f)

        // Distância de cada ponto do usuário até o ponto mais próximo do modelo (e vice-versa)
        val distUsuario = FloatArray(usuario.size) { Float.MAX_VALUE }
        val distModelo = FloatArray(modelo.size) { Float.MAX_VALUE }
        for (i in usuario.indices) {
            val ux = usuario[i].first
            val uy = usuario[i].second
            for (j in modelo.indices) {
                val dx = ux - modelo[j].first
                val dy = uy - modelo[j].second
                val d2 = dx * dx + dy * dy
                if (d2 < distUsuario[i]) distUsuario[i] = d2
                if (d2 < distModelo[j]) distModelo[j] = d2
            }
        }
        for (i in distUsuario.indices) distUsuario[i] = kotlin.math.sqrt(distUsuario[i])
        for (j in distModelo.indices) distModelo[j] = kotlin.math.sqrt(distModelo[j])

        val precisao = distUsuario.count { it <= tolerancia }.toFloat() / distUsuario.size
        val cobertura = distModelo.count { it <= tolerancia }.toFloat() / distModelo.size
        ultimaPrecisao = precisao
        ultimaCobertura = cobertura

        if (precisao + cobertura <= 0f) return pontos
        val f1 = 2f * precisao * cobertura / (precisao + cobertura)

        // Quão colado no modelo está o traço (média das distâncias nos dois sentidos)
        val mediaDist = (distUsuario.average().toFloat() + distModelo.average().toFloat()) / 2f
        val proximidade = (1f - mediaDist / tolerancia).coerceIn(0f, 1f)

        // Rabisco: muito mais tinta do que a letra pede
        val razaoTinta = compUsuario / compModelo
        val penalidadeTinta = if (razaoTinta > 2f) (2f / razaoTinta).coerceIn(0.3f, 1f) else 1f

        val nota = f1 * (0.7f + 0.3f * proximidade) * penalidadeTinta * 100f
        pontos = nota.toInt().coerceIn(0, 100)
        return pontos
    }

    /** Dica curta baseada na última verificação. */
    fun getDica(): String = when {
        pontos >= 90 -> "Excelente! Seu traço ficou quase igual ao modelo."
        pontos >= 70 -> "Muito bom! Só falta um pouquinho de capricho."
        ultimaCobertura < 0.6f -> "Faltou passar por cima de partes da letra. Siga todo o modelo."
        ultimaPrecisao < 0.6f -> "Seu traço saiu do caminho. Fique mais perto do modelo cinza."
        else -> "Quase lá! Tente de novo seguindo o modelo com calma."
    }

    // Transforma um Path (com todos os traços/contornos) em pontos (X, Y) a cada "passo" pixels.
    // Retorna também o comprimento total. Antes só o 1º traço era lido; agora todos.
    private fun extrairPontos(path: Path, passo: Float): Pair<List<Pair<Float, Float>>, Float> {
        val lista = mutableListOf<Pair<Float, Float>>()
        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        var total = 0f
        do {
            val comprimento = pm.length
            if (comprimento > 0f) {
                total += comprimento
                var distancia = 0f
                while (distancia < comprimento) {
                    pm.getPosTan(distancia, pos, null)
                    lista.add(Pair(pos[0], pos[1]))
                    distancia += passo
                }
                pm.getPosTan(comprimento, pos, null)
                lista.add(Pair(pos[0], pos[1]))
            }
        } while (pm.nextContour())
        return Pair(lista, total)
    }

    fun limpar() {
        pathUsuario.reset()
        pontos = 0
        ultimaPrecisao = 0f
        ultimaCobertura = 0f
        invalidate()
    }

    fun getPontos(): Int = pontos
}