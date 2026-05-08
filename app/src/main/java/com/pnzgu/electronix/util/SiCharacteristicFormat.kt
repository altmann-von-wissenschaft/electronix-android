package com.pnzgu.electronix.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign

/**
 * Форматирует числовую характеристику на карточке товара: приставки СИ шагом 10³
 * (к, М, Г, … и м, мк, н, …) и **русские** обозначения единиц (ГОСТ 8.417).
 * Ввод может быть на латинице или кириллице; на экране — русские символы.
 *
 * Составные единицы (м/с и т.п.) не масштабируются; латиница в них переводится в русскую.
 */
fun formatCharacteristicValueForProductCard(value: Double, unitRaw: String): String {
    val unit = unitRaw.trim()
    if (unit.isEmpty()) return formatPlainNumber(value)
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return "${value.toPlainString()} $unit"

    if (isCompoundUnit(unit)) {
        return "${formatPlainNumber(value)} ${toRussianCompoundUnit(unit)}"
    }

    if (unit.equals("kg", ignoreCase = true) || unit.equals("кг", ignoreCase = true)) {
        return formatWithPowerOf1000Prefixes(value * 1000.0, "g")
    }

    if (unit.equals("dB", ignoreCase = true) || unit.equals("дБ", ignoreCase = true)) {
        return "${formatPlainNumber(value)} дБ"
    }

    tryResolveWholeUnitToLatin(unit)?.let { latin ->
        return formatWithPowerOf1000Prefixes(value, latin)
    }

    val (exp10, remainder) = parseLeadingSiPrefix(unit)
    val baseLatin = remainderToCanonicalLatin(remainder)
    val normalized = value * 10.0.pow(exp10.toDouble())
    return formatWithPowerOf1000Prefixes(normalized, baseLatin)
}

private val unitsParsedAsSingleSymbolLatin =
    setOf(
        "Pa",
        "mol",
        "cd",
        "kat",
    )

private val unitsParsedAsSingleSymbolRussian =
    mapOf(
        "Па" to "Pa",
        "моль" to "mol",
        "Моль" to "mol",
        "кд" to "cd",
        "Кд" to "cd",
        "кат" to "kat",
        "Кат" to "kat",
    )

/** Целая единица без приставки — чтобы «Гц», «Па», «Вт» не резались как гига+ц и т.п. */
private fun tryResolveWholeUnitToLatin(unit: String): String? {
    val u = unit.trim()
    unitsParsedAsSingleSymbolRussian[u]?.let { return it }
    unitsParsedAsSingleSymbolRussian.entries.firstOrNull {
        it.key.equals(u, ignoreCase = true)
    }?.let { return it.value }

    for ((ru, lat) in russianToLatinPairs.sortedByDescending { it.first.length }) {
        if (u.equals(ru, ignoreCase = true)) return lat
    }
    for (lat in latinCanonicalToRussian.keys) {
        if (u.equals(lat, ignoreCase = true)) return lat
    }
    for (lat in unitsParsedAsSingleSymbolLatin) {
        if (u.equals(lat, ignoreCase = true)) return lat
    }
    return null
}

private fun isCompoundUnit(unit: String): Boolean =
    unit.any { ch ->
        ch.isWhitespace() ||
            ch == '/' ||
            ch == '·' ||
            ch == '*' ||
            ch == '(' ||
            ch == ')' ||
            ch == '²' ||
            ch == '³' ||
            ch == '^' ||
            ch == '°'
    }

private fun toRussianCompoundUnit(unit: String): String {
    if (unit.any { it in '\u0400'..'\u04FF' }) return unit
    var result = unit
    compoundLatinToRussian
        .sortedByDescending { it.first.length }
        .forEach { (lat, ru) ->
            result = result.replace(lat, ru)
        }
    return result
}

private val compoundLatinToRussian =
    listOf(
        "m/s²" to "м/с²",
        "m/s^2" to "м/с²",
        "m/s2" to "м/с²",
        "m/s" to "м/с",
        "s^-1" to "с⁻¹",
        "1/s" to "1/с",
    )

private val inputPrefixExponent2: Map<String, Int> =
    mapOf(
        "da" to 1,
        "да" to 1,
    )

private val inputPrefixExponent1: Map<String, Int> =
    buildMap {
        put("Y", 24)
        put("Z", 21)
        put("E", 18)
        put("P", 15)
        put("T", 12)
        put("G", 9)
        put("M", 6)
        put("k", 3)
        put("h", 2)
        put("d", -1)
        put("c", -2)
        put("m", -3)
        put("\u00B5", -6)
        put("\u03BC", -6)
        put("n", -9)
        put("p", -12)
        put("f", -15)
        put("a", -18)
        put("z", -21)
        put("y", -24)
        put("\u0419", 24) // Й йотта
        put("\u0417", 21) // З зетта
        put("\u042D", 18) // Э экса
        put("\u041F", 15) // П пета
        put("\u0422", 12) // Т тера
        put("\u0413", 9) // Г гига
        put("\u041C", 6) // М мега
        put("\u043A", 3) // к кило
        put("\u0433", 2) // г гекто (редко)
        put("\u0434", -1) // д деци
        put("\u0441", -2) // с санти
        put("\u043C", -3) // м милли
        put("\u043D", -9) // н нано
        put("\u043F", -12) // п пико
        put("\u0444", -15) // ф фемто
        put("\u0430", -18) // а атто
        put("\u0437", -21) // з зепто
        put("\u0438", -24) // и йокто
    }

private fun parseLeadingSiPrefix(unit: String): Pair<Int, String> {
    if (unit.length <= 1) return 0 to unit

    if (unit.startsWith("мк") && unit.length > 2) {
        return -6 to unit.drop(2)
    }
    if (unit.startsWith("mk", ignoreCase = true) && unit.length > 2) {
        return -6 to unit.drop(2)
    }

    val two = unit.take(2)
    val expDa = inputPrefixExponent2[two]
    if (expDa != null) {
        val rest = unit.drop(2)
        if (rest.isNotEmpty()) return expDa to rest
    }

    val one = unit.take(1)
    val exp1 = inputPrefixExponent1[one] ?: return 0 to unit
    val rest = unit.drop(1)
    if (rest.isEmpty()) {
        return 0 to unit
    }
    // не трактовать «гг» как гекто+грамм при опечатках — если основание одна «г», это грамм
    if (one == "\u0433" && rest == "\u0433") {
        return 0 to unit
    }
    return exp1 to rest
}

private val russianToLatinPairs: List<Pair<String, String>> =
    listOf(
        "Вт" to "W",
        "Гц" to "Hz",
        "Дж" to "J",
        "Гн" to "H",
        "Тл" to "T",
        "Вб" to "Wb",
        "Ом" to "Ω",
        "Па" to "Pa",
        "Нп" to "Np",
        "Бк" to "Bq",
        "Гр" to "Gy",
        "Зв" to "Sv",
        "Кл" to "C",
        "лм" to "lm",
        "лк" to "lx",
        "моль" to "mol",
        "рад" to "rad",
        "кат" to "kat",
        "бит" to "bit",
        "кг" to "kg",
        "В" to "V",
        "А" to "A",
        "Н" to "N",
        "Ф" to "F",
        "К" to "K",
        "г" to "g",
        "м" to "m",
        "с" to "s",
        "кд" to "cd",
        "ср" to "sr",
    )

private val latinCanonicalToRussian: Map<String, String> =
    mapOf(
        "W" to "Вт",
        "V" to "В",
        "A" to "А",
        "Ω" to "Ом",
        "Hz" to "Гц",
        "Pa" to "Па",
        "J" to "Дж",
        "N" to "Н",
        "F" to "Ф",
        "H" to "Гн",
        "T" to "Тл",
        "Wb" to "Вб",
        "lm" to "лм",
        "lx" to "лк",
        "Bq" to "Бк",
        "Gy" to "Гр",
        "Sv" to "Зв",
        "kat" to "кат",
        "mol" to "моль",
        "cd" to "кд",
        "sr" to "ср",
        "rad" to "рад",
        "Np" to "Нп",
        "g" to "г",
        "m" to "м",
        "s" to "с",
        "C" to "Кл",
        "K" to "К",
        "bit" to "бит",
    )

private val latinKnownTokens: Set<String> =
    latinCanonicalToRussian.keys + unitsParsedAsSingleSymbolLatin + setOf("kg")

private val lowercaseCanonicalUnits = setOf("m", "g", "s")

private fun remainderToCanonicalLatin(rest: String): String {
    val r = rest.trim()
    if (r.isEmpty()) return r

    latinKnownTokens.firstOrNull { it.equals(r, ignoreCase = true) }?.let { canon ->
        return when {
            canon == "Pa" -> "Pa"
            canon == "Hz" -> "Hz"
            canon in lowercaseCanonicalUnits -> canon.lowercase(Locale.ROOT)
            canon.length == 1 -> canon.uppercase(Locale.ROOT)
            else -> canon
        }
    }

    for ((ru, lat) in russianToLatinPairs.sortedByDescending { it.first.length }) {
        if (r.equals(ru, ignoreCase = true)) {
            return lat
        }
    }
    return r
}

private fun latinBaseToRussianDisplay(baseLatin: String): String =
    latinCanonicalToRussian[baseLatin]
        ?: latinCanonicalToRussian.entries.firstOrNull { it.key.equals(baseLatin, ignoreCase = true) }?.value
        ?: baseLatin

private val ln1000: Double = ln(1000.0)

private val positivePrefixesRu = arrayOf("", "к", "М", "Г", "Т", "П", "Э", "З", "Й")
private val negativePrefixesRu = arrayOf("", "м", "мк", "н", "п", "ф", "а", "з", "и")

private const val MAX_K = 8
private const val MIN_K = -8

private fun formatWithPowerOf1000Prefixes(value: Double, baseLatin: String): String {
    val ruBase = latinBaseToRussianDisplay(baseLatin)

    val sgn = value.sign
    if (value == 0.0) {
        return "${formatPlainNumber(0.0)} $ruBase"
    }
    val av = abs(value)
    val log1000 = ln(av) / ln1000
    var k = floor(log1000).toInt()
    var mantissa = av / 1000.0.pow(k.toDouble())

    while (mantissa >= 1000.0 - 1e-9 && k < MAX_K) {
        mantissa /= 1000.0
        k++
    }
    while (mantissa > 0 && mantissa < 1.0 && k > MIN_K) {
        mantissa *= 1000.0
        k--
    }

    if (k > MAX_K || k < MIN_K) {
        val sym = DecimalFormatSymbols(Locale.getDefault())
        val df =
            DecimalFormat("0.###E0", sym).apply {
                roundingMode = java.math.RoundingMode.HALF_UP
            }
        return "${df.format(value)} $ruBase"
    }

    val prefix =
        when {
            k == 0 -> ""
            k > 0 -> positivePrefixesRu[k]
            else -> negativePrefixesRu[-k]
        }
    val signedMantissa = sgn * mantissa
    return "${formatPlainNumber(signedMantissa)} $prefix$ruBase"
}

private fun formatPlainNumber(x: Double): String {
    if (!x.isFinite()) return x.toString()
    if (x == 0.0) return "0"

    val sym = DecimalFormatSymbols(Locale.getDefault())
    val df =
        DecimalFormat("0.##########", sym).apply {
            maximumFractionDigits = 12
            minimumFractionDigits = 0
            roundingMode = java.math.RoundingMode.HALF_UP
            isGroupingUsed = false
        }
    var s = df.format(x)
    if (s.contains(sym.decimalSeparator)) {
        while (s.endsWith('0')) {
            s = s.dropLast(1)
        }
        if (s.endsWith(sym.decimalSeparator)) {
            s = s.dropLast(1)
        }
    }
    return s
}

private fun Double.toPlainString(): String =
    when {
        isInfinite() -> if (this > 0) "Infinity" else "-Infinity"
        isNaN() -> "NaN"
        else -> toString()
    }
