package com.solupos.contenedor.pricecheck

/** Versión de tienda: sin datos de demostración. Se reemplaza por el cliente del proxy. */
object PriceLookupProvider {
    fun create(): PriceLookupService = NotConfiguredPriceLookupService()
}
