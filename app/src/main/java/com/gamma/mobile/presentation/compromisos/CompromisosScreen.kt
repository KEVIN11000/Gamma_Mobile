package com.gamma.mobile.presentation.compromisos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamma.mobile.presentation.asistencia.AsistenciaScreen
import com.gamma.mobile.presentation.asistencia.AsistenciaViewModel
import com.gamma.mobile.presentation.avisos.AvisosScreen
import com.gamma.mobile.presentation.avisos.AvisosViewModel
import com.gamma.mobile.presentation.obligaciones.ObligacionesScreen
import com.gamma.mobile.presentation.obligaciones.ObligacionesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompromisosScreen(
    initialTab: Int = 0
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Compromisos", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Deudas") },
                icon = { Icon(Icons.Default.Info, contentDescription = "Deudas") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Avisos") },
                icon = { Icon(Icons.Default.Notifications, contentDescription = "Avisos") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Asistencia") },
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Asistencia") }
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> {
                    val obligacionesViewModel: ObligacionesViewModel = hiltViewModel()
                    ObligacionesScreen(viewModel = obligacionesViewModel, showTopBar = false)
                }
                1 -> {
                    val avisosViewModel: AvisosViewModel = hiltViewModel()
                    AvisosScreen(viewModel = avisosViewModel)
                }
                2 -> {
                    val asistenciaViewModel: AsistenciaViewModel = hiltViewModel()
                    AsistenciaScreen(viewModel = asistenciaViewModel, showTopBar = false)
                }
            }
        }
    }
}
