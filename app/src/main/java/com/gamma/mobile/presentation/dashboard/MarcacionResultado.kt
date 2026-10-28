package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.domain.model.AsistenciaMarcaResponse

data class MarcacionResultado(
    val mensaje: String,
    val esError: Boolean,
    val esInformativo: Boolean = false,
    val hora: String = "",
    val tipo: String = "",
    val planilla: String? = null,
    val registradoEnPlanilla: Boolean = false
)

/**
 * Valida la respuesta del servidor ante una marcación. Un HTTP 200 no basta: el mensaje puede
 * informar un rechazo (❌/⚠️), un aviso informativo (ℹ️) o faltar la hora registrada,
 * en cuyo caso NO se debe mostrar éxito.
 */
fun evaluarMarcacion(respuesta: AsistenciaMarcaResponse): MarcacionResultado {
    val mensaje = respuesta.mensaje.trim()
    val hora = respuesta.hora.trim()
    val tipo = respuesta.tipo.trim().lowercase()
    val planilla = respuesta.planilla?.trim()

    val mensajeLower = mensaje.lowercase()

    // 1. Detección de mensajes informativos neutrales (ej: día libre o registros completos)
    if (mensaje.startsWith("ℹ️") ||
        mensajeLower.contains("día libre") ||
        mensajeLower.contains("dia libre") ||
        mensajeLower.contains("ya completaste") ||
        mensajeLower.contains("ya completó") ||
        mensajeLower.contains("ya tiene salida") ||
        mensajeLower.contains("ya tiene entrada") ||
        mensajeLower.contains("no es posible marcar")
    ) {
        val textoConIcono = if (mensaje.startsWith("ℹ️")) mensaje else "ℹ️ $mensaje"
        return MarcacionResultado(
            mensaje = textoConIcono,
            esError = false,
            esInformativo = true,
            hora = hora,
            tipo = tipo,
            planilla = planilla,
            registradoEnPlanilla = false
        )
    }

    // 2. Detección de errores o rechazos
    val esMensajeError = mensaje.contains("❌") ||
        mensaje.contains("⚠️") ||
        tipo in listOf("error", "fallo", "falló", "rechazado") ||
        mensajeLower.contains("no se pudo") ||
        mensajeLower.contains("error") ||
        mensajeLower.contains("falló") ||
        mensajeLower.contains("fallo")

    if (esMensajeError) {
        val textoConIcono = if (mensaje.startsWith("❌") || mensaje.startsWith("⚠️")) mensaje else "❌ $mensaje"
        return MarcacionResultado(
            mensaje = textoConIcono,
            esError = true,
            esInformativo = false,
            hora = hora,
            tipo = tipo,
            planilla = planilla,
            registradoEnPlanilla = false
        )
    }

    // 3. Validación de hora
    if (hora.isEmpty()) {
        val detalle = if (mensaje.isNotEmpty()) mensaje else "El servidor no confirmó la marcación (sin hora registrada)"
        return MarcacionResultado(
            mensaje = "❌ $detalle",
            esError = true,
            esInformativo = false,
            hora = "",
            tipo = tipo,
            planilla = planilla,
            registradoEnPlanilla = false
        )
    }

    // 4. Éxito confirmado en planilla
    val base = if (mensaje.isEmpty()) "Marcación registrada" else mensaje
    val conIcono = if (base.startsWith("✅")) base else "✅ $base"
    val mensajeFinal = if (conIcono.contains("a las $hora")) conIcono else "$conIcono a las $hora"

    val mensajeConPlanilla = if (!planilla.isNullOrBlank() && !mensajeFinal.contains(planilla)) {
        "$mensajeFinal (Planilla: $planilla)"
    } else {
        mensajeFinal
    }

    return MarcacionResultado(
        mensaje = mensajeConPlanilla,
        esError = false,
        esInformativo = false,
        hora = hora,
        tipo = tipo,
        planilla = planilla,
        registradoEnPlanilla = true
    )
}
