package com.solupos.contenedor.pricecheck

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeInterpreterTest {
    @Test fun etiquetaGeneradaSeConvierteEnId() =
        assertEquals(CodeQuery.ById(187), CodeInterpreter.interpret("0000000187"))

    @Test fun etiquetaGeneradaConEspaciosSeLimpia() =
        assertEquals(CodeQuery.ById(184), CodeInterpreter.interpret(" 0000000184\n"))

    @Test fun diezCerosNoEsUnId() =
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret("0000000000"))

    @Test fun upcA_seProbaTambienComoEan13() =
        assertEquals(
            CodeQuery.ByNumber(listOf("011418218703", "0011418218703")),
            CodeInterpreter.interpret("011418218703")
        )

    @Test fun ean13ConCeroInicialSeProbaSinElCero() =
        assertEquals(
            CodeQuery.ByNumber(listOf("0011418218703", "011418218703")),
            CodeInterpreter.interpret("0011418218703")
        )

    @Test fun ean13NormalSeBuscaTalCual() =
        assertEquals(CodeQuery.ByNumber(listOf("7501055300631")), CodeInterpreter.interpret("7501055300631"))

    @Test fun codigoInternoCorto() =
        assertEquals(CodeQuery.ByNumber(listOf("806")), CodeInterpreter.interpret("806"))

    @Test fun diezDigitosSinCeroInicialEsNumero() =
        assertEquals(CodeQuery.ByNumber(listOf("1234567890")), CodeInterpreter.interpret("1234567890"))

    @Test fun urlYTextoNoSeReconocen() {
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret("https://solupos.com/x"))
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret("ABC123"))
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret(""))
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret(null))
    }

    @Test fun demasiadoLargoNoSeReconoce() =
        assertEquals(CodeQuery.Unrecognized, CodeInterpreter.interpret("123456789012345"))
}
