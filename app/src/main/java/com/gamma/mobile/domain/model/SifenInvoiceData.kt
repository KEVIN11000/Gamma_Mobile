package com.gamma.mobile.domain.model

data class SifenInvoiceData(
    val cdc: String,
    val rucEmisor: String,
    val nroFactura: String,
    val fechaEmision: String,
    val total: Long,
    val iva10: Long,
    val iva5: Long = 0L,
    val rawQrUrl: String
)
