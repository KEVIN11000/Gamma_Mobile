package com.gamma.mobile.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ComprobanteNotificationHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `buildPayload genera titulo, mensaje e id determinista para un periodo dado`() {
        val payload = ComprobanteNotificationHelper.buildPayload("  Octubre 2026  ")

        assertEquals("Octubre 2026".hashCode(), payload.id)
        assertEquals(ComprobanteNotificationHelper.CHANNEL_ID, payload.channelId)
        assertEquals("Comprobante PDF descargado", payload.title)
        assertEquals("El comprobante (Octubre 2026) está listo para visualizarse.", payload.message)
        assertEquals("Octubre 2026", payload.fileName)
    }

    @Test
    fun `buildPayload usa etiqueta por defecto cuando el texto es nulo o vacio`() {
        val payloadNull = ComprobanteNotificationHelper.buildPayload(null)
        assertEquals("Reporte PDF", payloadNull.fileName)
        assertEquals("El comprobante (Reporte PDF) está listo para visualizarse.", payloadNull.message)

        val payloadBlank = ComprobanteNotificationHelper.buildPayload("   ")
        assertEquals("Reporte PDF", payloadBlank.fileName)
        assertEquals("Reporte PDF".hashCode(), payloadBlank.id)
    }

    @Test
    fun `isValidPdfFile valida existencia y contenido no vacio`() {
        assertFalse(ComprobanteNotificationHelper.isValidPdfFile(null))

        val nonExistent = File(tempFolder.root, "no_existe.pdf")
        assertFalse(ComprobanteNotificationHelper.isValidPdfFile(nonExistent))

        val emptyFile = tempFolder.newFile("vacio.pdf")
        assertFalse(ComprobanteNotificationHelper.isValidPdfFile(emptyFile))

        val validFile = tempFolder.newFile("valido.pdf").apply {
            writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
        }
        assertTrue(ComprobanteNotificationHelper.isValidPdfFile(validFile))
    }
}
