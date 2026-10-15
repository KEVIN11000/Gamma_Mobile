package com.gamma.mobile.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemverUtilsTest {

    @Test
    fun parseVersion_handlesStandardSemver() {
        val (major, minor, patch) = SemverUtils.parseVersion("0.0.4")
        assertEquals(0, major)
        assertEquals(0, minor)
        assertEquals(4, patch)
    }

    @Test
    fun parseVersion_stripsPrefixVAndGitHash() {
        val (major, minor, patch) = SemverUtils.parseVersion("v0.0.4+6bb982e")
        assertEquals(0, major)
        assertEquals(0, minor)
        assertEquals(4, patch)
    }

    @Test
    fun isNewer_returnsTrueWhenRemotePatchIsHigher() {
        assertTrue(SemverUtils.isNewer("v0.0.5", "0.0.4+6bb982e"))
        assertTrue(SemverUtils.isNewer("0.0.5", "0.0.4"))
    }

    @Test
    fun isNewer_returnsTrueWhenRemoteMinorIsHigher() {
        assertTrue(SemverUtils.isNewer("v0.1.0", "0.0.9+commit"))
        assertTrue(SemverUtils.isNewer("1.0.0", "0.9.9"))
    }

    @Test
    fun isNewer_returnsFalseWhenVersionsAreEqual() {
        assertFalse(SemverUtils.isNewer("v0.0.4", "0.0.4+6bb982e"))
        assertFalse(SemverUtils.isNewer("0.0.4", "0.0.4"))
    }

    @Test
    fun isNewer_returnsFalseWhenRemoteIsOlder() {
        assertFalse(SemverUtils.isNewer("v0.0.3", "0.0.4+6bb982e"))
        assertFalse(SemverUtils.isNewer("0.0.1", "0.0.4"))
    }
}
