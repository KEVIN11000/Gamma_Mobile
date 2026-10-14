package com.gamma.mobile.presentation.transacciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.utils.formatGuaranies
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    viewModel: TransaccionesViewModel,
    onScanQr: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Manage pull to refresh state changes
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadMovimientos(forceRefresh = true)
        }
    }
    
    // Auto-hide the indicator if not refreshing from the VM
    LaunchedEffect(uiState) {
        if (uiState is TransaccionesUiState.Success && !(uiState as TransaccionesUiState.Success).isRefreshing) {
            pullToRefreshState.endRefresh()
        }
        if (uiState is TransaccionesUiState.Error || uiState is TransaccionesUiState.Empty) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libro Diario", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onScanQr) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear SIFEN")
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
                        Button(onClick = { viewModel.loadMovimientos(forceRefresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is TransaccionesUiState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FilterTabs(
                            selectedFiltro = FiltroTipo.TODOS,
                            onFilterSelected = { viewModel.setFiltro(it) }
                        )
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay movimientos registrados.")
                        }
                    }
                }
                is TransaccionesUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FilterTabs(
                            selectedFiltro = state.filtroActual,
                            onFilterSelected = { viewModel.setFiltro(it) }
                        )
                        
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
