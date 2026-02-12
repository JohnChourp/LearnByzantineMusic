package com.johnchourp.learnbyzantinemusic.editor.palette

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.editor.model.SymbolDefinition

class SymbolPaletteAdapter(
    private val symbols: List<SymbolDefinition>,
    private val symbolTypefaces: Map<String, Typeface>,
    private val onSymbolClicked: (SymbolDefinition) -> Unit
) : RecyclerView.Adapter<SymbolPaletteAdapter.SymbolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_symbol, parent, false)
        return SymbolViewHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        holder.bind(symbols[position], symbolTypefaces, onSymbolClicked)
    }

    override fun getItemCount(): Int = symbols.size

    class SymbolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symbolText: TextView = itemView.findViewById(R.id.symbol_text)

        fun bind(
            symbol: SymbolDefinition,
            symbolTypefaces: Map<String, Typeface>,
            onSymbolClicked: (SymbolDefinition) -> Unit
        ) {
            symbolText.typeface = symbolTypefaces[symbol.fontId] ?: Typeface.DEFAULT
            symbolText.text = symbol.text
            symbolText.contentDescription = symbol.label
            symbolText.setOnClickListener { onSymbolClicked(symbol) }
        }
    }
}
