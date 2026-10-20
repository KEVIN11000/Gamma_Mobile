package com.gamma.mobile.presentation.avisos

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gamma.mobile.domain.model.Aviso
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisosScreen(
    viewModel: AvisosViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val createError by viewModel.createError.collectAsState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showIaDialog by remember { mutableStateOf(false) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingAviso by remember { mutableStateOf<Aviso?>(null) }
    var textoIa by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            showFormDialog = true
        }
    }

    LaunchedEffect(createError) {
        if (createError != null) {
            snackbarHostState.showSnackbar(createError!!)
            viewModel.dismissError()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showIaDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Asistente IA")
                }
                FloatingActionButton(onClick = {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        showPermissionRationale = true
                    } else {
                        editingAviso = null
                        showFormDialog = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Aviso")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is AvisosUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AvisosUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionBanner(onEnable = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            })
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            "No hay avisos registrados",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            editingAviso = null
                            showFormDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Primer Aviso")
                        }
                    }
                }
                is AvisosUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is AvisosUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            item {
                                PermissionBanner(onEnable = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(state.avisos, key = { it.id }) { aviso ->
                            AvisoItem(
                                aviso = aviso,
                                onEdit = {
                                    editingAviso = aviso
                                    showFormDialog = true
                                },
                                onDelete = { viewModel.deleteAviso(aviso.id, context) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            if (isCreating) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                showPermissionRationale = false
                editingAviso = null
                showFormDialog = true
            },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Activar Notificaciones") },
            text = {
                Text("Para recibir alarmas sobre el vencimiento de tus obligaciones y avisos, Gamma Mobile necesita permiso para enviarte notificaciones en este dispositivo.")
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    editingAviso = null
                    showFormDialog = true
                }) {
                    Text("Continuar sin avisos")
                }
            }
        )
    }

    // Modal de IA para interpretar y luego abrir en el formulario completo
    if (showIaDialog) {
        AlertDialog(
            onDismissRequest = { showIaDialog = false },
            title = { Text("Interpretar Aviso con IA") },
            text = {
                Column {
                    Text(
                        "Escribe el evento en lenguaje natural. Se pre-llenará el formulario para que puedas revisarlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textoIa,
                        onValueChange = { textoIa = it },
                        label = { Text("Ej. Pagar ANDE el 25 a las 10:00 repetir mensual") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textoIa.isNotBlank()) {
                        viewModel.interpretarAvisoIa(textoIa) { avisoPropuesto ->
                            editingAviso = avisoPropuesto
                            showIaDialog = false
                            showFormDialog = true
                            textoIa = ""
                        }
                    }
                }) {
                    Text("Interpretar y Editar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal unificado de Crear / Editar Aviso
    if (showFormDialog) {
        AvisoFormDialog(
            initialAviso = editingAviso,
            onDismiss = {
                showFormDialog = false
                editingAviso = null
            },
            onSave = { avisoGuardado ->
                if (editingAviso != null && editingAviso!!.id.isNotBlank()) {
                    viewModel.updateAviso(avisoGuardado, context)
                } else {
                    viewModel.createAviso(avisoGuardado, context)
                }
                showFormDialog = false
                editingAviso = null
            }
        )
    }
}

@Composable
fun PermissionBanner(onEnable: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
        onClick = onEnable
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Notificaciones desactivadas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "Toca aquí para activarlas y recibir alarmas de tus vencimientos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun AvisoItem(
    aviso: Aviso,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aviso.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = aviso.fechaEvento,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (aviso.repeticion != Aviso.REPETICION_NINGUNA) {
                        AssistChip(
                            onClick = onEdit,
                            label = { Text("Repite: ${aviso.repeticion}", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    val hitos = Aviso.normalizarHitos(aviso.hitosMinutos)
                    if (hitos.isNotEmpty()) {
                        val hitosTexto = hitos.joinToString(", ") { m ->
                            if (m >= 1440) "${m / 1440}d" else if (m >= 60) "${m / 60}h" else "${m}m"
                        }
                        Text(
                            text = "Avisar: $hitosTexto antes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar aviso?") },
            text = { Text("Se cancelarán las alarmas y se eliminará '${aviso.titulo}' de la agenda.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AvisoFormDialog(
    initialAviso: Aviso?,
    onDismiss: () -> Unit,
    onSave: (Aviso) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf(initialAviso?.titulo ?: "") }
    var fechaStr by remember { mutableStateOf(initialAviso?.fechaEvento ?: "") }
    var repeticion by remember { mutableStateOf(initialAviso?.repeticion ?: Aviso.REPETICION_NINGUNA) }

    // Opciones predefinidas de hitos en minutos: 15m, 30m, 1h (60), 2h (120), 1d (1440)
    val opcionesHitos = listOf(
        15 to "15 min",
        30 to "30 min",
        60 to "1 hora",
        120 to "2 horas",
        1440 to "1 día"
    )
    val initialHitos = remember { (initialAviso?.hitosMinutos ?: Aviso.DEFAULT_HITOS_MINUTOS).toSet() }
    val hitosSeleccionados = remember { mutableStateListOf<Int>().apply { addAll(initialHitos) } }

    val calendar = remember { Calendar.getInstance() }

    val showDateTimePickers = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        fechaStr = String.format("%02d/%02d/%04d %02d:%02d", dayOfMonth, month + 1, year, hourOfDay, minute)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val repeticionesDisponibles = listOf(
        Aviso.REPETICION_NINGUNA to "Ninguna",
        Aviso.REPETICION_DIARIA to "Diaria",
        Aviso.REPETICION_SEMANAL to "Semanal",
        Aviso.REPETICION_MENSUAL to "Mensual",
        Aviso.REPETICION_ANUAL to "Anual"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialAviso != null && initialAviso.id.isNotBlank()) "Editar Aviso" else "Nuevo Aviso")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título / Asunto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fechaStr,
                    onValueChange = { fechaStr = it },
                    label = { Text("Fecha y Hora (dd/MM/yyyy HH:mm)") },
                    trailingIcon = {
                        IconButton(onClick = showDateTimePickers) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha y hora")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Repetición:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeticionesDisponibles.forEach { (clave, label) ->
                        FilterChip(
                            selected = repeticion == clave,
                            onClick = { repeticion = clave },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Alarmas previas (hasta 5 hitos):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    opcionesHitos.forEach { (minutos, label) ->
                        val isSelected = hitosSeleccionados.contains(minutos)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    hitosSeleccionados.remove(minutos)
                                } else {
                                    if (hitosSeleccionados.size < Aviso.MAX_HITOS) {
                                        hitosSeleccionados.add(minutos)
                                    }
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && fechaStr.isNotBlank()) {
                        val id = initialAviso?.id?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                        val avisoActualizado = Aviso(
                            id = id,
                            titulo = titulo.trim(),
                            fechaEvento = fechaStr.trim(),
                            repeticion = repeticion,
                            hitosMinutos = Aviso.normalizarHitos(hitosSeleccionados.toList())
                        )
                        onSave(avisoActualizado)
                    }
                },
                enabled = titulo.isNotBlank() && fechaStr.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
