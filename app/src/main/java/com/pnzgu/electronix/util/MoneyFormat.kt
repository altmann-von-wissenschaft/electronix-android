package com.pnzgu.electronix.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val rubleFormat: DecimalFormat by lazy {
    val sym = DecimalFormatSymbols(Locale("ru", "RU"))
    DecimalFormat("#,##0.##", sym).apply {
        decimalFormatSymbols = sym
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
}

/** Amount in rubles with explicit «руб.» suffix (Russian grouping). */
fun formatRubles(amount: Double): String = "${rubleFormat.format(amount)} руб."
