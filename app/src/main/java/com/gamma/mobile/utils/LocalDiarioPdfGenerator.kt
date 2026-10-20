package com.gamma.mobile.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.presentation.transacciones.FiltroTipo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiarioPdfRow(
    val fecha: String,
    val tipo: String,
    val categoria: String,
    val comprobante: String,
    val montoGs: String,
    val isIngreso: Boolean
)

data class DiarioPdfReportModel(
    val titulo: String = "Gamma - Libro Diario",
    val fechaEmision: String,
    val rangoPeriodo: String,
    val filtroAplicado: String,
    val totalIngresosGs: String,
    val totalGastosGs: String,
    val saldoNetoGs: String,
    val rows: List<DiarioPdfRow>
) {
    companion object {
        fun buildFrom(
            movimientos: List<Movimiento>,
            filtro: FiltroTipo,
            fechaDesde: String?,
            fechaHasta: String?
        ): DiarioPdfReportModel {
            val fechaEmision = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val rangoPeriodo = when {
                !fechaDesde.isNullOrBlank() && !fechaHasta.isNullOrBlank() -> "Desde $fechaDesde hasta $fechaHasta"
                !fechaDesde.isNullOrBlank() -> "Desde $fechaDesde"
                !fechaHasta.isNullOrBlank() -> "Hasta $fechaHasta"
                else -> "Todos los periodos registrados"
            }

            val filtroAplicado = when (filtro) {
                FiltroTipo.TODOS -> "Todos los movimientos"
                FiltroTipo.INGRESO -> "Solo Ingresos"
                FiltroTipo.GASTO -> "Solo Gastos"
            }

            var sumIngresos = 0L
            var sumGastos = 0L

            val rows = movimientos.map { mov ->
                val isIngreso = mov.tipo.equals("Ingreso", ignoreCase = true)
                if (isIngreso) {
                    sumIngresos += mov.total
                } else {
                    sumGastos += mov.total
                }
                DiarioPdfRow(
                    fecha = mov.fecha,
                    tipo = mov.tipo,
                    categoria = mov.categoria,
                    comprobante = mov.comprobante,
                    montoGs = formatGuaranies(mov.total),
                    isIngreso = isIngreso
                )
            }

            val saldoNeto = sumIngresos - sumGastos

            return DiarioPdfReportModel(
                fechaEmision = fechaEmision,
                rangoPeriodo = rangoPeriodo,
                filtroAplicado = filtroAplicado,
                totalIngresosGs = formatGuaranies(sumIngresos),
                totalGastosGs = formatGuaranies(sumGastos),
                saldoNetoGs = formatGuaranies(saldoNeto),
                rows = rows
            )
        }
    }
}

object LocalDiarioPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard pt
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val ROWS_PER_PAGE = 26

    fun generarPdf(
        context: Context,
        reportModel: DiarioPdfReportModel
    ): File {
        val outputDir = File(context.cacheDir, "comprobantes").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val targetFile = File(outputDir, "reporte_diario_$timestamp.pdf")

        val pdfDoc = PdfDocument()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(20, 40, 80)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bgHeaderPaint = Paint().apply {
            color = Color.rgb(30, 60, 110)
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }
        val ingresoPaint = Paint().apply {
            color = Color.rgb(0, 130, 60)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val gastoPaint = Paint().apply {
            color = Color.rgb(180, 20, 20)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rows = reportModel.rows
        val totalRows = rows.size
        val totalPages = if (totalRows == 0) 1 else ((totalRows - 1) / ROWS_PER_PAGE) + 1

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN + 20f

            // Encabezado solo en página 1
            if (pageIndex == 0) {
                canvas.drawText(reportModel.titulo, MARGIN, y, titlePaint)
                y += 16f
                canvas.drawText("Emitido: ${reportModel.fechaEmision} | Periodo: ${reportModel.rangoPeriodo}", MARGIN, y, subPaint)
                y += 12f
                canvas.drawText("Filtro: ${reportModel.filtroAplicado}", MARGIN, y, subPaint)
                y += 16f

                // Resumen Financiero en cabecera
                val summaryBoxTop = y
                val summaryBoxBottom = y + 26f
                val summaryPaint = Paint().apply { color = Color.rgb(238, 242, 248) }
                canvas.drawRect(MARGIN, summaryBoxTop, PAGE_WIDTH - MARGIN, summaryBoxBottom, summaryPaint)

                val summaryTextPaint = Paint().apply {
                    color = Color.rgb(20, 30, 60)
                    textSize = 9f
                    isAntiAlias = true
                }
                val summaryBoldPaint = Paint().apply {
                    color = Color.rgb(20, 30, 60)
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val colSummaryWidth = (PAGE_WIDTH - 2 * MARGIN) / 3f
                canvas.drawText("Ingresos: ", MARGIN + 8f, y + 17f, summaryTextPaint)
                canvas.drawText(reportModel.totalIngresosGs, MARGIN + 55f, y + 17f, ingresoPaint)

                canvas.drawText("Gastos: ", MARGIN + colSummaryWidth + 8f, y + 17f, summaryTextPaint)
                canvas.drawText(reportModel.totalGastosGs, MARGIN + colSummaryWidth + 48f, y + 17f, gastoPaint)

                canvas.drawText("Neto: ", MARGIN + 2 * colSummaryWidth + 8f, y + 17f, summaryTextPaint)
                canvas.drawText(reportModel.saldoNetoGs, MARGIN + 2 * colSummaryWidth + 40f, y + 17f, summaryBoldPaint)

                y += 36f
            } else {
                y += 10f
                canvas.drawText("${reportModel.titulo} - Página ${pageIndex + 1}/$totalPages", MARGIN, y, subPaint)
                y += 20f
            }

            // Encabezados de Tabla
            val tableTop = y
            val rowHeight = 18f
            canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + rowHeight, bgHeaderPaint)

            val colFecha = MARGIN + 6f
            val colTipo = MARGIN + 70f
            val colCat = MARGIN + 130f
            val colComp = MARGIN + 310f
            val colMonto = MARGIN + 430f

            canvas.drawText("Fecha", colFecha, tableTop + 12f, headerPaint)
            canvas.drawText("Tipo", colTipo, tableTop + 12f, headerPaint)
            canvas.drawText("Categoría", colCat, tableTop + 12f, headerPaint)
            canvas.drawText("Comprobante", colComp, tableTop + 12f, headerPaint)
            canvas.drawText("Total (Gs.)", colMonto, tableTop + 12f, headerPaint)

            y = tableTop + rowHeight

            // Filas
            val startIdx = pageIndex * ROWS_PER_PAGE
            val endIdx = minOf(startIdx + ROWS_PER_PAGE, totalRows)

            for (i in startIdx until endIdx) {
                val row = rows[i]
                if ((i % 2) == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraBgPaint)
                }

                canvas.drawText(row.fecha.take(10), colFecha, y + 12f, textPaint)
                canvas.drawText(row.tipo.take(8), colTipo, y + 12f, textPaint)
                canvas.drawText(row.categoria.take(28), colCat, y + 12f, textPaint)
                canvas.drawText(row.comprobante.take(18), colComp, y + 12f, textPaint)

                val mPaint = if (row.isIngreso) ingresoPaint else gastoPaint
                canvas.drawText(row.montoGs, colMonto, y + 12f, mPaint)

                canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, linePaint)
                y += rowHeight
            }

            // Pie de página
            canvas.drawText(
                "Página ${pageIndex + 1} de $totalPages — Gamma Mobile (Generado en el dispositivo, sin envío a servidor)",
                MARGIN,
                PAGE_HEIGHT - MARGIN + 10f,
                subPaint
            )

            pdfDoc.finishPage(page)
        }

        FileOutputStream(targetFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return targetFile
    }

    fun abrirPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir reporte de Libro Diario"))
    }
}
