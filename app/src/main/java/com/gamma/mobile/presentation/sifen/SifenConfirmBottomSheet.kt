package com.gamma.mobile.presentation.sifen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamma.mobile.domain.model.SifenInvoiceData
import com.gamma.mobile.utils.formatGuaranies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SifenConfirmBottomSheet(
    invoice: SifenInvoiceData,
    isSaving: Boolean,
    onConfirm: (categoria: String) -> Unit,
    onDismiss: () -> Unit
) {
    val categorias = listOf("Gastos Generales", "Alimentación", "Insumos", "Servicios", "Combustible", "Otros")
    var selectedCategoria by remember { mutableStateOf(categorias.first()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Factura Electrónica SIFEN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "RUC Emisor", value = invoice.rucEmisor)
                    DetailRow(label = "Nro. Factura", value = invoice.nroFactura)
                    DetailRow(label = "Fecha", value = invoice.fechaEmision)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow(
                        label = "Total a Pagar",
                        value = formatGuaranies(invoice.total),
                        isHighlight = true
                    )
                    DetailRow(
                        label = "IVA Incluido (10%)",
                        value = formatGuaranies(invoice.iva10)
                    )
                    if (invoice.iva5 > 0L) {
                        DetailRow(
                            label = "IVA Incluido (5%)",
                            value = formatGuaranies(invoice.iva5)
                        )
                    }
                    Text(
                        text = "CDC: ${invoice.cdc.take(8)}...${invoice.cdc.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Selector de Categoría
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedCategoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría de Gasto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategoria = cat
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Descartar")
                }
                Button(
                    onClick = { onConfirm(selectedCategoria) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
