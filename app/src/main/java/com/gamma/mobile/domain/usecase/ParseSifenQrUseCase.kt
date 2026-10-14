package com.gamma.mobile.domain.usecase

import com.gamma.mobile.domain.model.SifenInvoiceData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParseSifenQrUseCase @Inject constructor() {

    private val cdcRegex = Regex("\\b(\\d{44})\\b")
    private val dateIsoRegex = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
    private val dateCompactRegex = Regex("(\\d{4})(\\d{2})(\\d{2})")

    operator fun invoke(qrContent: String): Result<SifenInvoiceData> {
        val trimmed = qrContent.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("El contenido del código QR está vacío."))
        }

        val cdc = extractCdc(trimmed)
            ?: return Result.failure(IllegalArgumentException("No se encontró un CDC válido de 44 dígitos en el código QR."))

        val params = parseQueryParams(trimmed)

        val rucFromCdc = extractRucFromCdc(cdc)
        val nroFacturaFromCdc = extractNroFacturaFromCdc(cdc)
        val defaultFechaFromCdc = extractFechaFromCdc(cdc)

        val ruc = params["dRucEmisor"] ?: params["ruc"] ?: rucFromCdc
        val nroFactura = params["dNumDoc"] ?: params["factura"] ?: nroFacturaFromCdc

        val fechaEmision = extractFecha(params["dFeEmiDE"] ?: params["dFeEmi"] ?: params["fecha"]) ?: defaultFechaFromCdc

        val total = parseMonto(params["dTotGralOpe"] ?: params["total"] ?: params["monto"]) ?: 0L
        val iva5 = parseMonto(params["dIVA5"]) ?: 0L
        val iva10 = parseMonto(params["dIVA10"] ?: params["dTotIVA"] ?: params["iva"])
            ?: if (total > 0L) Math.round((total - iva5) / 11.0) else 0L

        return Result.success(
            SifenInvoiceData(
                cdc = cdc,
                rucEmisor = ruc,
                nroFactura = nroFactura,
                fechaEmision = fechaEmision,
                total = total,
                iva10 = iva10,
                iva5 = iva5,
                rawQrUrl = trimmed
            )
        )
    }

    private fun extractCdc(content: String): String? {
        val idParamMatch = Regex("[?&](?:Id|cdc|CDC)=([0-9]{44})", RegexOption.IGNORE_CASE).find(content)
        if (idParamMatch != null) {
            return idParamMatch.groupValues[1]
        }

        val directParamMatch = Regex("^(?:Id|cdc|CDC)=([0-9]{44})", RegexOption.IGNORE_CASE).find(content)
        if (directParamMatch != null) {
            return directParamMatch.groupValues[1]
        }

        val any44Digits = cdcRegex.find(content)
        return any44Digits?.groupValues?.get(1)
    }

    private fun extractRucFromCdc(cdc: String): String {
        return try {
            val rucNum = cdc.substring(2, 10).trimStart('0')
            val dv = cdc[10]
            if (rucNum.isNotEmpty()) "$rucNum-$dv" else cdc.substring(2, 11)
        } catch (_: Exception) {
            cdc.substring(2, 11)
        }
    }

    private fun extractNroFacturaFromCdc(cdc: String): String {
        return try {
            val est = cdc.substring(11, 14)
            val punExp = cdc.substring(14, 17)
            val numDoc = cdc.substring(17, 24)
            "$est-$punExp-$numDoc"
        } catch (_: Exception) {
            "001-001-${cdc.takeLast(7)}"
        }
    }

    private fun extractFechaFromCdc(cdc: String): String {
        return try {
            val yyyy = cdc.substring(25, 29)
            val mm = cdc.substring(29, 31)
            val dd = cdc.substring(31, 33)
            "$yyyy-$mm-$dd"
        } catch (_: Exception) {
            java.time.LocalDate.now().toString()
        }
    }

    private fun extractFecha(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        val decoded = tryDecodeHex(raw)

        val isoMatch = dateIsoRegex.find(decoded)
        if (isoMatch != null) {
            return isoMatch.groupValues[0]
        }

        val compactMatch = dateCompactRegex.find(decoded)
        if (compactMatch != null) {
            val y = compactMatch.groupValues[1]
            val m = compactMatch.groupValues[2]
            val d = compactMatch.groupValues[3]
            return "$y-$m-$d"
        }

        return null
    }

    private fun tryDecodeHex(input: String): String {
        val clean = input.trim()
        if (clean.length >= 8 && clean.length % 2 == 0 && clean.all { it in "0123456789abcdefABCDEF" }) {
            return try {
                val bytes = ByteArray(clean.length / 2)
                for (i in clean.indices step 2) {
                    bytes[i / 2] = clean.substring(i, i + 2).toInt(16).toByte()
                }
                val result = String(bytes, Charsets.UTF_8)
                if (result.any { it.isLetterOrDigit() }) result else input
            } catch (_: Exception) {
                input
            }
        }
        return input
    }

    private fun parseMonto(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            val cleaned = raw.replace(",", ".").trim()
            if (cleaned.contains(".")) {
                cleaned.toDouble().toLong()
            } else {
                cleaned.toLong()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseQueryParams(url: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val queryPart = when {
            url.contains("?") -> url.substringAfter("?")
            url.contains("&") -> url
            else -> ""
        }
        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = try {
                        java.net.URLDecoder.decode(parts[1].trim(), "UTF-8")
                    } catch (_: Exception) {
                        parts[1].trim()
                    }
                    map[key] = value
                }
            }
        }
        return map
    }
}
