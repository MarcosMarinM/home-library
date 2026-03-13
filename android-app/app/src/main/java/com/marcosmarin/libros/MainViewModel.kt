package com.marcosmarin.libros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marcosmarin.libros.cache.CacheManager
import com.marcosmarin.libros.data.SheetRepository
import com.marcosmarin.libros.model.Book
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: SheetRepository = SheetRepository()

    private val _books = MutableLiveData<List<Book>>(emptyList())
    val books: LiveData<List<Book>> = _books

    private val _colors = MutableLiveData<Map<String, String>>(emptyMap())
    val colors: LiveData<Map<String, String>> = _colors

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _lastUpdated = MutableLiveData<String?>(null)
    val lastUpdated: LiveData<String?> = _lastUpdated

    init {
        loadCachedData()
        refreshData()
    }

    private fun loadCachedData() {
        val cachedBooks = CacheManager.loadBooks(getApplication())
        val cachedColors = CacheManager.loadColors(getApplication())

        if (!cachedBooks.isNullOrEmpty()) {
            _books.value = cachedBooks
        }
        if (!cachedColors.isNullOrEmpty()) {
            _colors.value = cachedColors
        }
    }

    fun refreshData() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val colors = repository.loadColorDictionary()
                val books = repository.loadBooks()

                _colors.value = colors
                _books.value = books

                CacheManager.saveColors(getApplication(), colors)
                CacheManager.saveBooks(getApplication(), books)

                _lastUpdated.value = DateFormat.getDateTimeInstance().format(Date())
            } catch (t: Throwable) {
                _error.value = t.message ?: "Error desconocido"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
