package com.gamma.mobile.presentation.obligaciones

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligacionesScreen() {
    var showSimuladorModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Obligaciones") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Gestión de Obligaciones", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showSimuladorModal = true }) {
                Text("Abrir Simulador de Cuotas")
            }
        }
    }

    if (showSimuladorModal) {
        SimuladorModal(onDismiss = { showSimuladorModal = false })
    }
}
