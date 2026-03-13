package com.marcosmarin.libros.model

/** Represents a single row in the `libros` sheet. */
data class Book(
    val titulo: String,
    val autor: String,
    val colorLomo: String,
    val balda: Int?,
    val cuadrante: String,
    val profundidad: String,
    val posicionRelativa: Int?,

    // Normalized versions, precomputed to speed up searching.
    val tituloNorm: String,
    val autorNorm: String,
    val colorNorm: String,
    val profundidadOrden: Int,
) {
    companion object {
        /**
         * Order used by the Shiny app: Delante, Detrás, Encima, Debajo, Completa.
         */
        private val depthOrder = listOf("Delante", "Detrás", "Encima", "Debajo", "Completa")

        fun fromRow(row: Map<String, String?>, colorDict: Map<String, String>): Book {
            val titulo = row["Título"].orEmpty().trim()
            val autor = row["Autor"].orEmpty().trim()
            val colorLomo = row["Color_Lomo"].orEmpty().trim()
            val balda = row["Balda"]?.toIntOrNull()
            val cuadrante = row["Cuadrante"].orEmpty().trim()
            val profundidadRaw = row["Profundidad"].orEmpty().trim()
            val posicionRelativa = row["Posicion_Relativa"]?.toIntOrNull()

            val profundidad = interpretarProfundidad(profundidadRaw)
            val profundidadOrden = depthOrder.indexOf(profundidad).takeIf { it >= 0 } ?: Int.MAX_VALUE

            return Book(
                titulo = titulo,
                autor = autor,
                colorLomo = colorLomo,
                balda = balda,
                cuadrante = cuadrante,
                profundidad = profundidad,
                posicionRelativa = posicionRelativa,
                tituloNorm = normalize(titulo),
                autorNorm = normalize(autor),
                colorNorm = normalize(colorLomo),
                profundidadOrden = profundidadOrden,
            )
        }

        fun normalize(input: String): String {
            val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()

            return normalized
                .replace("\\u00A0", " ")
                .replace("[^a-z0-9 ]+".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
        }

        private fun interpretarProfundidad(input: String): String {
            val xx = normalize(input)
            return when (xx) {
                "f", "frente", "delante" -> "Delante"
                "t", "tras", "detras", "detrás" -> "Detrás"
                "b", "debajo" -> "Debajo"
                "a", "encima" -> "Encima"
                "c", "completa" -> "Completa"
                else -> input
            }
        }
    }
}
