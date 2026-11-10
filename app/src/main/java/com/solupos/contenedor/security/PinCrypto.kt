package com.solupos.contenedor.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Funciones puras del PIN del quiosco: hash con sal y tiempo de bloqueo. */
object PinCrypto {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isValidFormat(pin: String) = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS))
            .encoded

    /** Comparación en tiempo constante. */
    fun matches(expected: ByteArray, actual: ByteArray): Boolean = MessageDigest.isEqual(expected, actual)

    /** Hasta 4 fallos no hay espera; desde el 5.º, 30 s duplicándose, con tope de 15 min. */
    fun lockSeconds(failures: Int): Long {
        if (failures < 5) return 0
        val doublings = (failures - 5).coerceAtMost(5)
        return (30L shl doublings).coerceAtMost(15 * 60L)
    }
}
