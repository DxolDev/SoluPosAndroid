package com.solupos.contenedor.pricecheck

/** Cómo debe buscarse un código leído por la cámara o por un lector. */
sealed interface CodeQuery {
    /** Código generado por SoluPos a partir del `item_id` (10 dígitos con ceros a la izquierda). */
    data class ById(val id: Long) : CodeQuery

    /** Código guardado en `item_number`; se prueba cada variante, en orden. */
    data class ByNumber(val variants: List<String>) : CodeQuery

    /** No tiene forma de código de producto (URL, texto, vacío). */
    data object Unrecognized : CodeQuery
}

object CodeInterpreter {
    private const val GENERATED_ID_LENGTH = 10
    private const val MAX_NUMBER_LENGTH = 14

    fun interpret(raw: String?): CodeQuery {
        val code = raw?.trim().orEmpty()
        if (code.isEmpty() || !code.all { it in '0'..'9' }) return CodeQuery.Unrecognized

        // Etiqueta generada por SoluPos: 0000000187 -> id 187.
        if (code.length == GENERATED_ID_LENGTH && code.startsWith("0")) {
            val digits = code.trimStart('0')
            return if (digits.isEmpty()) CodeQuery.Unrecognized
            else CodeQuery.ById(digits.toLong())
        }

        return when {
            // La cámara puede devolver un UPC-A como EAN-13 con un 0 delante, y al revés.
            code.length == 12 -> CodeQuery.ByNumber(listOf(code, "0$code"))
            code.length == 13 && code.startsWith("0") -> CodeQuery.ByNumber(listOf(code, code.drop(1)))
            code.length in 1..MAX_NUMBER_LENGTH -> CodeQuery.ByNumber(listOf(code))
            else -> CodeQuery.Unrecognized
        }
    }
}
