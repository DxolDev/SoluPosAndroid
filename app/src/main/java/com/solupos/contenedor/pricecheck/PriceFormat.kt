package com.solupos.contenedor.pricecheck

import java.math.BigDecimal
import java.time.ZoneId
import java.util.Locale

/** Las ofertas se evalúan en hora de Guatemala, sin importar la zona del equipo. */
val GuatemalaZone: ZoneId = ZoneId.of("America/Guatemala")

fun formatQuetzales(value: BigDecimal): String = "Q" + String.format(Locale.US, "%,.2f", value)

data class LookupMessage(val title: String, val detail: String?)

/**
 * Texto para los resultados que no son un precio. En el quiosco el cliente no
 * ve jerga técnica ni el código; el detalle técnico queda para el cajero.
 */
fun LookupResult.message(kiosk: Boolean): LookupMessage? = when (this) {
    is LookupResult.Found -> null
    LookupResult.NotFound ->
        if (kiosk) LookupMessage("No encontramos este producto", "Pida ayuda a un vendedor")
        else LookupMessage(
            "Producto no encontrado",
            "Este código no está registrado en la tienda. Si el producto existe, falta asignarle el código en SoluPos."
        )
    LookupResult.Ambiguous ->
        if (kiosk) LookupMessage("No encontramos este producto", "Pida ayuda a un vendedor")
        else LookupMessage(
            "Código duplicado",
            "Este código coincide con más de un artículo, por eso no se muestra precio. Corríjalo en SoluPos."
        )
    LookupResult.Unrecognized ->
        if (kiosk) LookupMessage("No pudimos leer el código", "Intente de nuevo")
        else LookupMessage("Código no reconocido", "No tiene el formato de un código de producto.")
    LookupResult.NotConfigured ->
        if (kiosk) LookupMessage("Consultador no disponible", null)
        else LookupMessage("Consultador sin configurar", "Esta tienda no tiene token de consulta. Edita la tienda para agregarlo.")
    LookupResult.Unauthorized ->
        if (kiosk) LookupMessage("Consultador no disponible", null)
        else LookupMessage("Sin permiso", "El token de esta tienda no tiene permiso. Pide uno nuevo al administrador.")
    LookupResult.Offline -> LookupMessage("Sin conexión", "Intente de nuevo en unos segundos")
    is LookupResult.Failure -> LookupMessage("No se pudo consultar", "Intente de nuevo")
}
