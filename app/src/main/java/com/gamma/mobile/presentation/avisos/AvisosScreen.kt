package com.gamma.mobile.presentation.avisos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gamma.mobile.domain.model.Aviso

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
    var showDialog by remember { mutableStateOf(false) }
    var textoIa by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            showDialog = true
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
            FloatingActionButton(onClick = {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showPermissionRationale = true
                } else {
                    showDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Aviso")
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
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            item {
                                PermissionBanner(onEnable = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(state.avisos) { aviso ->
                            AvisoItem(
                                aviso = aviso,
                                onDelete = { viewModel.deleteAviso(aviso.id) }
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
                showDialog = true
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
                    showDialog = true
                }) {
                    Text("Continuar sin avisos")
                }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Crear Aviso con IA") },
            text = {
                OutlinedTextField(
                    value = textoIa,
                    onValueChange = { textoIa = it },
                    label = { Text("Texto libre (ej. Pagar ANDE el 25 a las 10:00)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textoIa.isNotBlank()) {
                        viewModel.interpretarAvisoIa(textoIa)
                    }
                    showDialog = false
                    textoIa = ""
                }) {
                    Text("Interpretar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisoItem(aviso: Aviso, onDelete: () -> Unit) {
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showDetails = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = aviso.titulo, style = MaterialTheme.typography.titleMedium)
                Text(text = aviso.fechaEvento, style = MaterialTheme.typography.bodyMedium)
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
            text = { Text("Se eliminará '${aviso.titulo}' de la agenda.") },
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

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Detalle del Aviso") },
            text = {
                Column {
                    Text("Título: ${aviso.titulo}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fecha Evento: ${aviso.fechaEvento}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
