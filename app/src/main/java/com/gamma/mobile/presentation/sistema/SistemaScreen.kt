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
    val scrollState = rememberScrollState()
    var showNewTicketDialog by remember { mutableStateOf(false) }
    var isTicketsExpanded by remember { mutableStateOf(false) }

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
                                Text("Versión instalada: ${uiState.appVersion.ifEmpty { "0.0.4+debug" }}", fontWeight = FontWeight.SemiBold)
                                Text("Entorno: Android (minSdk 26, targetSdk 34)", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        val context = androidx.compose.ui.platform.LocalContext.current

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
                                    Text("${uiState.tickets.size}")
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.tickets.isEmpty()) {
                                    Text("No hay tickets reportados.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    uiState.tickets.forEach { ticket ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(ticket.id, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(ticket.estado, style = MaterialTheme.typography.labelSmall)
                                                }
                                                Text(ticket.asunto, fontWeight = FontWeight.SemiBold)
                                                Text(ticket.descripcion, style = MaterialTheme.typography.bodySmall)
                                                if (ticket.fecha.isNotEmpty()) {
                                                    Text("Fecha: ${ticket.fecha}", style = MaterialTheme.typography.labelSmall)
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
