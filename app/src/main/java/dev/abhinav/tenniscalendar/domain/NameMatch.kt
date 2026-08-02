package dev.abhinav.tenniscalendar.domain

import java.text.Normalizer

/**
 * Accent-insensitive player-name matching, ported from the web `PlayerSearch`. A query matches when,
 * after stripping diacritics and lowercasing, it is contained in either the full name or the
 * abbreviated name — so "carlos", "alcaraz", and "c. alcaraz" all match "Carlos Alcaraz".
 */
object NameMatch {

    fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()

    /**
     * The folded surname (last whitespace token), so "C. Alcaraz", "Carlos Alcaraz" and "Alcaraz"
     * all reduce to "alcaraz". Used as a lenient key when the exact abbreviated form differs between
     * the results feed and the player/country list.
     */
    fun surname(text: String): String {
        val folded = fold(text)
        return folded.substringAfterLast(' ').trim(' ', '.').ifBlank { folded }
    }

    fun matches(query: String, fullName: String, abbreviated: String): Boolean {
        val q = fold(query)
        if (q.isEmpty()) return false
        return fold(fullName).contains(q) || fold(abbreviated).contains(q)
    }
}
