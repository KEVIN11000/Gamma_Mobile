package com.gamma.mobile.presentation.presupuesto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gamma.mobile.data.remote.dto.GastoFijoItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.utils.formatGuaranies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoScreen(
    viewModel: PresupuestoViewModel,
    showTopBar: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val feedbackIsError by viewModel.feedbackIsError.collectAsState()

    var showEditSalarioDialog by remember { mutableStateOf(false) }
    var showCreateFijoDialog by remember { mutableStateOf(false) }
    var gastoFijoToEdit by remember { mutableStateOf<GastoFijoItemDto?>(null) }
    var gastoFijoToDelete by remember { mutableStateOf<String?>(null) }
    var techoToEdit by remember { mutableStateOf<TechoPresupuestarioItemDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Presupuesto Base", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadPresupuesto(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateFijoDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Gasto Fijo") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PresupuestoUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PresupuestoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPresupuesto(forceRefresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is PresupuestoUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
                    ) {
                        // 1. Tarjeta Resumen Estructural
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Ingreso / Salario Base",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (state.puedeEditarSalario) {
                                            IconButton(
                                                onClick = { showEditSalarioDialog = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar Salario",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        formatGuaranies(state.salarioBase),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "Total Gastos Fijos",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                formatGuaranies(state.totalGastosFijos),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "Margen Estructural",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                formatGuaranies(state.margenEstructural),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Sección Gastos Fijos por Categoría
                        item {
                            Text(
                                "Gastos Fijos Estructurales",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.categoriasFijos.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No hay gastos fijos registrados.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.categoriasFijos.entries.toList(), key = { it.key }) { entry ->
                                CategoriaFijosCard(
                                    categoria = entry.key,
                                    items = entry.value,
                                    puedeEliminar = state.puedeEliminarFijos,
                                    onEditItem = { gastoFijoToEdit = it },
                                    onDeleteItem = { gastoFijoToDelete = it.concepto }
                                )
                            }
                        }

                        // 3. Sección Techos de Gastos Variables
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Monitor de Techos Presupuestarios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.techosVariables.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No se encontraron techos configurados.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.techosVariables, key = { it.categoria }) { techo ->
                                TechoCardItem(
                                    techo = techo,
                                    puedeEditar = state.puedeEditarTechos,
                                    onEditClick = { techoToEdit = techo }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos de Mutación
    if (showEditSalarioDialog && uiState is PresupuestoUiState.Success) {
        val currSalario = (uiState as PresupuestoUiState.Success).salarioBase
        EditarSalarioDialog(
            salarioActual = currSalario,
            onDismiss = { showEditSalarioDialog = false },
            onGuardar = { nuevo ->
                viewModel.actualizarSalarioBase(nuevo)
                showEditSalarioDialog = false
            }
        )
    }

    if (showCreateFijoDialog) {
        CrearGastoFijoDialog(
            onDismiss = { showCreateFijoDialog = false },
            onGuardar = { cat, conc, monto ->
                viewModel.crearGastoFijo(cat, conc, monto)
                showCreateFijoDialog = false
            }
        )
    }

    if (gastoFijoToEdit != null) {
        EditarGastoFijoDialog(
            item = gastoFijoToEdit!!,
            onDismiss = { gastoFijoToEdit = null },
            onGuardar = { conc, monto ->
                viewModel.actualizarGastoFijo(conc, monto)
                gastoFijoToEdit = null
            }
        )
    }

    if (gastoFijoToDelete != null) {
        AlertDialog(
            onDismissRequest = { gastoFijoToDelete = null },
            title = { Text("Eliminar Gasto Fijo") },
            text = { Text("¿Deseas eliminar '$gastoFijoToDelete' de la planilla Presupuesto_Base?") },
            confirmButton = {
                Button(
                    onClick = {
                        val conc = gastoFijoToDelete!!
                        viewModel.eliminarGastoFijo(conc)
                        gastoFijoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { gastoFijoToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (techoToEdit != null) {
        EditarTechoDialog(
            techo = techoToEdit!!,
            onDismiss = { techoToEdit = null },
            onGuardar = { cat, limite, umbral ->
                viewModel.actualizarTecho(cat, limite, umbral)
                techoToEdit = null
            }
        )
    }
}

@Composable
fun CategoriaFijosCard(
    categoria: String,
    items: List<GastoFijoItemDto>,
    puedeEliminar: Boolean,
    onEditItem: (GastoFijoItemDto) -> Unit,
    onDeleteItem: (GastoFijoItemDto) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val totalCat = items.sumOf { it.monto }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        categoria,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    formatGuaranies(totalCat),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.concepto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    formatGuaranies(item.monto),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (puedeEliminar) {
                                    IconButton(
                                        onClick = { onDeleteItem(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
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

@Composable
fun TechoCardItem(
    techo: TechoPresupuestarioItemDto,
    puedeEditar: Boolean,
    onEditClick: () -> Unit
) {
    val progress = (techo.porcentaje / 100.0).toFloat().coerceIn(0f, 1f)
    val colorSemaforo = when (techo.estado.uppercase()) {
        "EXCEDIDO" -> MaterialTheme.colorScheme.error
        "ALERTA" -> Color(0xFFE65100) // Naranja intenso
        else -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colorSemaforo)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        techo.categoria,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${techo.porcentaje}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorSemaforo
                    )
                    if (puedeEditar) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar Techo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorSemaforo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gastado: ${formatGuaranies(techo.consumido)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Techo: ${formatGuaranies(techo.limiteMensual)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (techo.mensaje.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    techo.mensaje,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorSemaforo
                )
            }
        }
    }
}

@Composable
fun EditarSalarioDialog(
    salarioActual: Long,
    onDismiss: () -> Unit,
    onGuardar: (Long) -> Unit
) {
    var salarioText by remember { mutableStateOf(salarioActual.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Salario Base") },
        text = {
            Column {
                Text(
                    "Actualiza el salario mensual de referencia configurado en Presupuesto_Base.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = salarioText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) salarioText = it },
                    label = { Text("Salario Base (₲)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = salarioText.toLongOrNull() ?: 0L
                    onGuardar(monto)
                },
                enabled = (salarioText.toLongOrNull() ?: 0L) > 0L
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

@Composable
fun CrearGastoFijoDialog(
    onDismiss: () -> Unit,
    onGuardar: (categoria: String, concepto: String, monto: Long) -> Unit
) {
    var categoria by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    var montoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto Fijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría (ej: Vivienda, Servicios)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (ej: Alquiler, Internet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) montoText = it },
                    label = { Text("Monto Mensual (₲)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoText.toLongOrNull() ?: 0L
                    onGuardar(categoria.trim(), concepto.trim(), m)
                },
                enabled = categoria.isNotBlank() && concepto.isNotBlank() && (montoText.toLongOrNull() ?: 0L) > 0L
            ) {
                Text("Registrar")
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
fun EditarGastoFijoDialog(
    item: GastoFijoItemDto,
    onDismiss: () -> Unit,
    onGuardar: (concepto: String, nuevoMonto: Long) -> Unit
) {
    var montoText by remember { mutableStateOf(item.monto.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar '${item.concepto}'") },
        text = {
            Column {
                Text("Ingresa el nuevo monto mensual para este gasto estructural:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) montoText = it },
                    label = { Text("Nuevo Monto (₲)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoText.toLongOrNull() ?: 0L
                    onGuardar(item.concepto, m)
                },
                enabled = (montoText.toLongOrNull() ?: 0L) > 0L
            ) {
                Text("Actualizar")
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
fun EditarTechoDialog(
    techo: TechoPresupuestarioItemDto,
    onDismiss: () -> Unit,
    onGuardar: (categoria: String, limiteMensual: Long?, umbralAlerta: Int?) -> Unit
) {
    var limiteText by remember { mutableStateOf(techo.limiteMensual.toString()) }
    var umbralText by remember { mutableStateOf(techo.umbralAlerta.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Techo: ${techo.categoria}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = limiteText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) limiteText = it },
                    label = { Text("Límite Mensual Máximo (₲)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = umbralText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) umbralText = it },
                    label = { Text("Umbral de Alerta (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lim = limiteText.toLongOrNull()
                    val umb = umbralText.toIntOrNull()
                    onGuardar(techo.categoria, lim, umb)
                },
                enabled = (limiteText.toLongOrNull() ?: 0L) > 0L
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
