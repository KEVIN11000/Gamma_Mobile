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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligacionesScreen(viewModel: ObligacionesViewModel) {
    var showCrearModal by remember { mutableStateOf(false) }
    var showSimulador by remember { mutableStateOf(false) }
    var selectedObligacionToAbonar by remember { mutableStateOf<Obligacion?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    val simulacionResult by viewModel.simulacionResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Obligaciones", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
                FloatingActionButton(onClick = { showCrearModal = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Deuda")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
                            "No tienes obligaciones registradas.",
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
fun CrearObligacionModal(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
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
                    label = { Text("Monto") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toIntOrNull() ?: 0
                if (nombre.isNotBlank() && m > 0) {
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
fun AbonarModal(obligacion: Obligacion, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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
                    label = { Text("Monto a abonar") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toIntOrNull() ?: 0
                if (m > 0) {
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
    onSimular: (Int, Int) -> Unit,
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
                    label = { Text("Monto del préstamo") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cuotas,
                    onValueChange = { if (it.all { char -> char.isDigit() }) cuotas = it },
                    label = { Text("Cantidad de cuotas") }
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
                val m = monto.toIntOrNull() ?: 0
                val c = cuotas.toIntOrNull() ?: 0
                if (m > 0 && c > 0) {
                    onSimular(m, c)
                }
            }) { Text("Calcular") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
