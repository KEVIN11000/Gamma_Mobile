package com.gamma.mobile.presentation.sistema

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.utils.DeviceAuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SistemaScreen(
    viewModel: SistemaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showNewTicketDialog by remember { mutableStateOf(false) }
    var isTicketsExpanded by remember { mutableStateOf(false) }
    var selectedTicketDetail by remember { mutableStateOf<TicketItem?>(null) }

    // Estado de configuración y seguridad
    var isConfigLocked by remember { mutableStateOf(true) }
    var showKeyText by remember { mutableStateOf(false) }
    var serverUrlInput by remember(uiState.baseUrl) { mutableStateOf(uiState.baseUrl) }
    var apiKeyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }

    // Launcher para autenticación del dispositivo (Keyguard / PIN / Patrón / Huella)
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingAuthAction?.invoke()
        }
        pendingAuthAction = null
    }

    val solicitarAutenticacion: (() -> Unit) -> Unit = { onSuccess ->
        if (DeviceAuthHelper.isDeviceSecure(context)) {
            val intent = DeviceAuthHelper.createConfirmDeviceCredentialIntent(
                context = context,
                title = "Autenticación del Dispositivo",
                description = "Confirme su PIN, patrón o biometría para acceder a las credenciales"
            )
            if (intent != null) {
                pendingAuthAction = onSuccess
                authLauncher.launch(intent)
            } else {
                onSuccess()
            }
        } else {
            // El celular no tiene configurado bloqueo de pantalla
            onSuccess()
        }
    }

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
                                Text("Versión instalada: ${uiState.appVersion.ifEmpty { "0.0.9" }}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Card 2: Conexión y API Key (Protegida con Bloqueo del Celular)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    onClick = {
                                        if (isConfigLocked) {
                                            solicitarAutenticacion { isConfigLocked = false }
                                        } else {
                                            isConfigLocked = true
                                            showKeyText = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isConfigLocked) Icons.Default.Lock else Icons.Default.LockOpen,
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
                                TextButton(onClick = {
                                    if (showKeyText) {
                                        showKeyText = false
                                    } else {
                                        solicitarAutenticacion { showKeyText = true }
                                    }
                                }) {
                                    Text(if (showKeyText) "Ocultar" else "Ver")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isConfigLocked) {
                            FilledTonalButton(
                                onClick = {
                                    solicitarAutenticacion { isConfigLocked = false }
                                },
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
                                    showKeyText = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar y Probar Conexión")
                            }
                        }

                        if (!uiState.configMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.configMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.configMessage!!.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Card 3: Tickets de Soporte (Solo Lectura de Estado + Nuevo Ticket)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
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
                                                .clickable { selectedTicketDetail = ticket },
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
                                                            onClick = { selectedTicketDetail = ticket },
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
                                                        onClick = { selectedTicketDetail = ticket },
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

                                                if (ticket.fecha.isNotEmpty()) {
                                                    Text(
                                                        "📅 ${ticket.fecha}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
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

                // Card 4: Registros y Logs Dinámicos
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                            Text("Registros y Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { viewModel.recargarLogs() },
                                enabled = !uiState.isLogsRefreshing
                            ) {
                                if (uiState.isLogsRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar logs")
                                }
                            }
                        }

                        if (uiState.logs.isEmpty()) {
                            Text("Sin logs recientes.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.logs.take(15).forEach { log ->
                                    val nivelColor = when (log.nivel.uppercase()) {
                                        "ERROR" -> MaterialTheme.colorScheme.error
                                        "WARN" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (log.timestamp.isNotBlank()) {
                                            Text(
                                                log.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            "[${log.nivel}]",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = nivelColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            log.mensaje,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
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
                                label = { Text("Descripción del problema") },
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
                            Text("Enviar Ticket")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTicketDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo de Detalle de Ticket (Solo Lectura)
            selectedTicketDetail?.let { ticket ->
                AlertDialog(
                    onDismissRequest = { selectedTicketDetail = null },
                    title = {
                        Text("Detalle: ${ticket.id}", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(ticket.asunto, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Estado: ${ticket.estado}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("Canal: ${ticket.origen}", style = MaterialTheme.typography.bodySmall)
                            if (ticket.fecha.isNotEmpty()) {
                                Text("Fecha: ${ticket.fecha}", style = MaterialTheme.typography.bodySmall)
                            }
                            HorizontalDivider()
                            Text("Descripción:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text(ticket.descripcion.ifEmpty { "Sin descripción adicional." }, style = MaterialTheme.typography.bodySmall)

                            if (!ticket.resolucion.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Resolución del equipo:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                Text(ticket.resolucion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedTicketDetail = null }) {
                            Text("Cerrar")
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
