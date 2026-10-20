package com.gamma.mobile.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.utils.formatGuaranies
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Work
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNuevaTransaccion: () -> Unit,
    onNuevoRecordatorio: () -> Unit,
    onNavigateToAvisos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val asistenciaMessage by viewModel.asistenciaMessage.collectAsState()
    val asistenciaIsError by viewModel.asistenciaIsError.collectAsState()
    val isMarkingAsistencia by viewModel.isMarkingAsistencia.collectAsState()
    val modoMarcacion by viewModel.modoMarcacion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamma Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaTransaccion,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nueva Transacción") },
                text = { Text("Transacción") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        if (state.isOffline) {
                            Text("Estás offline. Revisa tu conexión.", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDashboard(forceRefresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is DashboardUiState.Empty -> {
                    DashboardContent(
                        summary = DashboardSummary(0L, 0L, 0L, false),
                        avisos = emptyList(),
                        onNavigateToAvisos = onNavigateToAvisos,
                        onMarcarAsistencia = { viewModel.marcarAsistencia() },
                        isMarkingAsistencia = isMarkingAsistencia,
                        asistenciaMessage = asistenciaMessage,
                        asistenciaIsError = asistenciaIsError,
                        modoMarcacion = modoMarcacion,
                        onModoMarcacionChange = { viewModel.setModoMarcacion(it) },
                        onDismissAsistenciaMessage = { viewModel.dismissAsistenciaMessage() }
                    )
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        summary = state.summary, 
                        avisos = state.avisos,
                        onNavigateToAvisos = onNavigateToAvisos,
                        onMarcarAsistencia = { viewModel.marcarAsistencia() },
                        isMarkingAsistencia = isMarkingAsistencia,
                        asistenciaMessage = asistenciaMessage,
                        asistenciaIsError = asistenciaIsError,
                        modoMarcacion = modoMarcacion,
                        onModoMarcacionChange = { viewModel.setModoMarcacion(it) },
                        onDismissAsistenciaMessage = { viewModel.dismissAsistenciaMessage() }
                    )
                }
            }
        }

        // Espacio para evitar solapamiento con el ExtendedFloatingActionButton ("Transacción")
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
fun DashboardContent(
    summary: DashboardSummary, 
    avisos: List<com.gamma.mobile.presentation.dashboard.AvisoUiModel>,
    onNavigateToAvisos: () -> Unit,
    onMarcarAsistencia: () -> Unit = {},
    isMarkingAsistencia: Boolean = false,
    asistenciaMessage: String? = null,
    asistenciaIsError: Boolean = false,
    modoMarcacion: String = DashboardViewModel.MODO_NORMAL,
    onModoMarcacionChange: (String) -> Unit = {},
    onDismissAsistenciaMessage: () -> Unit = {}
) {
    val dayOfWeek = remember { LocalDate.now().dayOfWeek }
    val esDiaLaboral = remember(dayOfWeek) {
        dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Atajo de Marcación Laboral (Visible de Lunes a Viernes)
        if (esDiaLaboral) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Marcación Laboral de Hoy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Turno Lunes a Viernes",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = onMarcarAsistencia,
                            enabled = !isMarkingAsistencia,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (isMarkingAsistencia) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcando...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Marcar")
                            }
                        }
                    }

                    // Selector de modo de marcación
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modo:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = modoMarcacion == DashboardViewModel.MODO_NORMAL,
                            onClick = { onModoMarcacionChange(DashboardViewModel.MODO_NORMAL) },
                            label = { Text("Normal", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        FilterChip(
                            selected = modoMarcacion == DashboardViewModel.MODO_DIRECTO,
                            onClick = { onModoMarcacionChange(DashboardViewModel.MODO_DIRECTO) },
                            label = { Text("Directo", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    if (!asistenciaMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val bannerColor = if (asistenciaIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                        val textColor = if (asistenciaIsError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = bannerColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = asistenciaMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissAsistenciaMessage) {
                                    Text("OK", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Tarjeta de Balance Actual (Saldo Acumulado)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Balance Actual (Saldo Acumulado)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Saldo total acumulado en caja y cuentas",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatGuaranies(summary.saldoAcumulado),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (summary.saldoAcumulado >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        // Tarjeta de Margen Libre y Posición IVA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Margen Libre (mes en curso)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Dinero disponible tras descontar el presupuesto base y las deudas",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatGuaranies(summary.margenLibreDisponible),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Posición IVA (Mes)", style = MaterialTheme.typography.labelMedium)
                        val ivaColor = if (summary.esDebitoFiscal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        Text(
                            "${formatGuaranies(summary.posicionIva)} ${if (summary.esDebitoFiscal) "a pagar (Débito)" else "a favor (Crédito)"}", 
                            fontWeight = FontWeight.Bold,
                            color = ivaColor
                        )
                    }
                }
            }
        }

        // Tarjeta Eventos de la semana
        Text(
            "Eventos de la semana", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        if (avisos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay eventos en los próximos 7 días", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(avisos) { aviso ->
                    val cardColor = if (aviso.esCritico) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    val contentColor = if (aviso.esCritico) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = cardColor,
                            contentColor = contentColor
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.clickable { onNavigateToAvisos() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Aviso próximo",
                                tint = if (aviso.esCritico) MaterialTheme.colorScheme.error else contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(aviso.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(aviso.textoTiempo, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Espacio para evitar solapamiento con el ExtendedFloatingActionButton ("Transacción")
        Spacer(modifier = Modifier.height(96.dp))
    }
}


