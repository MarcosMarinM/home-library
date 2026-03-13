package com.marcosmarin.libros

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marcosmarin.libros.model.Book

class BookAdapter(
    private var books: List<Book> = emptyList(),
    private var colors: Map<String, String> = emptyMap(),
    private val onItemClick: (Book) -> Unit = {}
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    fun updateData(newBooks: List<Book>, newColors: Map<String, String>) {
        this.books = newBooks
        this.colors = newColors
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view, colors, onItemClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    class BookViewHolder(
        itemView: View,
        private val colors: Map<String, String>,
        private val onItemClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.tv_title)
        private val author = itemView.findViewById<TextView>(R.id.tv_author)
        private val extra = itemView.findViewById<TextView>(R.id.tv_extra)
        private val colorContainer = itemView.findViewById<LinearLayout>(R.id.color_container)

        fun bind(book: Book) {
            title.text = book.titulo
            author.text = book.autor

            extra.text = listOfNotNull(
                book.balda?.let { "Balda: $it" },
                book.cuadrante.takeIf { it.isNotBlank() }?.let { "Cuadrante: $it" },
                book.profundidad.takeIf { it.isNotBlank() }?.let { "Profundidad: $it" },
                book.posicionRelativa?.let { "Posición: $it" }
            ).joinToString(" • ")

            colorContainer.removeAllViews()
            val parts = book.colorLomo
                .split(Regex("\\s*(?:,|;|y)\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            parts.forEach { part ->
                val rawHex = colors[part.lowercase()] ?: return@forEach
                val hex = if (rawHex.startsWith("#")) rawHex else "#${rawHex.trim()}"

                try {
                    val swatch = View(itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                            setMargins(0, 0, 8, 0)
                        }
                        setBackgroundColor(Color.parseColor(hex))
                    }
                    colorContainer.addView(swatch)
                } catch (e: IllegalArgumentException) {
                    // Invalid color value; ignore and continue.
                }
            }

            itemView.setOnClickListener { onItemClick(book) }
        }
    }
}
