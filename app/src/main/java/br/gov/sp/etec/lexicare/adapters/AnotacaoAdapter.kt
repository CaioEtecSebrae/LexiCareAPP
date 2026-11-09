package br.gov.sp.etec.lexicare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.gov.sp.etec.lexicare.R
import br.gov.sp.etec.lexicare.model.Anotacao

class AnotacaoAdapter(
    private var anotacoes: List<Anotacao>,
    private val onItemClick: (Anotacao) -> Unit,
    private val onItemLongClick: (Anotacao) -> Unit,
    private val onDeleteClick: (Anotacao) -> Unit // NOVO
) : RecyclerView.Adapter<AnotacaoAdapter.AnotacaoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnotacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anotacao, parent, false)
        return AnotacaoViewHolder(view, onDeleteClick) // Passa a ação
    }

    override fun onBindViewHolder(holder: AnotacaoViewHolder, position: Int) {
        val anotacao = anotacoes[position]
        holder.bind(anotacao)

        holder.itemView.setOnClickListener {
            onItemClick(anotacao)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(anotacao)
            true
        }
    }

    override fun getItemCount(): Int = anotacoes.size

    fun atualizarLista(novaLista: List<Anotacao>) {
        anotacoes = novaLista
        notifyDataSetChanged()
    }

    class AnotacaoViewHolder(
        itemView: View,
        private val onDeleteClick: (Anotacao) -> Unit // NOVO
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloAnotacao)
        private val tvConteudo: TextView = itemView.findViewById(R.id.tvConteudoAnotacao)
        private val tvData: TextView = itemView.findViewById(R.id.tvDataAnotacao)
        private val btnDeletar: TextView = itemView.findViewById(R.id.btnDeletarAnotacao) // NOVO

        fun bind(anotacao: Anotacao) {
            tvTitulo.text = anotacao.titulo
            tvConteudo.text = anotacao.conteudo
            tvData.text = anotacao.data

            // Configura o clique do botão de deletar
            btnDeletar.setOnClickListener {
                onDeleteClick(anotacao)
            }
        }
    }
}