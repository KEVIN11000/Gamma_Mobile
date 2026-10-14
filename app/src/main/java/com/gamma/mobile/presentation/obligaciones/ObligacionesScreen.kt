package com.gamma.mobile.presentation.obligaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.utils.formatGuaranies
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligacionesScreen(
    viewModel: ObligacionesViewModel,
    showTopBar: Boolean = true
) {
    var showCrearModal by remember { mutableStateOf(false) }
    var showSimulador by remember { mutableStateOf(false) }
    var selectedObligacionToAbonar by remember { mutableStateOf<Obligacion?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    val filtroActual by viewModel.filtroActual.collectAsState()
    val simulacionResult by viewModel.simulacionResult.collectAsState()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Obligaciones", fontWeight = FontWeight.Bold) },
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
                    onClick = { showCrearModal = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva Obligación") },
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
            // Chips de filtrado por estado
            FilterTabsObligaciones(
                selectedFiltro = filtroActual,
                onFilterSelected = { viewModel.setFiltro(it) }
            )

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
                                "No hay obligaciones en esta categoría.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.obligaciones) { obligacion ->
                                    ObligacionItem(
                                        obligacion = obligacion,
                                        onAprobar = { viewModel.aprobarObligacion(obligacion) },
                                        onAbonarClick = { selectedObligacionToAbonar = obligacion }
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
            onDismiss = { showCrearModal = false },
            onConfirm = { nombre, monto ->
                viewModel.crearObligacion(nombre, monto)
                showCrearModal = false
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

    if (showSimulador) {
        SimuladorCuotasModal(
            simulacionResult = simulacionResult,
            onSimular = { m, c -> viewModel.simularCuota(m, c) },
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
    onAbonarClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(obligacion.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Estado: ${obligacion.estado}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
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

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (obligacion.estado == "Simulado") {
                    Button(onClick = onAprobar, modifier = Modifier.weight(1f)) {
                        Text("Aprobar")
                    }
                } else if (obligacion.estado == "Activo") {
                    Button(onClick = onAbonarClick, modifier = Modifier.weight(1f)) {
                        Text("Abonar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearObligacionModal(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Obligación") },
        text = {
            Column {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = monto, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toLongOrNull() ?: 0L
                if (nombre.isNotBlank() && m > 0L) {
                    onConfirm(nombre, m)
                }
            }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbonarModal(obligacion: Obligacion, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var monto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a ${obligacion.nombre}") },
        text = {
            Column {
                Text("Saldo Actual: ${formatGuaranies(obligacion.saldoActual)}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = monto, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                    label = { Text("Monto a abonar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toLongOrNull() ?: 0L
                if (m > 0L) {
                    onConfirm(m)
                }
            }) { Text("Confirmar") }
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
    onSimular: (Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var monto by remember { mutableStateOf("") }
    var cuotas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulador de Cuotas") },
        text = {
            Column {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (it.all { char -> char.isDigit() }) monto = it },
                    label = { Text("Monto del préstamo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cuotas,
                    onValueChange = { if (it.all { char -> char.isDigit() }) cuotas = it },
                    label = { Text("Cantidad de cuotas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (simulacionResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val color = when (simulacionResult.nivelRiesgo) {
                        NivelRiesgo.VERDE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        NivelRiesgo.AMARILLO -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                        NivelRiesgo.ROJO -> androidx.compose.ui.graphics.Color(0xFFF44336)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Cuota proyectada: ${formatGuaranies(simulacionResult.cuotaMensual)}",
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(simulacionResult.mensaje, color = color)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toLongOrNull() ?: 0L
                val c = cuotas.toIntOrNull() ?: 0
                if (m > 0L && c > 0) {
                    onSimular(m, c)
                }
            }) { Text("Calcular") }
        },
        dismissButton = {
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
        FiltroObligacion.SALDADAS to "Saldadas",
        FiltroObligacion.SIMULADAS to "Simuladas"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
