package com.gamma.mobile.presentation.cierre

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.ui.theme.GammaTheme
import com.gamma.mobile.utils.ComprobanteNotificationHelper
import com.gamma.mobile.utils.formatGuaranies
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialCierresScreen(
    viewModel: CierresViewModel,
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtroTipo by viewModel.filtroTipo.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val context = LocalContext.current

    // Gestión de descarga y apertura de PDF nativo
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadPdfState.Downloaded -> {
                try {
                    val cacheDir = File(context.cacheDir, "comprobantes")
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs()
                    }
                    val tempFile = File(cacheDir, state.fileName)
                    FileOutputStream(tempFile).use { it.write(state.fileBytes) }

                    ComprobanteNotificationHelper.mostrarNotificacionDescarga(
                        context = context,
                        file = tempFile,
                        etiqueta = state.fileName
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
                    Toast.makeText(context, "No hay visor de PDF instalado en el dispositivo", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al abrir el comprobante: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    viewModel.clearDownloadState()
                }
            }
            is DownloadPdfState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearDownloadState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Historial de Cierres", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.loadCierres(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            FilterTabsCierres(
                selectedFiltro = filtroTipo,
                onFilterSelected = { viewModel.setFiltroTipo(it) }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is HistorialCierresUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is HistorialCierresUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { viewModel.loadCierres(forceRefresh = true) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                    is HistorialCierresUiState.Success -> {
                        if (state.cierres.isEmpty()) {
                            Text(
                                "No hay cierres registrados en esta categoría.",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.cierres, key = { it.id }) { cierre ->
                                    val isDownloading = downloadState is DownloadPdfState.Downloading &&
                                            (downloadState as DownloadPdfState.Downloading).id == cierre.id

                                    CierreHistorialCard(
                                        cierre = cierre,
                                        isDownloading = isDownloading,
                                        onDownloadClick = { viewModel.descargarComprobante(cierre) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabsCierres(
    selectedFiltro: FiltroTipoCierre,
    onFilterSelected: (FiltroTipoCierre) -> Unit
) {
    val filters = listOf(
        FiltroTipoCierre.TODOS to "Todos",
        FiltroTipoCierre.CONTABLE to "Contables",
        FiltroTipoCierre.LABORAL to "Laborales"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filtro, label) ->
            FilterChip(
                selected = selectedFiltro == filtro,
                onClick = { onFilterSelected(filtro) },
                label = { Text(label, fontWeight = if (selectedFiltro == filtro) FontWeight.Bold else FontWeight.Normal) }
            )
        }
    }
}

@Composable
fun CierreHistorialCard(
    cierre: CierreHistorialItem,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit
) {
    val esContable = cierre.tipo.equals("contable", ignoreCase = true)
    val badgeContainerColor = if (esContable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val badgeContentColor = if (esContable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cierre.periodo.ifBlank { "Cierre ${cierre.tipo.replaceFirstChar { it.uppercase() }}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (cierre.fecha.isNotBlank()) {
                        Text(
                            text = "Fecha: ${cierre.fecha}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeContainerColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (esContable) "Contable" else "Laboral",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeContentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (esContable) {
                    Text(
                        text = "Total Neto:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatGuaranies(cierre.totalMonto),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Column {
                        Text(
                            text = "Total Nómina: ${formatGuaranies(cierre.totalMonto)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (cierre.totalHoras > 0) {
                            Text(
                                text = "Horas registradas: ${cierre.totalHoras} hs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (cierre.resumen.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cierre.resumen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón de acción PDF
            if (cierre.pdfDisponible) {
                Button(
                    onClick = onDownloadClick,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargando PDF...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Comprobante PDF")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comprobante no generado")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorialCierresScreenPreview() {
    GammaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CierreHistorialCard(
                cierre = CierreHistorialItem(
                    id = "c1",
                    tipo = "contable",
                    periodo = "Octubre 2026",
                    fecha = "22/10/2026 19:30",
                    totalMonto = 14500000L,
                    totalHoras = 0.0,
                    resumen = "Cierre contable mensual balanceado. IVA Débito amortizado.",
                    pdfUrl = "https://gamma.bot/pdf/cierre_contable.pdf",
                    pdfDisponible = true
                ),
                isDownloading = false,
                onDownloadClick = {}
            )
            CierreHistorialCard(
                cierre = CierreHistorialItem(
                    id = "c2",
                    tipo = "laboral",
                    periodo = "Octubre 2026 (Quincena 2)",
                    fecha = "15/10/2026 18:00",
                    totalMonto = 6800000L,
                    totalHoras = 168.0,
                    resumen = "Liquidación de nómina docente y técnica.",
                    pdfUrl = null,
                    pdfDisponible = false
                ),
                isDownloading = false,
                onDownloadClick = {}
            )
        }
    }
}
