package com.solupos.contenedor.pricecheck

import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Catálogo de ejemplo que pasa por el mismo [LookupResolver] que usará el
 * cliente real. Códigos para probar: 806 (oferta), 0000000187 (id generado),
 * 011418218703 (UPC-A agotado), 011418218700 (duplicado: sus dos variantes
 * apuntan a artículos distintos), cualquier otro (no encontrado).
 */
class DemoPriceLookupService : PriceLookupService {
    private val catalog = listOf(
        PriceInfo(1, "806", "Abrazadera con llave para bebedero campana", BigDecimal("20.00"),
            BigDecimal("15.00"), null, LocalDate.now().plusDays(9), BigDecimal("490")),
        PriceInfo(187, null, "\tMaíz amarillo  quintal ", BigDecimal("187.50"), quantity = BigDecimal("12")),
        PriceInfo(184, "011418218703", "Tabcin Gripe y Tos", BigDecimal("10.00"), quantity = BigDecimal("-1")),
        PriceInfo(5, "011418218700", "Producto repetido A", BigDecimal("5.00"), quantity = BigDecimal("3")),
        PriceInfo(6, "0011418218700", "Producto repetido B", BigDecimal("9.00"), quantity = BigDecimal("3"))
    )

    override suspend fun lookup(storeId: String, query: CodeQuery): LookupResult {
        delay(400)
        return LookupResolver.resolve(query, catalog)
    }
}
