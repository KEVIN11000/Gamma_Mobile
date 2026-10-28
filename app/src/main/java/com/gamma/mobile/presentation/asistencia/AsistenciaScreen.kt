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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gamma.mobile.domain.model.AnaliticasHorasResponse
import com.gamma.mobile.domain.model.PeriodosResponse
import com.gamma.mobile.presentation.cierre.CierreUiState
import com.gamma.mobile.presentation.cierre.CierreViewModel
import com.gamma.mobile.presentation.cierre.CierresViewModel
import com.gamma.mobile.presentation.cierre.HistorialCierresScreen
import com.gamma.mobile.ui.theme.GammaTheme
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
                    text = { Text("Ejecutar") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Historial") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> MarcacionesTabContent(viewModel = viewModel)
                1 -> CierreTabContent(cierreViewModel = cierreViewModel)
                2 -> {
                    val cierresViewModel: CierresViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    HistorialCierresScreen(viewModel = cierresViewModel, showTopBar = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcacionesTabContent(viewModel: AsistenciaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val analiticasState by viewModel.analiticasState.collectAsState()
    val excelDownloadState by viewModel.excelDownloadState.collectAsState()
    val modo by viewModel.modo.collectAsState()
    val materia by viewModel.materia.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(excelDownloadState) {
        if (excelDownloadState is ExcelDownloadState.Success) {
            val success = excelDownloadState as ExcelDownloadState.Success
            try {
                val cacheDir = File(context.cacheDir, "comprobantes")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, success.fileName)
                FileOutputStream(tempFile).use { it.write(success.fileBytes) }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Planilla guardada en caché. No hay visor de Excel instalado.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir Excel: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                viewModel.resetExcelDownloadState()
            }
        } else if (excelDownloadState is ExcelDownloadState.Error) {
            val msg = (excelDownloadState as ExcelDownloadState.Error).message
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.resetExcelDownloadState()
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
            // Card Analíticas Laborales MTD
            AnaliticasCard(
                analiticasState = analiticasState,
                excelDownloadState = excelDownloadState,
                onRefresh = { viewModel.loadAnaliticas() },
                onDescargarExcel = { hoja -> viewModel.descargarExcel(hoja) }
            )

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
                        onClick = { viewModel.marcarAsistencia(context = context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is AsistenciaUiState.Loading && uiState !is AsistenciaUiState.Enviando
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
                        onExpandedChange = { if (uiState !is AsistenciaUiState.Loading && uiState !is AsistenciaUiState.Enviando) expandedMateria = !expandedMateria },
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
                            enabled = uiState !is AsistenciaUiState.Loading && uiState !is AsistenciaUiState.Enviando
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
                        enabled = uiState !is AsistenciaUiState.Loading && uiState !is AsistenciaUiState.Enviando && materia.isNotBlank()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar Asistencia a Materia")
                    }
                }
            }

            // Progreso
            if (uiState is AsistenciaUiState.Loading || uiState is AsistenciaUiState.Enviando) {
                val textoProgreso = when (val s = uiState) {
                    is AsistenciaUiState.Enviando -> "⏳ Marcación enviada al servidor a las ${s.horaEnvio}..."
                    else -> "Comunicando con el servidor..."
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            textoProgreso,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
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
                    title = {
                        Text(
                            if (state.confirmadoEnPlanilla) "Marcación Registrada en Planilla"
                            else "Marcación Procesada"
                        )
                    },
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
                val cacheDir = File(context.cacheDir, "comprobantes")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val tempFile = File(cacheDir, pdfState.fileName)
                FileOutputStream(tempFile).use { it.write(pdfState.fileBytes) }

                com.gamma.mobile.utils.ComprobanteNotificationHelper.mostrarNotificacionDescarga(
                    context = context,
                    file = tempFile,
                    etiqueta = hoja.ifBlank { pdfState.fileName }
                )

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
        } else if (uiState is CierreUiState.ExcelDownloaded) {
            val excelState = uiState as CierreUiState.ExcelDownloaded
            try {
                val cacheDir = File(context.cacheDir, "comprobantes")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val tempFile = File(cacheDir, excelState.fileName)
                FileOutputStream(tempFile).use { it.write(excelState.fileBytes) }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Planilla Excel guardada. No hay visor de Excel instalado.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir el Excel: ${e.message}", Toast.LENGTH_LONG).show()
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
            if (uiState is CierreUiState.LoadingCierre || uiState is CierreUiState.DownloadingPdf || uiState is CierreUiState.DownloadingExcel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        val text = when (uiState) {
                            is CierreUiState.DownloadingPdf -> "Descargando reporte PDF..."
                            is CierreUiState.DownloadingExcel -> "Generando y descargando planilla Excel..."
                            else -> "Ejecutando cierre en el servidor..."
                        }
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

                        if (tipoCierre == "horas") {
                            FilledTonalButton(
                                onClick = { cierreViewModel.descargarExcel(hoja) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar Planilla Excel (.xlsx)")
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

@Composable
fun AnaliticasCard(
    analiticasState: AnaliticasUiState,
    excelDownloadState: ExcelDownloadState,
    onRefresh: () -> Unit,
    onDescargarExcel: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Analíticas MTD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refrescar analíticas",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            when (val aState = analiticasState) {
                is AnaliticasUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargando analíticas...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AnaliticasUiState.Success -> {
                    val data = aState.analiticas
                    val periodos = aState.periodos

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.periodo.ifEmpty { "Período Actual" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (periodos != null) {
                            val (badgeBg, badgeText, badgeLabel) = if (periodos.sincronizados) {
                                Triple(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                    "✓ Sincronizado"
                                )
                            } else {
                                Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer,
                                    "⚠️ Desfasado"
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Horas MTD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(data.totalHorasFormato, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${data.diasTrabajados} días trabajados", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Devengado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatGuaranies(data.salarioAcumuladoGs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${formatGuaranies(data.montoPorHora.toLong())}/h", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extras: +${data.horasExtrasFormato}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Perdidas: -${data.horasPerdidasFormato}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Neto: ${data.balanceNetoFormato}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onDescargarExcel(data.periodo) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = excelDownloadState !is ExcelDownloadState.Downloading
                    ) {
                        if (excelDownloadState is ExcelDownloadState.Downloading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Descargando Excel...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Planilla Excel (.xlsx)", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                is AnaliticasUiState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = aState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefresh) {
                            Text("Reintentar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Analíticas MTD Exitosas", showBackground = true)
@Composable
fun AnaliticasCardPreview_Success() {
    GammaTheme {
        AnaliticasCard(
            analiticasState = AnaliticasUiState.Success(
                analiticas = AnaliticasHorasResponse(
                    periodo = "Octubre 2026",
                    montoPorHora = 14634,
                    diasTrabajados = 14,
                    totalHorasDecimal = 107.3,
                    totalHorasFormato = "107h 18m",
                    salarioAcumuladoGs = 1570228L,
                    horasPerdidasDecimal = 1.4,
                    horasExtrasDecimal = 2.5,
                    balanceNetoDecimal = 1.1,
                    horasPerdidasFormato = "1h 24m",
                    horasExtrasFormato = "2h 30m",
                    balanceNetoFormato = "+1h 06m",
                    promedioHorasPorDia = 7.66
                ),
                periodos = PeriodosResponse(
                    periodoLaboral = "Octubre 2026",
                    periodoContable = "Octubre 2026",
                    sincronizados = true
                )
            ),
            excelDownloadState = ExcelDownloadState.Idle,
            onRefresh = {},
            onDescargarExcel = {}
        )
    }
}

@Preview(name = "Analíticas MTD Cargando", showBackground = true)
@Composable
fun AnaliticasCardPreview_Loading() {
    GammaTheme {
        AnaliticasCard(
            analiticasState = AnaliticasUiState.Loading,
            excelDownloadState = ExcelDownloadState.Idle,
            onRefresh = {},
            onDescargarExcel = {}
        )
    }
}
