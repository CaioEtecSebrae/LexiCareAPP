package br.gov.sp.etec.lexicare.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import br.gov.sp.etec.lexicare.R

/**
 * LinearLayout com largura máxima opcional ([R.dimen.content_max_width], hoje sem limite prático,
 * ou o atributo app:larguraMaxima). Telas comuns ocupam a janela inteira; só login/cadastro usam coluna.
 *
 * Em celulares o limite não faz diferença (a tela é mais estreita que ele).
 * Em tablets, TVs, janelas de desktop/ChromeOS e celulares deitados, o conteúdo
 * fica numa coluna centralizada, em vez de esticar de ponta a ponta.
 * Use com android:layout_gravity="center_horizontal" no pai.
 */
class MaxWidthLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var larguraMaximaPx = context.resources.getDimensionPixelSize(R.dimen.content_max_width)

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MaxWidthLinearLayout)
            larguraMaximaPx = a.getDimensionPixelSize(R.styleable.MaxWidthLinearLayout_larguraMaxima, larguraMaximaPx)
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var largura = widthMeasureSpec
        val modo = MeasureSpec.getMode(largura)
        val tamanho = MeasureSpec.getSize(largura)
        if (modo != MeasureSpec.UNSPECIFIED && tamanho > larguraMaximaPx) {
            largura = MeasureSpec.makeMeasureSpec(larguraMaximaPx, modo)
        }
        super.onMeasure(largura, heightMeasureSpec)
    }
}