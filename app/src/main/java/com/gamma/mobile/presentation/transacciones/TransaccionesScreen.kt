package com.gamma.mobile.presentation.transacciones

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.utils.LocalDiarioPdfGenerator
import com.gamma.mobile.utils.formatGuaranies
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    viewModel: TransaccionesViewModel,
    onScanQr: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val pdfExportMessage by viewModel.pdfExportMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadMovimientos(forceRefresh = true)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is TransaccionesUiState.Success && !(uiState as TransaccionesUiState.Success).isRefreshing) {
            pullToRefreshState.endRefresh()
        }
        if (uiState is TransaccionesUiState.Error || uiState is TransaccionesUiState.Empty) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(pdfExportMessage) {
        pdfExportMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissPdfExportMessage()
        }
    }

    val isTableView = when (val state = uiState) {
        is TransaccionesUiState.Success -> state.isTableView
        is TransaccionesUiState.Empty -> state.isTableView
        else -> false
    }

    val filtroActual = when (val state = uiState) {
        is TransaccionesUiState.Success -> state.filtroActual
        is TransaccionesUiState.Empty -> state.filtroActual
        else -> FiltroTipo.TODOS
    }

    val fechaDesde = when (val state = uiState) {
        is TransaccionesUiState.Success -> state.fechaDesde
        is TransaccionesUiState.Empty -> state.fechaDesde
        else -> null
    }

    val fechaHasta = when (val state = uiState) {
        is TransaccionesUiState.Success -> state.fechaHasta
        is TransaccionesUiState.Empty -> state.fechaHasta
        else -> null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Libro Diario", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    // Botón alternar vista: Tarjetas <-> Planilla
                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        enabled = uiState is TransaccionesUiState.Success || uiState is TransaccionesUiState.Empty
                    ) {
                        Icon(
                            imageVector = if (isTableView) Icons.Default.ViewAgenda else Icons.Default.TableChart,
                            contentDescription = if (isTableView) "Ver Tarjetas" else "Ver Planilla"
                        )
                    }

                    // Botón exportar PDF local
                    IconButton(
                        onClick = {
                            viewModel.exportarPdf(context) { file ->
                                LocalDiarioPdfGenerator.abrirPdf(context, file)
                            }
                        },
                        enabled = !isExportingPdf && uiState is TransaccionesUiState.Success
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar PDF"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanQr,
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("Escanear Factura") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            when (val state = uiState) {
                is TransaccionesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TransaccionesUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadMovimientos(forceRefresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is TransaccionesUiState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FilterTabs(
                            selectedFiltro = filtroActual,
                            onFilterSelected = { viewModel.setFiltro(it) }
                        )
                        RangoFechasBar(
                            fechaDesde = fechaDesde,
                            fechaHasta = fechaHasta,
                            onSelectDesde = { nuevaDesde -> viewModel.setRangoFechas(nuevaDesde, fechaHasta) },
                            onSelectHasta = { nuevaHasta -> viewModel.setRangoFechas(fechaDesde, nuevaHasta) },
                            onLimpiar = { viewModel.limpiarRangoFechas() }
                        )
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No hay movimientos registrados para este filtro.")
                                Spacer(modifier = Modifier.height(8.dp))
                                if (fechaDesde != null || fechaHasta != null || filtroActual != FiltroTipo.TODOS) {
                                    OutlinedButton(onClick = {
                                        viewModel.limpiarRangoFechas()
                                        viewModel.setFiltro(FiltroTipo.TODOS)
                                    }) {
                                        Text("Restablecer Filtros")
                                    }
                                }
                            }
                        }
                    }
                }
                is TransaccionesUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FilterTabs(
                            selectedFiltro = state.filtroActual,
                            onFilterSelected = { viewModel.setFiltro(it) }
                        )

                        RangoFechasBar(
                            fechaDesde = state.fechaDesde,
                            fechaHasta = state.fechaHasta,
                            onSelectDesde = { nuevaDesde -> viewModel.setRangoFechas(nuevaDesde, state.fechaHasta) },
                            onSelectHasta = { nuevaHasta -> viewModel.setRangoFechas(state.fechaDesde, nuevaHasta) },
                            onLimpiar = { viewModel.limpiarRangoFechas() }
                        )

                        if (state.isTableView) {
                            // Vista de Planilla (Hoja de cálculo en pantalla)
                            PlanillaTableView(
                                movimientos = state.movimientosFiltrados,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            // Vista de Tarjetas agrupadas
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                state.movimientosAgrupados.forEach { (fecha, listaMovimientos) ->
                                    item(key = fecha) {
                                        Text(
                                            text = fecha,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    items(listaMovimientos, key = { it.id }) { mov ->
                                        MovimientoItem(mov)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun FilterTabs(
    selectedFiltro: FiltroTipo,
    onFilterSelected: (FiltroTipo) -> Unit
) {
    val filters = listOf(
        FiltroTipo.TODOS to "Todos",
        FiltroTipo.INGRESO to "Ingresos",
        FiltroTipo.GASTO to "Gastos"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filtro, title) ->
            FilterChip(
                selected = selectedFiltro == filtro,
                onClick = { onFilterSelected(filtro) },
                label = { Text(title, fontWeight = if (selectedFiltro == filtro) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = if (selectedFiltro == filtro) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun RangoFechasBar(
    fechaDesde: String?,
    fechaHasta: String?,
    onSelectDesde: (String) -> Unit,
    onSelectHasta: (String) -> Unit,
    onLimpiar: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val showDatePickerDesde = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val fecha = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onSelectDesde(fecha)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showDatePickerHasta = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val fecha = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onSelectHasta(fecha)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = showDatePickerDesde,
            label = {
                Text(
                    text = if (fechaDesde.isNullOrBlank()) "Desde" else "Desde: $fechaDesde",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )

        AssistChip(
            onClick = showDatePickerHasta,
            label = {
                Text(
                    text = if (fechaHasta.isNullOrBlank()) "Hasta" else "Hasta: $fechaHasta",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )

        if (!fechaDesde.isNullOrBlank() || !fechaHasta.isNullOrBlank()) {
            IconButton(
                onClick = onLimpiar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpiar rango de fechas",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlanillaTableView(
    movimientos: List<Movimiento>,
    modifier: Modifier = Modifier
) {
    val totalIngresos = remember(movimientos) {
        movimientos.filter { it.tipo.equals("Ingreso", ignoreCase = true) }.sumOf { it.total }
    }
    val totalGastos = remember(movimientos) {
        movimientos.filter { it.tipo.equals("Gasto", ignoreCase = true) }.sumOf { it.total }
    }
    val neto = totalIngresos - totalGastos

    Column(modifier = modifier) {
        // Resumen financiero de la planilla
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ingresos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatGuaranies(totalIngresos),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("Gastos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatGuaranies(totalGastos),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column {
                    Text("Neto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatGuaranies(neto),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (neto >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Tabla con scroll horizontal y vertical
        val horizontalScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Encabezado de la planilla
            Row(
                modifier = Modifier
                    .width(620.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fecha", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Tipo", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Categoría", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Comprobante", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Total (Gs.)", modifier = Modifier.width(150.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            // Filas
            LazyColumn(
                modifier = Modifier
                    .width(620.dp)
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(movimientos, key = { it.id }) { mov ->
                    val isIngreso = mov.tipo.equals("Ingreso", ignoreCase = true)
                    val montoColor = if (isIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mov.fecha, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                        Text(
                            mov.tipo,
                            modifier = Modifier.width(70.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = montoColor
                        )
                        Text(
                            mov.categoria,
                            modifier = Modifier.width(170.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            mov.comprobante,
                            modifier = Modifier.width(130.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatGuaranies(mov.total),
                            modifier = Modifier.width(150.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = montoColor
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun MovimientoItem(mov: Movimiento) {
    val isIngreso = mov.tipo == "Ingreso"
    val itemColor = if (isIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIngreso) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = mov.tipo,
                        tint = itemColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(mov.categoria, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(mov.fecha, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            mov.comprobante,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Text(
                formatGuaranies(mov.total),
                color = itemColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
