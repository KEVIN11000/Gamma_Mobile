package com.gamma.mobile.presentation.transacciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.utils.formatGuaranies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    movimientos: List<Movimiento>
) {
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Ingreso", "Gasto", "Deudas")

    val filteredMovimientos = if (selectedFilter == "Todos") {
        movimientos
    } else if (selectedFilter == "Deudas") {
        movimientos.filter { it.categoria == "Deuda" }
    } else {
        movimientos.filter { it.tipo == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libro Diario", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chips Filter
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                edgePadding = 16.dp,
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                filters.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilter == title,
                        onClick = { selectedFilter = title },
                        text = { Text(title, fontWeight = if (selectedFilter == title) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Lista de transacciones
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMovimientos) { mov ->
                    val isIngreso = mov.tipo == "Ingreso"
                    val itemColor = if (isIngreso) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

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
            }
        }
    }
}
