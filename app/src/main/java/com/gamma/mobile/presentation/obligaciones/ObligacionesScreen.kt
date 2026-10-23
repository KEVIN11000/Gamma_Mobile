package com.gamma.mobile.presentation.obligaciones

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import com.gamma.mobile.domain.model.DeudaFocoResponse
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.gamma.mobile.ui.theme.GammaTheme
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.utils.formatGuaranies
import com.gamma.mobile.utils.obtenerEstadoVencimiento
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligacionesScreen(
    viewModel: ObligacionesViewModel,
    showTopBar: Boolean = true,
    onNavigateToCapacidad: () -> Unit = {}
) {
    var showCrearModal by remember { mutableStateOf(false) }
    var showSimulador by remember { mutableStateOf(false) }
    var initialCrearObligacionData by remember { mutableStateOf<Obligacion?>(null) }
    var selectedObligacionToAbonar by remember { mutableStateOf<Obligacion?>(null) }
    var obligacionToDelete by remember { mutableStateOf<Obligacion?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val filtroActual by viewModel.filtroActual.collectAsState()
    val simulacionResult by viewModel.simulacionResult.collectAsState()
    val cronogramaVencimientos by viewModel.cronogramaVencimientos.collectAsState()
    val deudaFoco by viewModel.deudaFoco.collectAsState()
    val criterioFoco by viewModel.criterioFoco.collectAsState()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Obligaciones y Deudas", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showSimulador = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text("Simular Cuota")
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        initialCrearObligacionData = null
                        showCrearModal = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva ObligaciÃ³n") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            FilterTabsObligaciones(
                selectedFiltro = filtroActual,
                onFilterSelected = { viewModel.setFiltro(it) }
            )

            val compromisoMensual by viewModel.compromisoMensual.collectAsState()
            if (compromisoMensual > 0L) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Compromiso mensual activo:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatGuaranies(compromisoMensual),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToCapacidad,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Simular", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ObligacionesUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ObligacionesUiState.Error -> {
                        Text(
                            state.message, 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is ObligacionesUiState.Success -> {
                        if (state.obligaciones.isEmpty()) {
                            Text(
                                "No hay obligaciones en esta categorÃ­a.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 152.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (filtroActual == FiltroObligacion.TODAS || filtroActual == FiltroObligacion.ACTIVAS) {
                                    if (cronogramaVencimientos.isNotEmpty()) {
                                        item(key = "cronograma_vencimientos") {
                                            CronogramaVencimientosSection(
                                                cronograma = cronogramaVencimientos
                                            )
                                        }
                                    }

                                    deudaFoco?.let { foco ->
                                        if (!foco.idDeuda.isNullOrBlank()) {
                                            item(key = "deuda_foco") {
                                                DeudaFocoCard(
                                                    deudaFoco = foco,
                                                    criterioActual = criterioFoco,
                                                    onCriterioSelected = { viewModel.cambiarCriterioFoco(it) },
                                                    onAbonarClick = { idDeuda ->
                                                        val deuda = state.obligaciones.find { it.id == idDeuda }
                                                        if (deuda != null) {
                                                            selectedObligacionToAbonar = deuda
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                items(state.obligaciones, key = { it.id }) { obligacion ->
                                    ObligacionItem(
                                        obligacion = obligacion,
                                        onAprobar = { viewModel.aprobarObligacion(obligacion) },
                                        onCancelarSimulada = { viewModel.cancelarObligacionSimulada(obligacion) },
                                        onAbonarClick = { selectedObligacionToAbonar = obligacion },
                                        onDeleteClick = { obligacionToDelete = obligacion },
                                        onPausarClick = { viewModel.pausarObligacion(obligacion) },
                                        onReanudarClick = { viewModel.reanudarObligacion(obligacion) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCrearModal) {
        CrearObligacionModal(
            initialData = initialCrearObligacionData,
            onDismiss = {
                showCrearModal = false
                initialCrearObligacionData = null
            },
            onConfirm = { nuevaObligacion ->
                viewModel.crearObligacion(nuevaObligacion)
                showCrearModal = false
                initialCrearObligacionData = null
            }
        )
    }

    selectedObligacionToAbonar?.let { obligacion ->
        AbonarModal(
            obligacion = obligacion,
            onDismiss = { selectedObligacionToAbonar = null },
            onConfirm = { monto ->
                viewModel.abonarObligacion(obligacion, monto)
                selectedObligacionToAbonar = null
            }
        )
    }

    obligacionToDelete?.let { obligacion ->
        AlertDialog(
            onDismissRequest = { obligacionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Â¿Eliminar registro de deuda?") },
            text = { Text("Se eliminarÃ¡ definitivamente '${obligacion.nombre}' del sistema.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarObligacion(obligacion.id)
                        obligacionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { obligacionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSimulador) {
        SimuladorCuotasModal(
            simulacionResult = simulacionResult,
            onSimular = { m, c, cuotaManual -> viewModel.simularCuota(m, c, cuotaManual) },
            onTransferirANueva = { prep ->
                viewModel.clearSimulacion()
                showSimulador = false
                initialCrearObligacionData = prep
                showCrearModal = true
            },
            onDismiss = { 
                viewModel.clearSimulacion()
                showSimulador = false 
            }
        )
    }
}

@Composable
fun ObligacionItem(
    obligacion: Obligacion,
    onAprobar: () -> Unit,
    onCancelarSimulada: () -> Unit,
    onAbonarClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPausarClick: () -> Unit = {},
    onReanudarClick: () -> Unit = {}
) {
    var showConfirmCancelar by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        obligacion.nombre,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (obligacion.entidad.isNotBlank()) {
                        Text(
                            "Entidad: ${obligacion.entidad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de Estado
                    val badgeColor = when {
                        obligacion.esSaldada -> MaterialTheme.colorScheme.primaryContainer
                        obligacion.esSimulada -> MaterialTheme.colorScheme.tertiaryContainer
                        obligacion.esPausada -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                    val textColor = when {
                        obligacion.esSaldada -> MaterialTheme.colorScheme.onPrimaryContainer
                        obligacion.esSimulada -> MaterialTheme.colorScheme.onTertiaryContainer
                        obligacion.esPausada -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(obligacion.estado, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                    }

                    // BotÃ³n Eliminar para deudas
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar deuda",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Datos de cuotas y vencimiento si existen
            val vencimientoInfo = obligacion.obtenerEstadoVencimiento()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (obligacion.cuotasTotales > 0) {
                    val pagadas = (obligacion.cuotasTotales - obligacion.cuotasRestantes).coerceAtLeast(0)
                    Text(
                        "Cuotas: $pagadas / ${obligacion.cuotasTotales} (${formatGuaranies(obligacion.cuotaReferenciaGs)}/mes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (vencimientoInfo != null) {
                    val vencimientoColor = when {
                        obligacion.esSaldada -> MaterialTheme.colorScheme.onSurfaceVariant
                        vencimientoInfo.esVencido -> MaterialTheme.colorScheme.error
                        vencimientoInfo.esHoy -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vencimientoInfo.esVencido && !obligacion.esSaldada) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Vencido",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            vencimientoInfo.texto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (vencimientoInfo.esVencido) FontWeight.Bold else FontWeight.SemiBold,
                            color = vencimientoColor
                        )
                    }
                }
            }

            if (obligacion.observaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Obs: ${obligacion.observaciones}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            val abonado = obligacion.montoInicial - obligacion.saldoActual
            val progress = if (obligacion.montoInicial > 0) abonado.toFloat() / obligacion.montoInicial else 0f
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Abonado: ${formatGuaranies(abonado)}")
                Text("Total: ${formatGuaranies(obligacion.montoInicial)}")
            }
            Text("Saldo: ${formatGuaranies(obligacion.saldoActual)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (obligacion.esSimulada) {
                    Button(onClick = onAprobar, modifier = Modifier.weight(1f)) {
                        Text("Aprobar")
                    }
                    OutlinedButton(
                        onClick = { showConfirmCancelar = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar")
                    }
                } else if (obligacion.esPausada) {
                    Button(
                        onClick = onReanudarClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reanudar Cuota")
                    }
                } else if (obligacion.esActiva) {
                    Button(onClick = onAbonarClick, modifier = Modifier.weight(1f)) {
                        Text("Abonar Cuota")
                    }
                    OutlinedButton(
                        onClick = onPausarClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pausar este mes")
                    }
                }
            }
        }
    }

    if (showConfirmCancelar) {
        AlertDialog(
            onDismissRequest = { showConfirmCancelar = false },
            title = { Text("Â¿Cancelar simulaciÃ³n?") },
            text = { Text("Se descartarÃ¡ la simulaciÃ³n de '${obligacion.nombre}'.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCancelar = false
                        onCancelarSimulada()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancelar SimulaciÃ³n")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCancelar = false }) {
                    Text("Volver")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearObligacionModal(
    initialData: Obligacion? = null,
    onDismiss: () -> Unit,
    onConfirm: (Obligacion) -> Unit
) {
    val context = LocalContext.current
    var entidad by remember { mutableStateOf(initialData?.entidad ?: "") }
    var nombre by remember { mutableStateOf(initialData?.nombre ?: "") }
    var tipo by remember { mutableStateOf(initialData?.tipo?.takeIf { it.isNotBlank() } ?: "Deuda") }
    var montoTotal by remember { mutableStateOf(initialData?.montoInicial?.takeIf { it > 0 }?.toString() ?: "") }
    var cuotasTotales by remember { mutableStateOf(initialData?.cuotasTotales?.takeIf { it > 0 }?.toString() ?: "") }
    var cuotaRef by remember { mutableStateOf(initialData?.cuotaReferenciaGs?.takeIf { it > 0 }?.toString() ?: "") }
    var diaVencimiento by remember { mutableStateOf(initialData?.diaVencimiento?.takeIf { it > 0 }?.toString() ?: "") }
    var fechaInicio by remember { mutableStateOf(initialData?.fechaInicio ?: "") }
    var observaciones by remember { mutableStateOf(initialData?.observaciones ?: "") }
    var estadoInicial by remember { mutableStateOf(initialData?.estado?.takeIf { it.isNotBlank() } ?: "Activo") }

    val tiposDisponibles = listOf("Deuda", "PrÃ©stamo", "Tarjeta", "Gasto Fijo")
    val calendar = remember { Calendar.getInstance() }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fechaInicio = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Auto-cÃ¡lculo de cuota si cambia monto o cantidad de cuotas y cuotaRef no fue editada manualmente
    LaunchedEffect(montoTotal, cuotasTotales) {
        val m = montoTotal.toLongOrNull() ?: 0L
        val c = cuotasTotales.toIntOrNull() ?: 0
        if (m > 0L && c > 0 && cuotaRef.isBlank()) {
            cuotaRef = (m / c).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva ObligaciÃ³n / Deuda") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = entidad,
                    onValueChange = { entidad = it },
                    label = { Text("Entidad / Acreedor (ej. Banco ItaÃº)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Concepto (ej. PrÃ©stamo Personal)*") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Tipo de ObligaciÃ³n:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tiposDisponibles.forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = montoTotal,
                    onValueChange = { if (it.all { c -> c.isDigit() }) montoTotal = it },
                    label = { Text("Monto Total (Gs.)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cuotasTotales,
                        onValueChange = { if (it.all { c -> c.isDigit() }) cuotasTotales = it },
                        label = { Text("Cant. Cuotas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cuotaRef,
                        onValueChange = { if (it.all { c -> c.isDigit() }) cuotaRef = it },
                        label = { Text("Cuota (Gs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diaVencimiento,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) diaVencimiento = it },
                        label = { Text("DÃ­a Venc. (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fechaInicio,
                        onValueChange = { fechaInicio = it },
                        label = { Text("1Â° Vencimiento") },
                        trailingIcon = {
                            IconButton(onClick = showDatePicker) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.weight(1.5f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Estado Inicial:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = estadoInicial.equals("Activo", ignoreCase = true),
                        onClick = { estadoInicial = "Activo" },
                        label = { Text("Activa", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = estadoInicial.equals("Pausada", ignoreCase = true) || estadoInicial.equals("Pausado", ignoreCase = true),
                        onClick = { estadoInicial = "Pausada" },
                        label = { Text("Pausada", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = estadoInicial.equals("Simulado", ignoreCase = true) || estadoInicial.equals("Simulada", ignoreCase = true),
                        onClick = { estadoInicial = "Simulado" },
                        label = { Text("Simulada", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoTotal.toLongOrNull() ?: 0L
                    val cTot = cuotasTotales.toIntOrNull() ?: 0
                    val cRef = cuotaRef.toLongOrNull() ?: (if (cTot > 0) m / cTot else 0L)
                    val dia = diaVencimiento.toIntOrNull() ?: 0

                    if (nombre.isNotBlank() && m > 0L) {
                        val obligacion = Obligacion(
                            id = initialData?.id ?: "",
                            nombre = nombre.trim(),
                            montoInicial = m,
                            saldoActual = m,
                            estado = estadoInicial,
                            entidad = entidad.trim(),
                            tipo = tipo,
                            cuotaReferenciaGs = cRef,
                            cuotasTotales = cTot,
                            cuotasRestantes = cTot,
                            diaVencimiento = dia,
                            fechaInicio = fechaInicio.trim(),
                            observaciones = observaciones.trim()
                        )
                        onConfirm(obligacion)
                    }
                },
                enabled = nombre.isNotBlank() && (montoTotal.toLongOrNull() ?: 0L) > 0L
            ) {
                Text("Guardar ObligaciÃ³n")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbonarModal(obligacion: Obligacion, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var monto by remember {
        mutableStateOf(
            if (obligacion.cuotaReferenciaGs > 0L) obligacion.cuotaReferenciaGs.toString() else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a ${obligacion.nombre}") },
        text = {
            Column {
                Text("Saldo Actual: ${formatGuaranies(obligacion.saldoActual)}")
                if (obligacion.cuotaReferenciaGs > 0L) {
                    Text(
                        "Cuota sugerida: ${formatGuaranies(obligacion.cuotaReferenciaGs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = monto, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                    label = { Text("Monto a abonar (Gs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toLongOrNull() ?: 0L
                if (m > 0L) {
                    onConfirm(m)
                }
            }) { Text("Confirmar Abono") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimuladorCuotasModal(
    simulacionResult: SimulacionResult?,
    onSimular: (Long, Int, Long?) -> Unit,
    onTransferirANueva: (Obligacion) -> Unit,
    onDismiss: () -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var entidad by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var cuotas by remember { mutableStateOf("") }
    var cuotaPersonalizada by remember { mutableStateOf("") }

    // Auto-cÃ¡lculo de cuota sugerida al cambiar monto o cuotas
    LaunchedEffect(monto, cuotas) {
        val m = monto.toLongOrNull() ?: 0L
        val c = cuotas.toIntOrNull() ?: 0
        if (m > 0L && c > 0) {
            cuotaPersonalizada = (m / c).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulador de Cuotas Ampliado") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (ej. PrÃ©stamo Auto)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = entidad,
                    onValueChange = { entidad = it },
                    label = { Text("Entidad financiera (ej. Banco)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                    label = { Text("Monto del prÃ©stamo (Gs.)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cuotas,
                        onValueChange = { if (it.all { char -> char.isDigit() }) cuotas = it },
                        label = { Text("Cant. Cuotas*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cuotaPersonalizada,
                        onValueChange = { if (it.all { char -> char.isDigit() }) cuotaPersonalizada = it },
                        label = { Text("Cuota (Gs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val m = monto.toLongOrNull() ?: 0L
                        val c = cuotas.toIntOrNull() ?: 0
                        val cp = cuotaPersonalizada.toLongOrNull()
                        if (m > 0L && c > 0) {
                            onSimular(m, c, cp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (monto.toLongOrNull() ?: 0L) > 0L && (cuotas.toIntOrNull() ?: 0) > 0
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calcular Riesgo Crediticio")
                }

                if (simulacionResult != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val color = when (simulacionResult.nivelRiesgo) {
                        NivelRiesgo.VERDE -> Color(0xFF2E7D32)
                        NivelRiesgo.AMARILLO -> Color(0xFFF57F17)
                        NivelRiesgo.ROJO -> MaterialTheme.colorScheme.error
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Cuota proyectada: ${formatGuaranies(simulacionResult.cuotaMensual)}",
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(simulacionResult.mensaje, color = color, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val m = monto.toLongOrNull() ?: 0L
                            val c = cuotas.toIntOrNull() ?: 0
                            val cp = cuotaPersonalizada.toLongOrNull() ?: (if (c > 0) m / c else 0L)
                            val prep = Obligacion(
                                nombre = if (concepto.isNotBlank()) concepto else "PrÃ©stamo Simulado",
                                entidad = entidad,
                                montoInicial = m,
                                saldoActual = m,
                                estado = "Simulado",
                                cuotaReferenciaGs = cp,
                                cuotasTotales = c,
                                cuotasRestantes = c
                            )
                            onTransferirANueva(prep)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear como Nueva ObligaciÃ³n")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun FilterTabsObligaciones(
    selectedFiltro: FiltroObligacion,
    onFilterSelected: (FiltroObligacion) -> Unit
) {
    val filters = listOf(
        FiltroObligacion.TODAS to "Todas",
        FiltroObligacion.ACTIVAS to "Activas",
        FiltroObligacion.PAUSADAS to "Pausadas",
        FiltroObligacion.SALDADAS to "Saldadas",
        FiltroObligacion.SIMULADAS to "Simuladas"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filtro, title) ->
            FilterChip(
                selected = selectedFiltro == filtro,
                onClick = { onFilterSelected(filtro) },
                label = { Text(title, fontWeight = if (selectedFiltro == filtro) FontWeight.Bold else FontWeight.Normal) }
            )
        }
    }
}

@Composable
fun CronogramaVencimientosSection(
    cronograma: List<VencimientoCronogramaItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Cronograma de Vencimientos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${cronograma.size} este mes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(cronograma, key = { it.id }) { item ->
                    VencimientoItemChip(item = item)
                }
            }
        }
    }
}

@Composable
fun VencimientoItemChip(
    item: VencimientoCronogramaItem,
    modifier: Modifier = Modifier
) {
    val statusBgColor = when {
        item.vencido -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        item.diasRestantes == 0 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        item.diasRestantes in 1..7 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }

    val statusTextColor = when {
        item.vencido -> MaterialTheme.colorScheme.error
        item.diasRestantes == 0 -> MaterialTheme.colorScheme.tertiary
        item.diasRestantes in 1..7 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        item.vencido -> "VenciÃ³"
        item.diasRestantes == 0 -> "Hoy"
        item.diasRestantes == 1 -> "MaÃ±ana"
        else -> "En ${item.diasRestantes}d"
    }

    Surface(
        modifier = modifier.widthIn(min = 130.dp, max = 150.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.vencido) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "DÃ­a ${item.diaVencimiento}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBgColor
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = item.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (item.entidad.isNotBlank()) {
                Text(
                    text = item.entidad,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = formatGuaranies(item.cuotaGs),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DeudaFocoCard(
    deudaFoco: DeudaFocoResponse,
    criterioActual: String,
    onCriterioSelected: (String) -> Unit,
    onAbonarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Estrategia: Deuda Foco",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = criterioActual == "saldo",
                        onClick = { onCriterioSelected("saldo") },
                        label = { Text("Bola de Nieve", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (criterioActual == "saldo") {
                            { Text("â„ï¸", style = MaterialTheme.typography.labelSmall) }
                        } else null
                    )
                    FilterChip(
                        selected = criterioActual == "prioridad",
                        onClick = { onCriterioSelected("prioridad") },
                        label = { Text("Avalancha", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (criterioActual == "prioridad") {
                            { Text("âš¡", style = MaterialTheme.typography.labelSmall) }
                        } else null
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deudaFoco.nombre.ifBlank { "Deuda sugerida" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (deudaFoco.entidad.isNotBlank()) {
                                Text(
                                    text = deudaFoco.entidad,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!deudaFoco.idDeuda.isNullOrBlank()) {
                            Button(
                                onClick = { onAbonarClick(deudaFoco.idDeuda) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Abonar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Saldo: ${formatGuaranies(deudaFoco.saldoActual)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cuota: ${formatGuaranies(deudaFoco.cuotaGs)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (deudaFoco.explicacion.isNotBlank()) {
                        Text(
                            text = deudaFoco.explicacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CronogramaVencimientosPreview() {
    GammaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CronogramaVencimientosSection(
                cronograma = listOf(
                    VencimientoCronogramaItem(
                        id = "1",
                        nombre = "Tarjeta Visa",
                        entidad = "Banco Continental",
                        diaVencimiento = 5,
                        cuotaGs = 350000L,
                        saldoActual = 1200000L,
                        vencido = true,
                        diasRestantes = 0,
                        fechaVencimientoExacta = "05/10/2026"
                    ),
                    VencimientoCronogramaItem(
                        id = "2",
                        nombre = "PrÃ©stamo Personal",
                        entidad = "ItaÃº",
                        diaVencimiento = 18,
                        cuotaGs = 850000L,
                        saldoActual = 8500000L,
                        vencido = false,
                        diasRestantes = 0,
                        fechaVencimientoExacta = "18/10/2026"
                    ),
                    VencimientoCronogramaItem(
                        id = "3",
                        nombre = "Cuota Auto",
                        entidad = "Ueno Bank",
                        diaVencimiento = 25,
                        cuotaGs = 1200000L,
                        saldoActual = 24000000L,
                        vencido = false,
                        diasRestantes = 7,
                        fechaVencimientoExacta = "25/10/2026"
                    )
                )
            )

            DeudaFocoCard(
                deudaFoco = DeudaFocoResponse(
                    idDeuda = "1",
                    nombre = "Tarjeta Visa",
                    entidad = "Banco Continental",
                    saldoActual = 1200000L,
                    cuotaGs = 350000L,
                    criterio = "saldo",
                    ahorroEstimadoMeses = 3,
                    explicacion = "Estrategia Bola de Nieve: Tarjeta Visa tiene el menor saldo (1.200.000 ₲). Liquidarla primero genera una victoria rápida."
                ),
                criterioActual = "saldo",
                onCriterioSelected = {},
                onAbonarClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ObligacionItemPreview() {
    GammaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ObligacionItem(
                obligacion = Obligacion(
                    id = "preview-1",
                    nombre = "PrÃ©stamo Bancario",
                    montoInicial = 10000000L,
                    saldoActual = 6500000L,
                    estado = "Activo",
                    entidad = "Banco Continental",
                    cuotaReferenciaGs = 850000L,
                    cuotasTotales = 12,
                    cuotasRestantes = 8,
                    diaVencimiento = 25
                ),
                onAprobar = {},
                onCancelarSimulada = {},
                onAbonarClick = {},
                onDeleteClick = {},
                onPausarClick = {},
                onReanudarClick = {}
            )
            ObligacionItem(
                obligacion = Obligacion(
                    id = "preview-2",
                    nombre = "PrÃ©stamo Cooperativa (Vencido)",
                    montoInicial = 4000000L,
                    saldoActual = 2500000L,
                    estado = "Activo",
                    entidad = "Coop. San CristÃ³bal",
                    cuotaReferenciaGs = 450000L,
                    cuotasTotales = 10,
                    cuotasRestantes = 6,
                    diaVencimiento = 10
                ),
                onAprobar = {},
                onCancelarSimulada = {},
                onAbonarClick = {},
                onDeleteClick = {},
                onPausarClick = {},
                onReanudarClick = {}
            )
            ObligacionItem(
                obligacion = Obligacion(
                    id = "preview-3",
                    nombre = "Tarjeta Financiada",
                    montoInicial = 3000000L,
                    saldoActual = 1500000L,
                    estado = "Pausada",
                    entidad = "Banco Familiar",
                    cuotaReferenciaGs = 500000L,
                    cuotasTotales = 6,
                    cuotasRestantes = 3,
                    diaVencimiento = 12
                ),
                onAprobar = {},
                onCancelarSimulada = {},
                onAbonarClick = {},
                onDeleteClick = {},
                onPausarClick = {},
                onReanudarClick = {}
            )
        }
    }
}
