package com.gamma.mobile.presentation.transacciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaTransaccionScreen(
    onSave: () -> Unit,
    onBack: () -> Unit,
    onProcessIA: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("IA", "Manual")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Transacción") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTabIndex == 0) {
                    CargaAsistidaIA(onProcessIA)
                } else {
                    CargaManual(onSave)
                }
            }
        }
    }
}

@Composable
fun CargaAsistidaIA(onProcessIA: (String) -> Unit) {
    var frase by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = frase,
            onValueChange = { frase = it },
            label = { Text("Describe el gasto o ingreso...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onProcessIA(frase) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Interpretar con IA")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ejemplo: Compré repuestos por 250000 con factura crédito fiscal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CargaManual(onSave: () -> Unit) {
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Gasto") }
    var iva by remember { mutableStateOf("10%") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = monto,
            onValueChange = { monto = it },
            label = { Text("Monto (₲)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = categoria,
            onValueChange = { categoria = it },
            label = { Text("Categoría") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Tipo de Movimiento")
        Row {
            RadioButton(selected = tipo == "Gasto", onClick = { tipo = "Gasto" })
            Text("Gasto", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = tipo == "Ingreso", onClick = { tipo = "Ingreso" })
            Text("Ingreso", modifier = Modifier.align(Alignment.CenterVertically))
        }

        Text("Tasa de IVA")
        Row {
            listOf("10%", "5%", "Exento").forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = iva == option, onClick = { iva = option })
                    Text(option)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar Transacción")
        }
    }
}
