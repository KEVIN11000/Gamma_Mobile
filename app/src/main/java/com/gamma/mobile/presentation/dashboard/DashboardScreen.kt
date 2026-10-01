package com.gamma.mobile.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    avisos: List<Aviso>,
    onNuevaTransaccion: () -> Unit,
    onNuevoRecordatorio: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamma Dashboard") }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onNuevoRecordatorio,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Recordatorio")
                }
                FloatingActionButton(onClick = onNuevaTransaccion) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Transacción")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tarjeta de Posición Financiera
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Margen Libre Disponible", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatGuaranies(summary.margenLibreDisponible),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saldo Acumulado: ${formatGuaranies(summary.saldoAcumulado)}")
                    Text("Posición IVA: ${formatGuaranies(summary.posicionIva)} (${if (summary.esDebitoFiscal) "Débito" else "Crédito"})")
                }
            }

            // Banner de Alertas Inminentes
            Text("Alertas Inminentes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(avisos) { aviso ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(aviso.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Faltan ${aviso.diasRestantes} días", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

fun formatGuaranies(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "PY"))
    return "${formatter.format(amount)} ₲"
}
