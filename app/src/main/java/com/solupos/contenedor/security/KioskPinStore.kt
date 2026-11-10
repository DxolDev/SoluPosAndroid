package com.solupos.contenedor.security

import android.content.Context
import android.util.Base64

sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val attemptsBeforeLock: Int) : PinCheck
    data class Locked(val seconds: Long) : PinCheck
}

/** PIN único del equipo para salir del modo quiosco. Solo se guarda su hash. */
class KioskPinStore(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val prefs = context.getSharedPreferences("kiosk_pin", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        require(PinCrypto.isValidFormat(pin)) { "PIN inválido" }
        val salt = PinCrypto.newSalt()
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(PinCrypto.hash(pin, salt), Base64.NO_WRAP))
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0)
            .apply()
    }

    fun lockedSeconds(): Long = ((prefs.getLong(KEY_LOCKED_UNTIL, 0) - now()) / 1000).coerceAtLeast(0)

    fun verify(pin: String): PinCheck {
        val remaining = lockedSeconds()
        if (remaining > 0) return PinCheck.Locked(remaining)
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        val hash = prefs.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (salt == null || hash == null) return PinCheck.Wrong(0)

        if (PinCrypto.matches(hash, PinCrypto.hash(pin, salt))) {
            prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCKED_UNTIL, 0).apply()
            return PinCheck.Ok
        }
        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        val lock = PinCrypto.lockSeconds(fails)
        prefs.edit()
            .putInt(KEY_FAILS, fails)
            .putLong(KEY_LOCKED_UNTIL, if (lock > 0) now() + lock * 1000 else 0)
            .apply()
        return if (lock > 0) PinCheck.Locked(lock) else PinCheck.Wrong(4 - fails + 1)
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_FAILS = "fails"
        const val KEY_LOCKED_UNTIL = "locked_until"
    }
}
