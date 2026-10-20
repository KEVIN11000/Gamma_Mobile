package com.gamma.mobile.utils

import com.gamma.mobile.domain.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogBuffer {

    private const val MAX_LOGS = 100
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private val lock = Any()

    fun log(nivel: String, mensaje: String) {
        val timestamp = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(timestamp = timestamp, nivel = nivel.uppercase(), mensaje = mensaje)
        synchronized(lock) {
            val current = _logsFlow.value.toMutableList()
            current.add(0, entry) // más recientes primero
            if (current.size > MAX_LOGS) {
                _logsFlow.value = current.take(MAX_LOGS)
            } else {
                _logsFlow.value = current
            }
        }
    }

    fun info(mensaje: String) = log("INFO", mensaje)
    fun warn(mensaje: String) = log("WARN", mensaje)
    fun error(mensaje: String) = log("ERROR", mensaje)

    fun getLogs(): List<LogEntry> = _logsFlow.value

    fun clear() {
        synchronized(lock) {
            _logsFlow.value = emptyList()
        }
    }
}
