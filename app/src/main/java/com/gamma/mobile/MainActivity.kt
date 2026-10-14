package com.gamma.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.presentation.compromisos.CompromisosScreen
import com.gamma.mobile.presentation.dashboard.DashboardScreen
import com.gamma.mobile.presentation.obligaciones.ObligacionesScreen
import com.gamma.mobile.presentation.transacciones.NuevaTransaccionScreen
import com.gamma.mobile.presentation.transacciones.TransaccionesScreen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.gamma.mobile.ui.theme.GammaTheme
import dagger.hilt.android.AndroidEntryPoint

enum class Screen {
    Dashboard,
    Transacciones,
    NuevaTransaccion,
    SifenScanner,
    Compromisos,
    Sistema
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GammaTheme {
                    GammaApp()
                }
            }
        }
    }
}

@Composable
fun GammaApp() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var compromisosTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.NuevaTransaccion && currentScreen != Screen.SifenScanner) {
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
                        selected = currentScreen == Screen.Compromisos,
                        onClick = { currentScreen = Screen.Compromisos },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Compromisos") },
                        label = { Text("Compromisos") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Sistema,
                        onClick = { currentScreen = Screen.Sistema },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Sistema") },
                        label = { Text("Sistema") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Dashboard -> {
                    val dashboardViewModel: com.gamma.mobile.presentation.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNuevaTransaccion = { currentScreen = Screen.NuevaTransaccion },
                        onNuevoRecordatorio = {
                            compromisosTab = 0
                            currentScreen = Screen.Compromisos
                        },
                        onNavigateToAvisos = {
                            compromisosTab = 1
                            currentScreen = Screen.Compromisos
                        }
                    )
                }
                Screen.Transacciones -> {
                    val transaccionesViewModel: com.gamma.mobile.presentation.transacciones.TransaccionesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    TransaccionesScreen(
                        viewModel = transaccionesViewModel,
                        onScanQr = { currentScreen = Screen.SifenScanner }
                    )
                }
                Screen.NuevaTransaccion -> {
                    val nuevaTransaccionViewModel: com.gamma.mobile.presentation.transacciones.NuevaTransaccionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    NuevaTransaccionScreen(
                        viewModel = nuevaTransaccionViewModel,
                        onSave = { currentScreen = Screen.Transacciones },
                        onBack = { currentScreen = Screen.Dashboard },
                        onProcessIA = { texto -> nuevaTransaccionViewModel.interpretarTextoIa(texto) },
                        onScanQr = { currentScreen = Screen.SifenScanner }
                    )
                }
                Screen.SifenScanner -> {
                    val sifenViewModel: com.gamma.mobile.presentation.sifen.SifenScannerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    com.gamma.mobile.presentation.sifen.SifenScannerScreen(
                        viewModel = sifenViewModel,
                        onBack = { currentScreen = Screen.Transacciones },
                        onSavedSuccess = { currentScreen = Screen.Transacciones }
                    )
                }
                Screen.Compromisos -> {
                    CompromisosScreen(initialTab = compromisosTab)
                }
                Screen.Sistema -> com.gamma.mobile.presentation.sistema.SistemaScreen()
            }
        }
    }
}

@Composable
fun DummyScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}


