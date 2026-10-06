package com.gamma.mobile.utils

import java.text.NumberFormat
import java.util.Locale

fun formatGuaranies(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "PY"))
    return "${formatter.format(amount)} ₲"
}
