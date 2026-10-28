package com.gamma.mobile.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarcacionNotificationHelperTest {

    @Test
    fun `buildPayload generates valid notification content for normal marcacion`() {
        val payload = MarcacionNotificationHelper.buildPayload(
            hora = "08:15",
            tipo = "entrada",
            planilla = "Octubre 2026"
        )

        assertEquals("Marcación Registrada", payload.title)
        assertTrue(payload.message.contains("Entrada registrado a las 08:15"))
        assertTrue(payload.message.contains("Octubre 2026"))
        assertEquals(MarcacionNotificationHelper.CHANNEL_ID, payload.channelId)
    }

    @Test
    fun `buildPayload with null planilla uses fallback text`() {
        val payload = MarcacionNotificationHelper.buildPayload(
            hora = "17:30",
            tipo = "salida",
            planilla = null
        )

        assertEquals("Marcación Registrada", payload.title)
        assertTrue(payload.message.contains("Salida registrado a las 17:30"))
        assertTrue(payload.message.contains("en la planilla de horas"))
    }
}
