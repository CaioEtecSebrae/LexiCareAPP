package br.gov.sp.etec.lexicare.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.gov.sp.etec.lexicare.R
import br.gov.sp.etec.lexicare.model.Tarefa

class TarefaAdapter(
    listaInicial: List<Tarefa>,
    private val onItemClick: (Tarefa) -> Unit,
    private val onDeleteClick: (Tarefa) -> Unit
) : RecyclerView.Adapter<TarefaAdapter.TarefaViewHolder>() {

    private val lista: MutableList<Tarefa> = listaInicial.toMutableList()

    inner class TarefaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewCor: View = view.findViewById(R.id.viewCorTarefa)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloTarefa)
        val tvDificuldade: TextView = view.findViewById(R.id.tvDificuldadeTarefa)
        val tvDescricao: TextView = view.findViewById(R.id.tvDescricaoTarefa)
        val tvHorario: TextView = view.findViewById(R.id.tvHorarioTarefa)
        val btnDeletar: TextView = view.findViewById(R.id.btnDeletarTarefa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarefa, parent, false)
        return TarefaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        val tarefa = lista[position]

        holder.tvTitulo.text = tarefa.titulo
        holder.tvDificuldade.text = tarefa.dificuldade
        holder.tvDescricao.text = tarefa.descricao
        holder.tvHorario.text = "🕒 ${tarefa.horario}"

        // ✅ COR DA TAREFA (String hex), com fallback robusto
        val cor = try {
            Color.parseColor(tarefa.cor)
        } catch (e: Exception) {
            // pega null, "" e hex inválido de tarefas antigas
            ContextCompat.getColor(holder.itemView.context, R.color.primary)
        }
        holder.viewCor.setBackgroundColor(cor)

        holder.itemView.setOnClickListener { onItemClick(tarefa) }
        holder.btnDeletar.setOnClickListener { onDeleteClick(tarefa) }
    }

    override fun getItemCount(): Int = lista.size

    fun atualizarLista(novaLista: List<Tarefa>) {
        val copia = novaLista.toList()
        lista.clear()
        lista.addAll(copia)
        notifyDataSetChanged()
    }
}