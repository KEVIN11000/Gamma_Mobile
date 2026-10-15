package com.gamma.mobile.domain.util

object SemverUtils {
    /**
     * Extrae la tupla (major, minor, patch) limpiando prefijos como 'v'/'V', sufijos de build '+hash' o '-beta'.
     */
    fun parseVersion(version: String): Triple<Int, Int, Int> {
        val clean = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("+")
            .substringBefore("-")
            .trim()
        val parts = clean.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    /**
     * Retorna verdadero si remoteVersion es estrictamente mayor que localVersion.
     */
    fun isNewer(remoteVersion: String, localVersion: String): Boolean {
        val (rMaj, rMin, rPatch) = parseVersion(remoteVersion)
        val (lMaj, lMin, lPatch) = parseVersion(localVersion)

        if (rMaj != lMaj) return rMaj > lMaj
        if (rMin != lMin) return rMin > lMin
        return rPatch > lPatch
    }
}
