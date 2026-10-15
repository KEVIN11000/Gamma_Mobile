package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class HealthStatus(
    @SerializedName("status")
    val status: String = "ok", // "ok" o "degraded"
    @SerializedName("sheets")
    val sheets: Boolean = true,
    @SerializedName("avisos")
    val avisos: Boolean = true,
    @SerializedName("version_api")
    val versionApi: String = "1.0.0",
    @SerializedName("latest_app_version")
    val latestAppVersion: String? = null,
    @SerializedName("latest_app_version_code")
    val latestAppVersionCode: Int? = null,
    @SerializedName("download_url")
    val downloadUrl: String? = null
)

data class TicketCreate(
    @SerializedName("asunto")
    val asunto: String,
    @SerializedName("descripcion")
    val descripcion: String,
    @SerializedName("origen")
    val origen: String = "app"
)

data class TicketUpdate(
    @SerializedName("estado")
    val estado: String,
    @SerializedName("resolucion")
    val resolucion: String? = null
)

data class TicketItem(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("asunto")
    val asunto: String = "",
    @SerializedName("descripcion")
    val descripcion: String = "",
    @SerializedName("estado")
    val estado: String = "Abierto", // "Abierto", "En proceso", "Cerrado"
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName("origen")
    val origen: String = "app", // "app", "telegram", "ci", "servidor"
    @SerializedName("resolucion")
    val resolucion: String? = null
)

data class LogEntry(
    @SerializedName("timestamp")
    val timestamp: String = "",
    @SerializedName("nivel")
    val nivel: String = "INFO",
    @SerializedName("mensaje")
    val mensaje: String = ""
)
