package com.solupos.contenedor.pricecheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ScanAndFormatTest {
    private var now = 0L
    private fun stabilizer() = ScanStabilizer(clock = { now })

    @Test fun unaSolaLecturaNoBasta() = assertNull(stabilizer().offer("806"))

    @Test fun dosLecturasIgualesConfirman() {
        val s = stabilizer()
        assertNull(s.offer("806"))
        now += 100
        assertEquals("806", s.offer("806"))
    }

    @Test fun lecturasDistintasNoSeSuman() {
        val s = stabilizer()
        s.offer("806"); now += 100
        assertNull(s.offer("807")); now += 100
        assertNull(s.offer("806"))
    }

    @Test fun lecturaVencidaNoCuenta() {
        val s = stabilizer()
        s.offer("806"); now += 5_000
        assertNull(s.offer("806"))
    }

    @Test fun elMismoCodigoEnfriaTresSegundos() {
        val s = stabilizer()
        s.offer("806"); now += 100; s.offer("806")
        now += 200
        assertNull(s.offer("806")); now += 100
        assertNull(s.offer("806"))
        now += 4_000
        s.offer("806"); now += 100
        assertNotNull(s.offer("806"))
    }

    @Test fun otroCodigoNoEsperaElEnfriamiento() {
        val s = stabilizer()
        s.offer("806"); now += 100; s.offer("806"); now += 100
        s.offer("659"); now += 100
        assertEquals("659", s.offer("659"))
    }

    @Test fun nulosYVaciosSeIgnoran() {
        val s = stabilizer()
        assertNull(s.offer(null)); assertNull(s.offer(""))
    }

    @Test fun formatoQuetzales() {
        assertEquals("Q20.00", formatQuetzales(BigDecimal("20")))
        assertEquals("Q187.50", formatQuetzales(BigDecimal("187.5")))
        assertEquals("Q1,234.50", formatQuetzales(BigDecimal("1234.5")))
        assertEquals("Q0.99", formatQuetzales(BigDecimal("0.987")))
    }

    @Test fun quioscoNoMuestraJergaNiCodigo() {
        listOf(LookupResult.NotFound, LookupResult.Ambiguous, LookupResult.NotConfigured, LookupResult.Unauthorized)
            .forEach { r ->
                val m = r.message(kiosk = true)!!
                val text = (m.title + " " + (m.detail ?: "")).lowercase()
                listOf("token", "solupos", "código", "api", "item").forEach { bad ->
                    assert(!text.contains(bad)) { "$r muestra '$bad'" }
                }
            }
    }

    @Test fun encontradoNoTieneMensaje() =
        assertNull(LookupResult.Found(PriceInfo(1, null, "x", BigDecimal.ONE)).message(kiosk = false))
}
