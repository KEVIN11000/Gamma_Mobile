package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.domain.model.AsistenciaMarcaResponse

data class MarcacionResultado(val mensaje: String, val esError: Boolean)

/**
 * Valida la respuesta del servidor ante una marcación. Un HTTP 200 no basta: el mensaje puede
 * informar un rechazo (❌/⚠️) o faltar la hora registrada, en cuyo caso NO se debe mostrar éxito.
 */
fun evaluarMarcacion(respuesta: AsistenciaMarcaResponse): MarcacionResultado {
    val mensaje = respuesta.mensaje.trim()
    val hora = respuesta.hora.trim()
    val tipo = respuesta.tipo.trim().lowercase()

    val mensajeLower = mensaje.lowercase()
    val esMensajeError = mensaje.contains("❌") ||
        mensaje.contains("⚠️") ||
        tipo in listOf("error", "fallo", "falló", "rechazado") ||
        mensajeLower.contains("no se pudo") ||
        mensajeLower.contains("error") ||
        mensajeLower.contains("falló") ||
        mensajeLower.contains("fallo")

    if (esMensajeError) {
        val textoConIcono = if (mensaje.startsWith("❌") || mensaje.startsWith("⚠️")) mensaje else "❌ $mensaje"
        return MarcacionResultado(textoConIcono, esError = true)
    }

    if (hora.isEmpty()) {
        val detalle = if (mensaje.isNotEmpty()) mensaje else "El servidor no confirmó la marcación (sin hora registrada)"
        return MarcacionResultado("❌ $detalle", esError = true)
    }

    val base = if (mensaje.isEmpty()) "Marcación registrada" else mensaje
    val conIcono = if (base.startsWith("✅")) base else "✅ $base"
    return MarcacionResultado("$conIcono a las $hora", esError = false)
}
