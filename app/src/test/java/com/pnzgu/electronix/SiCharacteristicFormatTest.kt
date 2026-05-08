package com.pnzgu.electronix

import com.pnzgu.electronix.util.formatCharacteristicValueForProductCard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class SiCharacteristicFormatTest {
    private lateinit var savedLocale: Locale

    @Before
    fun saveLocale() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(savedLocale)
    }

    @Test
    fun watts_to_kilowatts() {
        assertEquals("1.5 кВт", formatCharacteristicValueForProductCard(1500.0, "W"))
    }

    @Test
    fun russian_watts_input() {
        assertEquals("1.5 кВт", formatCharacteristicValueForProductCard(1500.0, "Вт"))
    }

    @Test
    fun volts_to_millivolts() {
        assertEquals("1 мВ", formatCharacteristicValueForProductCard(0.001, "V"))
    }

    @Test
    fun grams_to_kilograms() {
        assertEquals("1 кг", formatCharacteristicValueForProductCard(1000.0, "g"))
    }

    @Test
    fun kilograms_stays_readable() {
        assertEquals("2 кг", formatCharacteristicValueForProductCard(2.0, "kg"))
    }

    @Test
    fun large_kilograms_to_megagrams() {
        assertEquals("2 Мг", formatCharacteristicValueForProductCard(2000.0, "kg"))
    }

    @Test
    fun millimeters_preserved() {
        assertEquals("5 мм", formatCharacteristicValueForProductCard(5.0, "mm"))
    }

    @Test
    fun decimeters_normalize_to_meters_not_dm() {
        assertEquals("1.5 м", formatCharacteristicValueForProductCard(15.0, "dm"))
    }

    @Test
    fun meters_to_kilometers() {
        assertEquals("5 км", formatCharacteristicValueForProductCard(5000.0, "m"))
    }

    @Test
    fun compound_unit_no_scaling() {
        assertEquals("10 м/с", formatCharacteristicValueForProductCard(10.0, "m/s"))
    }

    @Test
    fun prefixed_input_normalized() {
        assertEquals("2 кВт", formatCharacteristicValueForProductCard(2.0, "kW"))
    }

    @Test
    fun pascal_not_peta() {
        assertEquals("1.5 кПа", formatCharacteristicValueForProductCard(1500.0, "Pa"))
    }

    @Test
    fun mole_gets_milli_prefix() {
        assertEquals("1 ммоль", formatCharacteristicValueForProductCard(0.001, "mol"))
    }

    @Test
    fun microvolts() {
        assertEquals("5 мкВ", formatCharacteristicValueForProductCard(5e-6, "V"))
    }

    @Test
    fun gertz_whole_unit_not_giga() {
        assertEquals("5 Гц", formatCharacteristicValueForProductCard(5.0, "Гц"))
    }
}
