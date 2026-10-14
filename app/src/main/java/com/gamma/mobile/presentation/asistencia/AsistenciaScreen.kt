package com.gamma.mobile.presentation.asistencia

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gamma.mobile.presentation.cierre.CierreUiState
import com.gamma.mobile.presentation.cierre.CierreViewModel
import com.gamma.mobile.utils.formatGuaranies
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(
    viewModel: AsistenciaViewModel,
    cierreViewModel: CierreViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    showTopBar: Boolean = true
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Asistencia y RRHH", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Marcaciones") },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cierre Mensual") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> MarcacionesTabContent(viewModel = viewModel)
                1 -> CierreTabContent(cierreViewModel = cierreViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcacionesTabContent(viewModel: AsistenciaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val modo by viewModel.modo.collectAsState()
    val materia by viewModel.materia.collectAsState()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Marcación Laboral (Entrada / Salida)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Marcación Laboral",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Selecciona el modo de marcación y confirma entrada o salida:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Selector de Modo (Normal / Directo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = modo == "normal",
                            onClick = { viewModel.setModo("normal") },
                            label = { Text("Normal") },
                            leadingIcon = if (modo == "normal") {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = modo == "directo",
                            onClick = { viewModel.setModo("directo") },
                            label = { Text("Directo") },
                            leadingIcon = if (modo == "directo") {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { viewModel.marcarAsistencia() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is AsistenciaUiState.Loading
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar Entrada / Salida ($modo)")
                    }
                }
            }

            // Card Marcación de Materia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Marcación de Materia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Selecciona la materia para registrar asistencia:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    var expandedMateria by remember { mutableStateOf(false) }
                    val materiasDisponibles by viewModel.materiasDisponibles.collectAsState()

                    ExposedDropdownMenuBox(
                        expanded = expandedMateria,
                        onExpandedChange = { if (uiState !is AsistenciaUiState.Loading) expandedMateria = !expandedMateria },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = materia,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Materia") },
                            placeholder = { Text("Seleccionar materia") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMateria) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            enabled = uiState !is AsistenciaUiState.Loading
                        )

                        ExposedDropdownMenu(
                            expanded = expandedMateria,
                            onDismissRequest = { expandedMateria = false }
                        ) {
                            materiasDisponibles.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text(mat) },
                                    onClick = {
                                        viewModel.setMateria(mat)
                                        expandedMateria = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.marcarMateria() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is AsistenciaUiState.Loading && materia.isNotBlank()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar Asistencia a Materia")
                    }
                }
            }

            // Progreso
            if (uiState is AsistenciaUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Comunicando con el servidor...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Diálogos de Feedback
        when (val state = uiState) {
            is AsistenciaUiState.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = { Text("Marcación Registrada") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(state.mensaje, fontWeight = FontWeight.Bold)
                            Text(state.detalle, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
            is AsistenciaUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = { Text("Error al Marcar") },
                    text = {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Cerrar")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreTabContent(cierreViewModel: CierreViewModel) {
    val uiState by cierreViewModel.uiState.collectAsState()
    val hoja by cierreViewModel.hoja.collectAsState()
    val aplicarDescuento by cierreViewModel.aplicarDescuento.collectAsState()
    val tipoCierre by cierreViewModel.tipoCierre.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Manejo de PDF descargado -> Abrir intent de visualización
    LaunchedEffect(uiState) {
        if (uiState is CierreUiState.PdfDownloaded) {
            val pdfState = uiState as CierreUiState.PdfDownloaded
            try {
                val tempFile = File(context.cacheDir, pdfState.fileName)
                FileOutputStream(tempFile).use { it.write(pdfState.fileBytes) }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No hay visor de PDF instalado", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Parámetros de Cierre
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Cierre de Período Mensual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Selector de Tipo de Cierre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tipoCierre == "horas",
                            onClick = { cierreViewModel.setTipoCierre("horas") },
                            label = { Text("Laboral (Horas)") }
                        )
                        FilterChip(
                            selected = tipoCierre == "contable",
                            onClick = { cierreViewModel.setTipoCierre("contable") },
                            label = { Text("Contable (Libro Diario)") }
                        )
                    }

                    Text(
                        if (tipoCierre == "horas") 
                            "Consolida horas trabajadas, asistencias y reporte de nómina:" 
                        else 
                            "Consolida ingresos, gastos y posición fiscal del Libro Diario:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    var expandedPeriodo by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedPeriodo,
                        onExpandedChange = { if (uiState !is CierreUiState.LoadingCierre) expandedPeriodo = !expandedPeriodo },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = hoja,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Período / Hoja") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            enabled = uiState !is CierreUiState.LoadingCierre
                        )

                        ExposedDropdownMenu(
                            expanded = expandedPeriodo,
                            onDismissRequest = { expandedPeriodo = false }
                        ) {
                            cierreViewModel.periodosDisponibles.forEach { periodo ->
                                DropdownMenuItem(
                                    text = { Text(periodo) },
                                    onClick = {
                                        cierreViewModel.setHoja(periodo)
                                        expandedPeriodo = false
                                    }
                                )
                            }
                        }
                    }

                    if (tipoCierre == "horas") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aplicar descuento", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Descuenta horas faltantes o no computadas",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = aplicarDescuento,
                                onCheckedChange = { cierreViewModel.setAplicarDescuento(it) },
                                enabled = uiState !is CierreUiState.LoadingCierre
                            )
                        }
                    }

                    Button(
                        onClick = { cierreViewModel.ejecutarCierre() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is CierreUiState.LoadingCierre && hoja.isNotBlank()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (tipoCierre == "horas") "Ejecutar Cierre Laboral" else "Ejecutar Cierre Contable")
                    }
                }
            }

            // Progreso
            if (uiState is CierreUiState.LoadingCierre || uiState is CierreUiState.DownloadingPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        val text = if (uiState is CierreUiState.DownloadingPdf) "Descargando reporte PDF..." else "Ejecutando cierre en el servidor..."
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Resultado del Cierre
            if (uiState is CierreUiState.CierreSuccess) {
                val success = uiState as CierreUiState.CierreSuccess
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cierre Exitoso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text(success.response.resumen, style = MaterialTheme.typography.bodyMedium)

                        if (success.response.totalHoras > 0) {
                            Text("Total Horas: ${success.response.totalHoras} hs", fontWeight = FontWeight.SemiBold)
                        }
                        if (success.response.totalMonto > 0) {
                            Text("Total Monto: ${formatGuaranies(success.response.totalMonto)}", fontWeight = FontWeight.SemiBold)
                        }

                        if (!success.response.pdfUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { cierreViewModel.descargarPdf(success.response.pdfUrl) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver / Descargar Reporte PDF")
                            }
                        }
                    }
                }
            }
        }

        // Error Dialog
        if (uiState is CierreUiState.Error) {
            val error = uiState as CierreUiState.Error
            AlertDialog(
                onDismissRequest = { cierreViewModel.resetState() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Error en Cierre") },
                text = { Text(error.message) },
                confirmButton = {
                    Button(onClick = { cierreViewModel.resetState() }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}
