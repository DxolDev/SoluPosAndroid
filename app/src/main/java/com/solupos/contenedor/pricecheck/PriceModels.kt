package com.solupos.contenedor.pricecheck

import java.math.BigDecimal
import java.time.LocalDate

/** Lo único que la app necesita saber de un producto; nada de costos ni proveedores. */
data class PriceInfo(
    val itemId: Long,
    val itemNumber: String?,
    val name: String,
    val regularPrice: BigDecimal,
    val promoPrice: BigDecimal? = null,
    val promoStart: LocalDate? = null,
    val promoEnd: LocalDate? = null,
    /** Existencia total; null si no se conoce. */
    val quantity: BigDecimal? = null
) {
    /** Nombre sin tabulaciones ni espacios sobrantes (la API los trae a veces). */
    val displayName: String get() = name.replace(Regex("\\s+"), " ").trim()

    /** La oferta solo cuenta si hoy está dentro de su rango; sin fechas se considera vigente. */
    fun activePromo(today: LocalDate): BigDecimal? {
        val promo = promoPrice ?: return null
        if (promo.signum() <= 0 || promo >= regularPrice) return null
        if (promoStart != null && today.isBefore(promoStart)) return null
        if (promoEnd != null && today.isAfter(promoEnd)) return null
        return promo
    }

    fun finalPrice(today: LocalDate): BigDecimal = activePromo(today) ?: regularPrice

    /** null = no se sabe; cero o negativo (se ha visto -1) = agotado. */
    val available: Boolean? get() = quantity?.let { it.signum() > 0 }
}

sealed interface LookupResult {
    data class Found(val info: PriceInfo) : LookupResult
    data object NotFound : LookupResult
    /** El código coincide con dos artículos distintos: no se muestra ningún precio. */
    data object Ambiguous : LookupResult
    data object Unrecognized : LookupResult
    data object NotConfigured : LookupResult
    data object Unauthorized : LookupResult
    data object Offline : LookupResult
    data class Failure(val message: String) : LookupResult
}

/** Punto de unión con el proxy. La pantalla solo conoce esta interfaz. */
interface PriceLookupService {
    suspend fun lookup(storeId: String, query: CodeQuery): LookupResult
}

/** Sustituto hasta que exista el proxy: indica que la tienda no está lista. */
class NotConfiguredPriceLookupService : PriceLookupService {
    override suspend fun lookup(storeId: String, query: CodeQuery): LookupResult =
        LookupResult.NotConfigured
}
