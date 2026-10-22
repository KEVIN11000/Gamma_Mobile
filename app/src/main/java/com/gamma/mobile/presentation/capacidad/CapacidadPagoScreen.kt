package com.gamma.mobile.presentation.capacidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.CapacidadPagoResponse
import com.gamma.mobile.domain.model.EscenarioCapacidad
import com.gamma.mobile.domain.model.GastoSimulado
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.SemaforoViabilidad
import com.gamma.mobile.ui.theme.GammaTheme
import com.gamma.mobile.utils.formatGuaranies
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapacidadPagoScreen(
    viewModel: CapacidadPagoViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val deudasPausadas by viewModel.deudasPausadasSimuladas.collectAsState()
    val abonosExtra by viewModel.abonosExtraordinarios.collectAsState()
    val ajusteIngreso by viewModel.ajusteIngresoExtra.collectAsState()
    val gastosSimulados by viewModel.gastosSimulados.collectAsState()
    val escenariosGuardados by viewModel.escenariosGuardados.collectAsState()
    val escenarioActivoNombre by viewModel.escenarioActivoNombre.collectAsState()

    var showEscenariosSheet by remember { mutableStateOf(false) }
    var showGuardarDialog by remember { mutableStateOf(false) }
    var showAgregarGastoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Proyector de Capacidad", fontWeight = FontWeight.Bold)
                        if (escenarioActivoNombre != null) {
                            Text(
                                text = "Escenario: $escenarioActivoNombre",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showEscenariosSheet = true }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Escenarios guardados")
                    }
                    IconButton(onClick = { viewModel.resetSimulacion() }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Restablecer Simulación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is CapacidadPagoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CapacidadPagoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadDatos(forceRefresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is CapacidadPagoUiState.Success -> {
                    CapacidadPagoContent(
                        resultado = state.resultado,
                        deudasActivas = state.deudasActivas,
                        deudasPausadas = deudasPausadas,
                        abonosExtra = abonosExtra,
                        ajusteIngreso = ajusteIngreso,
                        gastosSimulados = gastosSimulados,
                        onTogglePausa = { viewModel.toggleSimularPausa(it) },
                        onSetAbono = { id, monto -> viewModel.setAbonoExtraordinario(id, monto) },
                        onSetIngresoExtra = { viewModel.setAjusteIngresoExtra(it) },
                        onToggleGasto = { viewModel.toggleGastoAdHoc(it) },
                        onEliminarGasto = { viewModel.eliminarGastoAdHoc(it) },
                        onOpenAgregarGasto = { showAgregarGastoDialog = true }
                    )
                }
            }
        }
    }

    if (showAgregarGastoDialog) {
        AgregarGastoDialog(
            onDismiss = { showAgregarGastoDialog = false },
            onConfirm = { concepto, monto ->
                viewModel.agregarGastoAdHoc(concepto, monto)
                showAgregarGastoDialog = false
            }
        )
    }

    if (showGuardarDialog) {
        GuardarEscenarioDialog(
            onDismiss = { showGuardarDialog = false },
            onConfirm = { nombre ->
                viewModel.guardarEscenarioActual(nombre)
                showGuardarDialog = false
            }
        )
    }

    if (showEscenariosSheet) {
        EscenariosBottomSheet(
            escenarios = escenariosGuardados,
            onDismiss = { showEscenariosSheet = false },
            onGuardarNuevo = {
                showGuardarDialog = true
            },
            onCargar = { escenario ->
                viewModel.cargarEscenario(escenario)
                showEscenariosSheet = false
            },
            onEliminar = { id ->
                viewModel.eliminarEscenario(id)
            }
        )
    }
}

@Composable
fun CapacidadPagoContent(
    resultado: CapacidadPagoResponse,
    deudasActivas: List<Obligacion>,
    deudasPausadas: Set<String>,
    abonosExtra: Map<String, Long>,
    ajusteIngreso: Long,
    gastosSimulados: List<GastoSimulado> = emptyList(),
    onTogglePausa: (String) -> Unit,
    onSetAbono: (String, Long) -> Unit,
    onSetIngresoExtra: (Long) -> Unit,
    onToggleGasto: (String) -> Unit = {},
    onEliminarGasto: (String) -> Unit = {},
    onOpenAgregarGasto: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Semáforo de Viabilidad
        item {
            SemaforoViabilidadCard(resultado = resultado)
        }

        // 2. Resumen Numérico
        item {
            MetricasCapacidadCard(resultado = resultado)
        }

        // 3. Ajuste de Ingreso Extraordinario
        item {
            IngresoExtraordinarioCard(
                ajusteIngreso = ajusteIngreso,
                onSetIngresoExtra = onSetIngresoExtra
            )
        }

        // 4. Sección de Gastos Adicionales / Imprevistos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gastos Adicionales / Imprevistos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simula compras o gastos no fijos para este mes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onOpenAgregarGasto,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Gasto", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (gastosSimulados.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "No has agregado gastos adicionales. Usa '+ Gasto' para proyectar compras o imprevistos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(gastosSimulados, key = { it.id }) { gasto ->
                GastoSimuladoItem(
                    gasto = gasto,
                    onToggle = { onToggleGasto(gasto.id) },
                    onEliminar = { onEliminarGasto(gasto.id) }
                )
            }
        }

        // 5. Encabezado de Deudas
        item {
            Text(
                text = "Simulador de Cuotas y Abonos del Mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (deudasActivas.isEmpty()) {
            item {
                Text(
                    text = "No hay deudas activas registradas para simular este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(deudasActivas, key = { it.id }) { deuda ->
                val esPausada = deudasPausadas.contains(deuda.id)
                val abonoActual = abonosExtra[deuda.id] ?: 0L
                val mesesAhorrados = resultado.mesesAhorradosPorAbonos[deuda.id] ?: 0

                DeudaSimuladaItem(
                    deuda = deuda,
                    esPausada = esPausada,
                    abonoActual = abonoActual,
                    mesesAhorrados = mesesAhorrados,
                    onTogglePausa = { onTogglePausa(deuda.id) },
                    onAbonoChange = { monto -> onSetAbono(deuda.id, monto) }
                )
            }
        }
    }
}

@Composable
fun GastoSimuladoItem(
    gasto: GastoSimulado,
    onToggle: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (gasto.incluido) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.concepto,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatGuaranies(gasto.monto),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (gasto.incluido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!gasto.incluido) {
                    Text(
                        text = "Excluido del cálculo temporalmente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = gasto.incluido,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEliminar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar gasto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SemaforoViabilidadCard(resultado: CapacidadPagoResponse) {
    val semaforo = resultado.semaforoEnum

    val containerColor = when (semaforo) {
        SemaforoViabilidad.SUPERAVIT -> MaterialTheme.colorScheme.primaryContainer
        SemaforoViabilidad.AJUSTADO -> MaterialTheme.colorScheme.tertiaryContainer
        SemaforoViabilidad.DEFICIT -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (semaforo) {
        SemaforoViabilidad.SUPERAVIT -> MaterialTheme.colorScheme.onPrimaryContainer
        SemaforoViabilidad.AJUSTADO -> MaterialTheme.colorScheme.onTertiaryContainer
        SemaforoViabilidad.DEFICIT -> MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = when (semaforo) {
        SemaforoViabilidad.SUPERAVIT -> Icons.Default.CheckCircle
        SemaforoViabilidad.AJUSTADO -> Icons.Default.Warning
        SemaforoViabilidad.DEFICIT -> Icons.Default.Warning
    }
    val titulo = when (semaforo) {
        SemaforoViabilidad.SUPERAVIT -> "Superávit Holgado"
        SemaforoViabilidad.AJUSTADO -> "Capacidad Ajustada"
        SemaforoViabilidad.DEFICIT -> "Déficit de Cobertura"
    }
    val descripcion = when (semaforo) {
        SemaforoViabilidad.SUPERAVIT -> "El fondo disponible cubre con holgura todas las cuotas y gastos del mes, permitiendo abonos extraordinarios."
        SemaforoViabilidad.AJUSTADO -> "Los egresos se cubren, pero el margen remanente es inferior al 15%. Precaución ante gastos imprevistos."
        SemaforoViabilidad.DEFICIT -> "Los compromisos superan tu fondo mensual disponible. Pausa cuotas, reduce gastos o aporta ingresos para evitar déficit."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun MetricasCapacidadCard(resultado: CapacidadPagoResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fondo Disponible Mes:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatGuaranies(resultado.fondoDisponibleMes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Compromisos Cuotas:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatGuaranies(resultado.compromisoTotalMes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            if (resultado.totalGastosProyectados > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gastos Proyectados:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatGuaranies(resultado.totalGastosProyectados), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val remanenteColor = if (resultado.capacidadNeta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text("Margen Remanente:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(formatGuaranies(resultado.capacidadNeta), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = remanenteColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DTI Proyectado (Compromiso %):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format("%.1f%%", resultado.dtiProyectado),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (resultado.dtiProyectado > 85.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun IngresoExtraordinarioCard(
    ajusteIngreso: Long,
    onSetIngresoExtra: (Long) -> Unit
) {
    var textoIngreso by remember(ajusteIngreso) {
        mutableStateOf(if (ajusteIngreso > 0L) ajusteIngreso.toString() else "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Simular Ingreso Extraordinario",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aguinaldo, comisiones o ventas excepcionales para este mes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = textoIngreso,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        textoIngreso = input
                        onSetIngresoExtra(input.toLongOrNull() ?: 0L)
                    }
                },
                label = { Text("Ingreso adicional (Gs.)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DeudaSimuladaItem(
    deuda: Obligacion,
    esPausada: Boolean,
    abonoActual: Long,
    mesesAhorrados: Int,
    onTogglePausa: () -> Unit,
    onAbonoChange: (Long) -> Unit
) {
    var textoAbono by remember(abonoActual) {
        mutableStateOf(if (abonoActual > 0L) abonoActual.toString() else "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esPausada) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deuda.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cuota base: ${formatGuaranies(deuda.cuotaReferenciaGs)} / mes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (esPausada) "Pausar este mes" else "Pagar cuota",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (esPausada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = !esPausada,
                        onCheckedChange = { onTogglePausa() }
                    )
                }
            }

            if (esPausada) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Pausada en simulación: se liberan ${formatGuaranies(deuda.cuotaReferenciaGs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textoAbono,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            textoAbono = input
                            onAbonoChange(input.toLongOrNull() ?: 0L)
                        }
                    },
                    label = { Text("Abono extraordinario a capital (Gs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mesesAhorrados > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "¡Ahorras $mesesAhorrados cuota(s) de deuda!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgregarGastoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var montoTexto by remember { mutableStateOf("") }
    var errorMonto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Gasto Proyectado", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ingresa el concepto y monto del gasto que deseas simular para este período.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto o motivo") },
                    placeholder = { Text("Ej: Mantenimiento vehículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            montoTexto = input
                            errorMonto = false
                        }
                    },
                    label = { Text("Monto estimado (Gs.)") },
                    isError = errorMonto,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoTexto.toLongOrNull() ?: 0L
                    if (concepto.isNotBlank() && monto > 0L) {
                        onConfirm(concepto, monto)
                    } else {
                        errorMonto = monto <= 0L
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun GuardarEscenarioDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar Escenario", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Asigna un nombre para recordar este escenario de proyección (incluye deudas pausadas, abonos, ingresos y gastos simulados).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del escenario") },
                    placeholder = { Text("Ej: Fin de Año / Vacaciones") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onConfirm(nombre)
                    }
                },
                enabled = nombre.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscenariosBottomSheet(
    escenarios: List<EscenarioCapacidad>,
    onDismiss: () -> Unit,
    onGuardarNuevo: () -> Unit,
    onCargar: (EscenarioCapacidad) -> Unit,
    onEliminar: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Escenarios de Simulación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(onClick = onGuardarNuevo) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar actual", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (escenarios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes escenarios guardados aún.\nConfigura tu simulación y presiona 'Guardar actual'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(escenarios, key = { it.id }) { escenario ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = escenario.nombre,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Actualizado: ${dateFormat.format(Date(escenario.fechaActualizacion))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${escenario.gastosAdHoc.size} gasto(s) • ${escenario.deudasPausadasIds.size} pausada(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onCargar(escenario) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Cargar", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { onEliminar(escenario.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar escenario",
                                            tint = MaterialTheme.colorScheme.error
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
}

@Preview(showBackground = true)
@Composable
fun CapacidadPagoScreenPreview() {
    GammaTheme {
        CapacidadPagoContent(
            resultado = CapacidadPagoResponse(
                compromisoTotalMes = 1350000L,
                fondoDisponibleMes = 3500000L,
                totalGastosProyectados = 500000L,
                capacidadNeta = 1150000L,
                semaforo = "SUPERAVIT",
                dtiProyectado = 67.1,
                mesesAhorradosPorAbonos = mapOf("1" to 2)
            ),
            deudasActivas = listOf(
                Obligacion(
                    id = "1",
                    nombre = "Préstamo Personal",
                    montoInicial = 5000000L,
                    saldoActual = 2500000L,
                    cuotaReferenciaGs = 500000L,
                    cuotasRestantes = 5,
                    estado = "Activo"
                ),
                Obligacion(
                    id = "2",
                    nombre = "Tarjeta Financiada",
                    montoInicial = 3000000L,
                    saldoActual = 1200000L,
                    cuotaReferenciaGs = 850000L,
                    cuotasRestantes = 2,
                    estado = "Activo"
                )
            ),
            deudasPausadas = emptySet(),
            abonosExtra = mapOf("1" to 1000000L),
            ajusteIngreso = 500000L,
            gastosSimulados = listOf(
                GastoSimulado(id = "g1", concepto = "Mantenimiento Camioneta", monto = 500000L, incluido = true),
                GastoSimulado(id = "g2", concepto = "Curso de Capacitación", monto = 300000L, incluido = false)
            ),
            onTogglePausa = {},
            onSetAbono = { _, _ -> },
            onSetIngresoExtra = {},
            onToggleGasto = {},
            onEliminarGasto = {},
            onOpenAgregarGasto = {}
        )
    }
}

@Preview(showBackground = true, name = "Capacidad Pago - Déficit")
@Composable
fun CapacidadPagoDeficitPreview() {
    GammaTheme {
        CapacidadPagoContent(
            resultado = CapacidadPagoResponse(
                compromisoTotalMes = 2500000L,
                fondoDisponibleMes = 1800000L,
                totalGastosProyectados = 600000L,
                capacidadNeta = -1300000L,
                semaforo = "DEFICIT",
                dtiProyectado = 172.2,
                mesesAhorradosPorAbonos = emptyMap()
            ),
            deudasActivas = listOf(
                Obligacion(
                    id = "1",
                    nombre = "Préstamo Bancario",
                    montoInicial = 10000000L,
                    saldoActual = 8000000L,
                    cuotaReferenciaGs = 1500000L,
                    cuotasRestantes = 8,
                    estado = "Activo"
                ),
                Obligacion(
                    id = "2",
                    nombre = "Cuota Vehículo",
                    montoInicial = 15000000L,
                    saldoActual = 9000000L,
                    cuotaReferenciaGs = 1000000L,
                    cuotasRestantes = 9,
                    estado = "Activo"
                )
            ),
            deudasPausadas = setOf("2"),
            abonosExtra = emptyMap(),
            ajusteIngreso = 0L,
            gastosSimulados = listOf(
                GastoSimulado(id = "g1", concepto = "Reparación Urgente", monto = 600000L, incluido = true)
            ),
            onTogglePausa = {},
            onSetAbono = { _, _ -> },
            onSetIngresoExtra = {},
            onToggleGasto = {},
            onEliminarGasto = {},
            onOpenAgregarGasto = {}
        )
    }
}
