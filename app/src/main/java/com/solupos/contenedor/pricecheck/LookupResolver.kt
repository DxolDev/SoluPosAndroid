package com.solupos.contenedor.pricecheck

/**
 * Decide qué artículo corresponde a un código entre los resultados que devolvió
 * la búsqueda. `search` también coincide con nombres, por eso se exige igualdad
 * exacta. Ante la duda se prefiere no mostrar precio a mostrar uno equivocado.
 */
object LookupResolver {
    fun resolve(query: CodeQuery, candidates: List<PriceInfo>): LookupResult {
        val matches = when (query) {
            is CodeQuery.ById -> candidates.filter { it.itemId == query.id }
            is CodeQuery.ByNumber -> candidates.filter { it.itemNumber in query.variants }
            CodeQuery.Unrecognized -> return LookupResult.Unrecognized
        }.distinctBy { it.itemId }

        return when (matches.size) {
            0 -> LookupResult.NotFound
            1 -> LookupResult.Found(matches.single())
            else -> LookupResult.Ambiguous
        }
    }
}
