package com.gamma.mobile.presentation.transacciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.presentation.dashboard.formatGuaranies

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
            TopAppBar(title = { Text("Libro Diario") })
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
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                filters.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilter == title,
                        onClick = { selectedFilter = title },
                        text = { Text(title) }
                    )
                }
            }

            // Lista de transacciones
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMovimientos) { mov ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(mov.categoria, fontWeight = FontWeight.Bold)
                                Text(mov.fecha, style = MaterialTheme.typography.bodySmall)
                                Badge { Text(mov.comprobante) }
                            }
                            Text(
                                formatGuaranies(mov.total),
                                color = if (mov.tipo == "Ingreso") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
