package com.marcosmarin.libros

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.marcosmarin.libros.model.Book

class MainActivity : AppCompatActivity() {

    private enum class SortMode { TITLE, AUTHOR, LOCATION, COLOR }

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: BookAdapter
    private var allBooks: List<Book> = emptyList()
    private var allColors: Map<String, String> = emptyMap()
    private var sortMode: SortMode = SortMode.LOCATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            // If the Sheet ID was not provided, show a clear message and stop.
            if (BuildConfig.GOOGLE_SHEET_ID.isBlank()) {
                AlertDialog.Builder(this)
                    .setTitle("Configuración incompleta")
                    .setMessage(
                        "No se encontró el Google Sheet ID. " +
                            "Agrega `googleSheetId=<ID>` a android-app/local.properties " +
                            "y recompila la app."
                    )
                    .setPositiveButton("Cerrar") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
                return
            }

            val progress = findViewById<ProgressBar>(R.id.progress)
            val editQuery = findViewById<EditText>(R.id.edit_query)
            // The catalog is shown directly in the main list, so no separate button is needed.
            val tvStatus = findViewById<TextView>(R.id.tv_status)
            val spinnerSort = findViewById<Spinner>(R.id.spinner_sort)
            val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
            val recycler = findViewById<RecyclerView>(R.id.recycler)

            recycler.layoutManager = LinearLayoutManager(this)
            adapter = BookAdapter(onItemClick = { showDetails(it) })
            recycler.adapter = adapter

            viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]

            viewModel.isLoading.observe(this) { loading ->
                progress.visibility = if (loading) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = loading
            }

            viewModel.error.observe(this) { error ->
                tvStatus.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
                tvStatus.text = error ?: ""
            }

            viewModel.lastUpdated.observe(this) { updated ->
                updated?.let {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "Última actualización: $it"
                }
            }

            viewModel.books.observe(this) { books ->
                allBooks = books
                refreshDisplay(editQuery.text.toString())
            }

            viewModel.colors.observe(this) { colors ->
                allColors = colors
                refreshDisplay(editQuery.text.toString())
            }

            ArrayAdapter.createFromResource(
                this,
                R.array.sort_options,
                android.R.layout.simple_spinner_item
            ).also { arrayAdapter ->
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSort.adapter = arrayAdapter
            }

            spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    sortMode = when (position) {
                        0 -> SortMode.LOCATION
                        1 -> SortMode.TITLE
                        2 -> SortMode.AUTHOR
                        3 -> SortMode.COLOR
                        else -> SortMode.LOCATION
                    }
                    refreshDisplay(editQuery.text.toString())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            editQuery.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    refreshDisplay(s?.toString().orEmpty())
                }
            })


            swipeRefresh.setOnRefreshListener {
                viewModel.refreshData()
            }

        } catch (t: Throwable) {
            AlertDialog.Builder(this)
                .setTitle("Error al iniciar")
                .setMessage(t.toString())
                .setPositiveButton("Cerrar") { _, _ -> finish() }
                .show()
        }
    }

    private fun refreshDisplay(query: String) {
        val trimmed = query.trim()
        val listToShow = if (trimmed.isEmpty()) {
            allBooks
        } else {
            val qn = Book.normalize(trimmed)
            allBooks.filter { book ->
                book.tituloNorm.contains(qn) ||
                    book.autorNorm.contains(qn) ||
                    book.colorNorm.contains(qn)
            }
        }

        adapter.updateData(sortBooks(listToShow), allColors)
    }

    private fun sortBooks(books: List<Book>): List<Book> {
        return when (sortMode) {
            SortMode.TITLE -> books.sortedBy { it.titulo.lowercase() }
            SortMode.AUTHOR -> books.sortedBy { it.autor.lowercase() }
            SortMode.LOCATION -> books.sortedWith(
                compareBy(
                    { it.balda ?: Int.MAX_VALUE },
                    { it.cuadrante.lowercase() },
                    { it.profundidadOrden },
                    { it.posicionRelativa ?: Int.MAX_VALUE },
                    { it.titulo.lowercase() }
                )
            )
            SortMode.COLOR -> books.sortedBy { it.colorLomo.lowercase() }
        }
    }

    private fun showDetails(book: Book) {
        val message = buildString {
            appendLine("Autor: ${book.autor}")
            appendLine("Color: ${book.colorLomo}")
            appendLine("Balda: ${book.balda ?: "-"}")
            appendLine("Cuadrante: ${book.cuadrante}")
            appendLine("Profundidad: ${book.profundidad}")
            appendLine("Posición: ${book.posicionRelativa ?: "-"}")
        }

        AlertDialog.Builder(this)
            .setTitle(book.titulo)
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showFullCatalog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_catalog, null)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recycler_catalog)

        recycler.layoutManager = LinearLayoutManager(this)
        val catalogAdapter = BookAdapter(onItemClick = { showDetails(it) })
        recycler.adapter = catalogAdapter

        catalogAdapter.updateData(allBooks, allColors)

        AlertDialog.Builder(this)
            .setTitle("📃 Catálogo completo")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}

