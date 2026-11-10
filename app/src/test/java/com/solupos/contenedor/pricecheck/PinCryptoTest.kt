package com.solupos.contenedor.pricecheck

import com.solupos.contenedor.security.PinCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCryptoTest {
    @Test fun formatoDelPin() {
        assertTrue(PinCrypto.isValidFormat("1234"))
        assertTrue(PinCrypto.isValidFormat("12345678"))
        assertFalse(PinCrypto.isValidFormat("123"))
        assertFalse(PinCrypto.isValidFormat("123456789"))
        assertFalse(PinCrypto.isValidFormat("12a4"))
        assertFalse(PinCrypto.isValidFormat(""))
    }

    @Test fun elHashCoincideSoloConElMismoPinYSal() {
        val salt = PinCrypto.newSalt()
        val h = PinCrypto.hash("4821", salt, iterations = 1000)
        assertTrue(PinCrypto.matches(h, PinCrypto.hash("4821", salt, iterations = 1000)))
        assertFalse(PinCrypto.matches(h, PinCrypto.hash("4822", salt, iterations = 1000)))
        assertFalse(PinCrypto.matches(h, PinCrypto.hash("4821", PinCrypto.newSalt(), iterations = 1000)))
    }

    @Test fun lasSalesSonDistintas() =
        assertNotEquals(PinCrypto.newSalt().toList(), PinCrypto.newSalt().toList())

    @Test fun bloqueoProgresivo() {
        assertEquals(0, PinCrypto.lockSeconds(0))
        assertEquals(0, PinCrypto.lockSeconds(4))
        assertEquals(30, PinCrypto.lockSeconds(5))
        assertEquals(60, PinCrypto.lockSeconds(6))
        assertEquals(120, PinCrypto.lockSeconds(7))
        assertEquals(900, PinCrypto.lockSeconds(10))
        assertEquals(900, PinCrypto.lockSeconds(500))
    }
}
