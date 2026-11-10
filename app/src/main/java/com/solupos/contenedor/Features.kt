package com.solupos.contenedor

/**
 * Interruptores de funciones. El consultador de precios depende de un proxy que
 * todavía no existe, así que la versión de tienda lo oculta por completo.
 * Cuando el proxy esté en producción, cambiar [priceCheck] a `true`.
 */
object Features {
    val priceCheck: Boolean = BuildConfig.DEBUG
}
