package com.solupos.contenedor.pricecheck

/** Versión de desarrollo: catálogo de demostración para ver las pantallas sin proxy. */
object PriceLookupProvider {
    fun create(): PriceLookupService = DemoPriceLookupService()
}
