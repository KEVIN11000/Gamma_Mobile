package com.gamma.mobile.presentation.sistema

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SistemaScreen(
    viewModel: SistemaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    var showNewTicketDialog by remember { mutableStateOf(false) }
    var isTicketsExpanded by remember { mutableStateOf(false) }
    var selectedTicketForAction by remember { mutableStateOf<com.gamma.mobile.domain.model.TicketItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sistema y Soporte", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.cargarDatosSistema() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar sistema")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Versión de la Aplicación y Estado de Actualizaciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gamma Mobile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Versión instalada: ${uiState.appVersion.ifEmpty { "0.0.5+debug" }}", fontWeight = FontWeight.SemiBold)
                                Text("Entorno: Android (minSdk 26, targetSdk 34)", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        when (val status = uiState.updateStatus) {
                            is com.gamma.mobile.domain.model.UpdateStatus.UpdateAvailable -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "¡Nueva versión ${status.version} disponible!",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            "Paquete: ${status.assetName} (${status.sizeMb} MB)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(
                                            onClick = {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(status.downloadUrl)
                                                )
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Descargar actualización (${status.version})")
                                        }
                                    }
                                }
                            }
                            is com.gamma.mobile.domain.model.UpdateStatus.UpToDate -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Versión al día con GitHub Releases", fontWeight = FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                val fallbackUrl = uiState.health?.downloadUrl
                                if (!fallbackUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(fallbackUrl)
                                            )
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Descargar actualización oficial (APK)")
                                    }
                                }
                            }
                        }
                    }
                }

                // Card: Configuración de Conexión y API Key
                var serverUrlInput by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }
                var apiKeyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
                var showKeyText by remember { mutableStateOf(false) }
                var isConfigLocked by remember(uiState.isApiKeyConfigured) { mutableStateOf(uiState.isApiKeyConfigured) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isConfigLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isConfigLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Conexión y API Key",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (uiState.isApiKeyConfigured) "CONFIGURADA" else "PENDIENTE") },
                                    leadingIcon = {
                                        Icon(
                                            if (uiState.isApiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (uiState.isApiKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                                IconButton(
                                    onClick = { isConfigLocked = !isConfigLocked }
                                ) {
                                    Icon(
                                        imageVector = if (isConfigLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = if (isConfigLocked) "Desbloquear edición" else "Bloquear edición",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("URL del Servidor Backend") },
                            readOnly = isConfigLocked,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("API Key (X-API-KEY)") },
                            readOnly = isConfigLocked,
                            singleLine = true,
                            visualTransformation = if (showKeyText) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showKeyText = !showKeyText }) {
                                    Text(if (showKeyText) "Ocultar" else "Ver")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isConfigLocked) {
                            FilledTonalButton(
                                onClick = { isConfigLocked = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Desbloquear para Editar")
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.guardarConfiguracion(serverUrlInput, apiKeyInput)
                                    isConfigLocked = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar y Probar Conexión")
                            }
                        }

                        if (!uiState.configMessage.isNullOrBlank()) {
                            val msg = uiState.configMessage!!
                            val msgColor = when {
                                msg.startsWith("✅") -> MaterialTheme.colorScheme.primary
                                msg.startsWith("❌") -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.tertiary
                            }
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = msgColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Card 2: Salud del Backend
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Salud del Backend (WSGI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val isHealthy = uiState.health?.status == "ok"
                            AssistChip(
                                onClick = {},
                                label = { Text(if (isHealthy) "OPERATIVO" else "DEGRADADO") },
                                leadingIcon = {
                                    Icon(
                                        if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }

                        val health = uiState.health
                        if (health != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Google Sheets:")
                                Text(if (health.sheets) "Conectado" else "Error", fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Avisos / Calendar:")
                                Text(if (health.avisos) "Conectado" else "Error", fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Versión API:")
                                Text(health.versionApi, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text("Sin conexión con el servicio de diagnóstico.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Card: Cierre Contable Mensual y Comprobante PDF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Cierre Contable Mensual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.isEjecutandoCierre) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }

                        Text(
                            "Genera el balance contable consolidado del período actual y emite el comprobante oficial en formato PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val cierre = uiState.cierreContable
                        if (cierre != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Resumen del Cierre:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(cierre.resumen, style = MaterialTheme.typography.bodySmall)

                                    if (cierre.totalMonto > 0) {
                                        Text(
                                            "Monto Total: ₲ ${java.lang.String.format(java.util.Locale.GERMAN, "%,d", cierre.totalMonto)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    val fullPdfUrl = viewModel.obtenerUrlAbsolutaPdf(cierre.pdfUrl)
                                    if (!fullPdfUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(fullPdfUrl)
                                                    )
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "No se encontró aplicación para abrir PDF",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ver Comprobante PDF 📄")
                                        }
                                    }
                                }
                            }
                        }

                        if (!uiState.cierreMensaje.isNullOrBlank()) {
                            val msg = uiState.cierreMensaje!!
                            val color = if (msg.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = { viewModel.ejecutarCierreContable() },
                            enabled = !uiState.isEjecutandoCierre,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (cierre != null) "Regenerar Cierre Contable" else "Ejecutar Cierre Contable"
                            )
                        }
                    }
                }

                // Card 3: Tickets de Soporte
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTicketsExpanded = !isTicketsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tickets de Soporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    val countText = if (uiState.ticketFiltro == "Todos") {
                                        "${uiState.tickets.size}"
                                    } else {
                                        "${uiState.ticketsFiltrados.size}/${uiState.tickets.size}"
                                    }
                                    Text(countText)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { showNewTicketDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nuevo")
                                }
                                IconButton(onClick = { isTicketsExpanded = !isTicketsExpanded }) {
                                    Icon(
                                        imageVector = if (isTicketsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isTicketsExpanded) "Colapsar tickets" else "Expandir tickets"
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isTicketsExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Barra de Chips de Filtro
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Todos", "Abierto", "En proceso", "Cerrado").forEach { filtro ->
                                        FilterChip(
                                            selected = uiState.ticketFiltro == filtro,
                                            onClick = { viewModel.setFiltroTickets(filtro) },
                                            label = { Text(filtro, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                if (uiState.ticketsFiltrados.isEmpty()) {
                                    Text(
                                        if (uiState.tickets.isEmpty()) "No hay tickets reportados." else "No hay tickets con estado '${uiState.ticketFiltro}'.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    uiState.ticketsFiltrados.forEach { ticket ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTicketForAction = ticket },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            ticket.id,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        val origenLabel = when (ticket.origen.lowercase()) {
                                                            "telegram" -> "✈️ Telegram"
                                                            "ci" -> "🤖 CI/CD"
                                                            "servidor" -> "🚨 Servidor"
                                                            else -> "📱 App"
                                                        }
                                                        AssistChip(
                                                            onClick = { selectedTicketForAction = ticket },
                                                            label = { Text(origenLabel, style = MaterialTheme.typography.labelSmall) },
                                                            modifier = Modifier.height(24.dp)
                                                        )
                                                    }

                                                    val (estadoContainer, estadoContent) = when (ticket.estado) {
                                                        "Cerrado" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                                        "En proceso" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                                        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                                                    }
                                                    SuggestionChip(
                                                        onClick = { selectedTicketForAction = ticket },
                                                        label = {
                                                            Text(
                                                                ticket.estado,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        },
                                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                                            containerColor = estadoContainer,
                                                            labelColor = estadoContent
                                                        ),
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                }

                                                Text(ticket.asunto, fontWeight = FontWeight.SemiBold)
                                                if (ticket.descripcion.isNotEmpty()) {
                                                    Text(ticket.descripcion, style = MaterialTheme.typography.bodySmall)
                                                }

                                                if (!ticket.resolucion.isNullOrBlank()) {
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                    ) {
                                                        Text(
                                                            "💡 Resolución: ${ticket.resolucion}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.padding(8.dp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        if (ticket.fecha.isNotEmpty()) "📅 ${ticket.fecha}" else "",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                    Text(
                                                        "Toca para atender ✏️",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 4: Registros / Logs del Servidor
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Registros y Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.logs.isEmpty()) {
                            Text("Sin logs recientes.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.logs.take(5).forEach { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("[${log.nivel}]", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(log.mensaje, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Diálogo de Nuevo Ticket
            if (showNewTicketDialog) {
                var asunto by remember { mutableStateOf("") }
                var descripcion by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showNewTicketDialog = false },
                    title = { Text("Reportar Ticket de Soporte") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = asunto,
                                onValueChange = { asunto = it },
                                label = { Text("Asunto") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.crearTicket(asunto, descripcion)
                                showNewTicketDialog = false
                            },
                            enabled = asunto.isNotBlank() && descripcion.isNotBlank()
                        ) {
                            Text("Crear Ticket")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTicketDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo de Atender Ticket
            val ticketToEdit = selectedTicketForAction
            if (ticketToEdit != null) {
                var nuevoEstado by remember(ticketToEdit) { mutableStateOf(ticketToEdit.estado) }
                var resolucionInput by remember(ticketToEdit) { mutableStateOf(ticketToEdit.resolucion ?: "") }

                AlertDialog(
                    onDismissRequest = { selectedTicketForAction = null },
                    title = {
                        Text("Atender ${ticketToEdit.id}", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(ticketToEdit.asunto, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            if (ticketToEdit.descripcion.isNotEmpty()) {
                                Text(ticketToEdit.descripcion, style = MaterialTheme.typography.bodySmall)
                            }
                            val origenDesc = when (ticketToEdit.origen.lowercase()) {
                                "telegram" -> "✈️ Telegram"
                                "ci" -> "🤖 GitHub CI/CD"
                                "servidor" -> "🚨 Servidor / Vigía"
                                else -> "📱 App Móvil"
                            }
                            Text("Canal emisor: $origenDesc", style = MaterialTheme.typography.labelMedium)

                            HorizontalDivider()

                            Text("Cambiar Estado:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Abierto", "En proceso", "Cerrado").forEach { opt ->
                                    FilterChip(
                                        selected = nuevoEstado == opt,
                                        onClick = { nuevoEstado = opt },
                                        label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = resolucionInput,
                                onValueChange = { resolucionInput = it },
                                label = { Text("Resolución / Nota técnica (opcional)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.actualizarEstadoTicket(
                                    ticketId = ticketToEdit.id,
                                    nuevoEstado = nuevoEstado,
                                    resolucion = resolucionInput.ifBlank { null }
                                )
                                selectedTicketForAction = null
                            }
                        ) {
                            Text("Guardar Cambios")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedTicketForAction = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Notificación de éxito al actualizar ticket
            if (uiState.ticketActionMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.limpiarMensajes() },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Ticket Actualizado") },
                    text = { Text(uiState.ticketActionMessage!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.limpiarMensajes() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            // Notificación de éxito al crear ticket
            if (uiState.ticketCreationSuccess != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.limpiarMensajes() },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Ticket Creado") },
                    text = { Text(uiState.ticketCreationSuccess!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.limpiarMensajes() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }

            // Error dialog
            if (uiState.error != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.limpiarMensajes() },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Error") },
                    text = { Text(uiState.error!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.limpiarMensajes() }) {
                            Text("Cerrar")
                        }
                    }
                )
            }
        }
    }
}
