package com.gamma.mobile.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParseSifenQrUseCaseTest {

    private lateinit var useCase: ParseSifenQrUseCase

    @Before
    fun setUp() {
        useCase = ParseSifenQrUseCase()
    }

    @Test
    fun parseValidEkuatiaUrl_extractsAllFieldsCorrectly() {
        val cdc = "01080012345001001000123412026101411234567895"
        val url = "https://ekuatia.set.gov.py/consultas/qr?nVersion=150&Id=$cdc&dFeEmiDE=2026-10-14&dTotGralOpe=1500000&dTotIVA=136364"

        val result = useCase(url)

        assertTrue("El resultado debe ser exitoso", result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals(cdc, data?.cdc)
        assertEquals("8001234-5", data?.rucEmisor)
        assertEquals("001-001-0001234", data?.nroFactura)
        assertEquals("2026-10-14", data?.fechaEmision)
        assertEquals(1500000L, data?.total)
        assertEquals(136364L, data?.iva10)
        assertEquals(0L, data?.iva5)
    }

    @Test
    fun parseUrlWithHexEncodedDate_decodesDateCorrectly() {
        val cdc = "01080012345001001000123412026101411234567895"
        // "2026-10-14" encoded in hex: 323032362d31302d3134
        val hexDate = "323032362d31302d3134"
        val url = "https://sifen.set.gov.py/de/consulta-qr?Id=$cdc&dFeEmiDE=$hexDate&dTotGralOpe=500000&dIVA10=45455"

        val result = useCase(url)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("2026-10-14", data?.fechaEmision)
        assertEquals(500000L, data?.total)
        assertEquals(45455L, data?.iva10)
    }

    @Test
    fun parseUrlWithoutIva_calculatesStandardTenPercentIva() {
        val cdc = "01080012345001001000123412026101411234567895"
        val url = "https://ekuatia.set.gov.py/consultas/qr?Id=$cdc&dTotGralOpe=1100000"

        val result = useCase(url)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals(1100000L, data?.total)
        assertEquals(100000L, data?.iva10) // 1.100.000 / 11 = 100.000
    }

    @Test
    fun parseBareCdc_extractsMetadataFromCdcItself() {
        val cdc = "01080012345001001000123412026101411234567895"

        val result = useCase(cdc)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals(cdc, data?.cdc)
        assertEquals("8001234-5", data?.rucEmisor)
        assertEquals("001-001-0001234", data?.nroFactura)
        assertEquals("2026-10-14", data?.fechaEmision)
        assertEquals(0L, data?.total)
    }

    @Test
    fun rejectEmptyOrInvalidQrCode_returnsFailure() {
        val resultEmpty = useCase("")
        assertTrue(resultEmpty.isFailure)

        val resultInvalidUrl = useCase("https://google.com/search?q=factura")
        assertTrue(resultInvalidUrl.isFailure)

        val resultShortCdc = useCase("1234567890")
        assertTrue(resultShortCdc.isFailure)
    }
}
