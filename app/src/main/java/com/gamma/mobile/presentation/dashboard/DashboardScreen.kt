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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.utils.formatGuaranies
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNuevaTransaccion: () -> Unit,
    onNuevoRecordatorio: () -> Unit,
    onNavigateToAvisos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamma Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    }
                }
                is DashboardUiState.Empty -> {
                    Text(
                        "No hay datos registrados aún.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        summary = state.summary, 
                        avisos = state.avisos,
                        onNavigateToAvisos = onNavigateToAvisos
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    summary: DashboardSummary, 
    avisos: List<com.gamma.mobile.presentation.dashboard.AvisoUiModel>,
    onNavigateToAvisos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Tarjeta de Posición Financiera
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
    }
}


