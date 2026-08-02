package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.domain.NameMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameMatchTest {

    @Test
    fun `first name, last name, and abbreviated form all match`() {
        val full = "Carlos Alcaraz"
        val abbr = "C. Alcaraz"
        assertTrue(NameMatch.matches("carlos", full, abbr))
        assertTrue(NameMatch.matches("alcaraz", full, abbr))
        assertTrue(NameMatch.matches("c. alcaraz", full, abbr))
        assertTrue(NameMatch.matches("novak", "Novak Djokovic", "N. Djokovic"))
    }

    @Test
    fun `matching is accent-insensitive`() {
        assertTrue(NameMatch.matches("muller", "Alexandre Müller", "A. Müller"))
        assertTrue(NameMatch.matches("machac", "Tomáš Macháč", "T. Macháč"))
    }

    @Test
    fun `unrelated query does not match`() {
        assertFalse(NameMatch.matches("federer", "Carlos Alcaraz", "C. Alcaraz"))
        assertFalse(NameMatch.matches("", "Carlos Alcaraz", "C. Alcaraz"))
    }

    @Test
    fun `surname reduces full, abbreviated and accented forms to one key`() {
        // Regardless of how a name is spelled in the results feed vs the country list, the surname
        // key lines up — this is what lets a manually-added country resolve to a flag.
        val expected = "dzumhur"
        assertEquals(expected, NameMatch.surname("D. Džumhur"))
        assertEquals(expected, NameMatch.surname("Damir Džumhur"))
        assertEquals(expected, NameMatch.surname("Džumhur"))
        assertEquals(expected, NameMatch.surname("d. dzumhur"))
        assertEquals("alcaraz", NameMatch.surname("C. Alcaraz"))
    }
}
