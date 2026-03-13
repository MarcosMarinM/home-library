package com.marcosmarin.libros.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marcosmarin.libros.model.Book
import java.io.File

object CacheManager {
    private const val BOOKS_FILE = "books_cache.json"
    private const val COLORS_FILE = "colors_cache.json"

    private val gson = Gson()

    fun getBooksFile(context: Context): File = File(context.filesDir, BOOKS_FILE)
    fun getColorsFile(context: Context): File = File(context.filesDir, COLORS_FILE)

    fun loadBooks(context: Context): List<Book>? {
        val file = getBooksFile(context)
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<List<Book>>() {}.type
            gson.fromJson<List<Book>>(file.readText(), type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveBooks(context: Context, books: List<Book>) {
        val file = getBooksFile(context)
        file.writeText(gson.toJson(books))
    }

    fun loadColors(context: Context): Map<String, String>? {
        val file = getColorsFile(context)
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(file.readText(), type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveColors(context: Context, colors: Map<String, String>) {
        val file = getColorsFile(context)
        file.writeText(gson.toJson(colors))
    }
}
