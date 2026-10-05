package com.gamma.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.presentation.dashboard.DashboardScreen
import com.gamma.mobile.presentation.obligaciones.ObligacionesScreen
import com.gamma.mobile.presentation.transacciones.NuevaTransaccionScreen
import com.gamma.mobile.presentation.transacciones.TransaccionesScreen
import dagger.hilt.android.AndroidEntryPoint

enum class Screen {
    Dashboard,
    Transacciones,
    NuevaTransaccion,
    Obligaciones
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    GammaApp()
                }
            }
        }
    }
}

@Composable
fun GammaApp() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    val sampleSummary = DashboardSummary(
        margenLibreDisponible = 5000000,
        saldoAcumulado = 12000000,
        posicionIva = 450000,
        esDebitoFiscal = true
    )

    val sampleAvisos = listOf(
        Aviso(id = "1", titulo = "I.V.A. Mensual", diasRestantes = 3),
        Aviso(id = "2", titulo = "Pago Cuota Préstamo", diasRestantes = 5)
    )

    val sampleMovimientos = listOf(
        Movimiento(
            id = "1",
            fecha = "2026-04-01",
            tipo = "Ingreso",
            categoria = "Venta Servicios",
            total = 1500000,
            comprobante = "Virtual Legal"
        ),
        Movimiento(
            id = "2",
            fecha = "2026-04-02",
            tipo = "Gasto",
            categoria = "Repuestos",
            total = 250000,
            comprobante = "Física Legal"
        )
    )

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.NuevaTransaccion) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Transacciones,
                        onClick = { currentScreen = Screen.Transacciones },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transacciones") },
                        label = { Text("Diario") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Obligaciones,
                        onClick = { currentScreen = Screen.Obligaciones },
                        icon = { Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Obligaciones") },
                        label = { Text("Obligaciones") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    summary = sampleSummary,
                    avisos = sampleAvisos,
                    onNuevaTransaccion = { currentScreen = Screen.NuevaTransaccion },
                    onNuevoRecordatorio = { currentScreen = Screen.Obligaciones }
                )
                Screen.Transacciones -> TransaccionesScreen(
                    movimientos = sampleMovimientos
                )
                Screen.NuevaTransaccion -> NuevaTransaccionScreen(
                    onSave = { currentScreen = Screen.Transacciones },
                    onBack = { currentScreen = Screen.Dashboard },
                    onProcessIA = { currentScreen = Screen.Transacciones }
                )
                Screen.Obligaciones -> ObligacionesScreen()
            }
        }
    }
}
