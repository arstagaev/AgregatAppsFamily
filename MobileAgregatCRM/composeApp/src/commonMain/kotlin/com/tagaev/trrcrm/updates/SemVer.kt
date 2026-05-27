package com.tagaev.trrcrm.updates

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

object SemVerParser {
    private val regex = Regex("""^\s*(\d+)\.(\d+)\.(\d+)\s*$""")

    fun parseOrNull(raw: String?): SemVer? {
        val value = raw ?: return null
        val match = regex.matchEntire(value) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].toIntOrNull() ?: return null
        return SemVer(major, minor, patch)
    }
}

