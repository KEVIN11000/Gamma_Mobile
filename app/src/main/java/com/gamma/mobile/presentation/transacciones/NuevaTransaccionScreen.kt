package com.gamma.mobile.presentation.transacciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gamma.mobile.utils.formatGuaranies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaTransaccionScreen(
    viewModel: NuevaTransaccionViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onProcessIA: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("IA", "Foto", "Manual")
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSave()
        }
    }
    
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage!!) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Transacción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
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
                } else if (selectedTabIndex == 1) {
                    val context = LocalContext.current
                    CargaOcrUI { uri ->
                        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        if (bytes != null) {
                            viewModel.interpretarImagenOcr(bytes, "comprobante.jpg")
                            selectedTabIndex = 2
                        }
                    }
                } else {
                    CargaManual(viewModel, uiState)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargaManual(viewModel: NuevaTransaccionViewModel, uiState: NuevaTransaccionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tipo de Movimiento
        Text("Tipo de Movimiento", style = MaterialTheme.typography.labelLarge)
        Row {
            RadioButton(selected = uiState.tipo == "Gasto", onClick = { viewModel.updateTipo("Gasto") })
            Text("Gasto", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = uiState.tipo == "Ingreso", onClick = { viewModel.updateTipo("Ingreso") })
            Text("Ingreso", modifier = Modifier.align(Alignment.CenterVertically))
        }

        OutlinedTextField(
            value = uiState.categoria,
            onValueChange = { viewModel.updateCategoria(it) },
            label = { Text("Categoría") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = uiState.totalInput,
            onValueChange = { viewModel.updateTotal(it) },
            label = { Text("Monto Total (IVA Incluido, ₲)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Comprobante
        Text("Tipo de Comprobante", style = MaterialTheme.typography.labelLarge)
        var expandedComprobante by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedComprobante,
            onExpandedChange = { expandedComprobante = !expandedComprobante }
        ) {
            OutlinedTextField(
                value = uiState.comprobante,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedComprobante) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedComprobante,
                onDismissRequest = { expandedComprobante = false }
            ) {
                listOf("Virtual Legal", "Física Legal", "No Legal").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.updateComprobante(option)
                            expandedComprobante = false
                        }
                    )
                }
            }
        }

        // Tasa IVA
        Text("Tasa de IVA", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("10%", "5%", "Exento").forEach { option ->
                FilterChip(
                    selected = uiState.tasaIva == option,
                    onClick = { viewModel.updateTasaIva(option) },
                    label = { Text(option) }
                )
            }
        }

        // IVA Preview Card
        if (uiState.totalParsed > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Desglose de Impuestos (IVA extraído del total)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monto Gravado:")
                        Text(formatGuaranies(uiState.ivaPreview.montoGravado))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monto IVA:")
                        Text(formatGuaranies(uiState.ivaPreview.montoIva), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.saveMovimiento() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isValid && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Guardar Transacción")
            }
        }
    }
}

@Composable
fun CargaOcrUI(onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onImageSelected(uri)
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            onImageSelected(photoUri!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Seleccionar de la Galer�a")
        }
        
        Button(onClick = { 
            val tempFile = File.createTempFile("photo_", ".jpg", context.cacheDir).apply { createNewFile() }
            photoUri = FileProvider.getUriForFile(context, "com.gamma.mobile.fileprovider", tempFile)
            cameraLauncher.launch(photoUri!!)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Tomar Foto con C�mara")
        }
    }
}
