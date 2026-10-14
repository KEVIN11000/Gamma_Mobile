package com.gamma.mobile.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatGuaranies() {
        assertEquals("0 ₲", formatGuaranies(0))
        assertEquals("1.000 ₲", formatGuaranies(1000))
        assertEquals("1.000.000 ₲", formatGuaranies(1000000))
        assertEquals("12.345 ₲", formatGuaranies(12345))
        assertEquals("-500 ₲", formatGuaranies(-500))
        assertEquals("-1.500 ₲", formatGuaranies(-1500))
        assertEquals("2.500.000.000 ₲", formatGuaranies(2500000000L))
    }
}
