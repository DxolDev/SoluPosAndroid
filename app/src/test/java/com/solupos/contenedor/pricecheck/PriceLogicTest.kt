package com.solupos.contenedor.pricecheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PriceLogicTest {
    private val today = LocalDate.of(2026, 11, 6)

    private fun item(
        id: Long = 1, number: String? = "806", price: String = "20.00",
        promo: String? = null, start: LocalDate? = null, end: LocalDate? = null, qty: String? = "5"
    ) = PriceInfo(id, number, "  Abrazadera\tcon   llave ", BigDecimal(price),
        promo?.let(::BigDecimal), start, end, qty?.let(::BigDecimal))

    @Test fun nombreSeLimpia() = assertEquals("Abrazadera con llave", item().displayName)

    @Test fun ofertaVigenteSeUsa() =
        assertEquals(BigDecimal("15.00"), item(promo = "15.00", end = today.plusDays(3)).finalPrice(today))

    @Test fun ofertaVencidaSeIgnora() =
        assertEquals(BigDecimal("20.00"), item(promo = "15.00", end = today.minusDays(1)).finalPrice(today))

    @Test fun ofertaQueAunNoEmpiezaSeIgnora() =
        assertNull(item(promo = "15.00", start = today.plusDays(1)).activePromo(today))

    @Test fun ofertaSinFechasSeConsideraVigente() =
        assertEquals(BigDecimal("15.00"), item(promo = "15.00").activePromo(today))

    @Test fun ofertaMayorOIgualAlPrecioSeIgnora() {
        assertNull(item(promo = "20.00").activePromo(today))
        assertNull(item(promo = "0.00").activePromo(today))
    }

    @Test fun existencia() {
        assertEquals(true, item(qty = "490").available)
        assertEquals(false, item(qty = "0").available)
        assertEquals(false, item(qty = "-1").available)
        assertNull(item(qty = null).available)
    }

    // --- Resolución de coincidencias ---
    @Test fun porNumeroExacto() {
        val r = LookupResolver.resolve(
            CodeQuery.ByNumber(listOf("806")),
            listOf(item(id = 1, number = "806"), item(id = 2, number = "8061"))
        )
        assertTrue(r is LookupResult.Found && r.info.itemId == 1L)
    }

    @Test fun searchTrajoSoloCoincidenciasParciales() =
        assertEquals(LookupResult.NotFound,
            LookupResolver.resolve(CodeQuery.ByNumber(listOf("806")), listOf(item(id = 9, number = "8061"))))

    @Test fun upcPorVariante() {
        val r = LookupResolver.resolve(
            CodeQuery.ByNumber(listOf("011418218703", "0011418218703")),
            listOf(item(id = 184, number = "011418218703"))
        )
        assertTrue(r is LookupResult.Found && r.info.itemId == 184L)
    }

    @Test fun porIdExacto() {
        val r = LookupResolver.resolve(CodeQuery.ById(187), listOf(item(id = 187, number = null)))
        assertTrue(r is LookupResult.Found && r.info.itemId == 187L)
    }

    @Test fun idQueNoCoincideEsNoEncontrado() =
        assertEquals(LookupResult.NotFound,
            LookupResolver.resolve(CodeQuery.ById(187), listOf(item(id = 188))))

    @Test fun dosArticulosDistintosEsAmbiguo() =
        assertEquals(LookupResult.Ambiguous,
            LookupResolver.resolve(CodeQuery.ByNumber(listOf("806", "0806")),
                listOf(item(id = 1, number = "806"), item(id = 2, number = "0806"))))

    @Test fun elMismoArticuloRepetidoNoEsAmbiguo() {
        val r = LookupResolver.resolve(CodeQuery.ByNumber(listOf("806")),
            listOf(item(id = 1), item(id = 1)))
        assertFalse(r is LookupResult.Ambiguous)
    }

    @Test fun sinListaEsNoEncontrado() =
        assertEquals(LookupResult.NotFound, LookupResolver.resolve(CodeQuery.ById(1), emptyList()))

    @Test fun codigoNoReconocido() =
        assertEquals(LookupResult.Unrecognized, LookupResolver.resolve(CodeQuery.Unrecognized, emptyList()))
}
