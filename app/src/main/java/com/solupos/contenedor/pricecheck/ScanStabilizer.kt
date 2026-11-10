package com.solupos.contenedor.pricecheck

/**
 * Confirma una lectura de cámara. Los códigos cortos (3 dígitos) se confunden
 * fácil, así que se exige ver el mismo valor en varios fotogramas seguidos, y
 * luego se ignora el mismo código unos segundos para no repetir la consulta.
 */
class ScanStabilizer(
    private val requiredHits: Int = 2,
    private val cooldownMs: Long = 3_000,
    private val candidateWindowMs: Long = 1_000,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var candidate: String? = null
    private var hits = 0
    private var candidateAt = 0L
    private var lastEmitted: String? = null
    private var lastEmitAt = 0L

    /** Devuelve el código cuando queda confirmado; si no, null. */
    fun offer(code: String?): String? {
        if (code.isNullOrEmpty()) return null
        val now = clock()
        if (code == lastEmitted && now - lastEmitAt < cooldownMs) return null

        if (code == candidate && now - candidateAt <= candidateWindowMs) {
            hits++
        } else {
            candidate = code
            hits = 1
        }
        candidateAt = now
        if (hits < requiredHits) return null

        lastEmitted = code
        lastEmitAt = now
        candidate = null
        hits = 0
        return code
    }

    fun reset() {
        candidate = null
        hits = 0
        lastEmitted = null
    }
}
