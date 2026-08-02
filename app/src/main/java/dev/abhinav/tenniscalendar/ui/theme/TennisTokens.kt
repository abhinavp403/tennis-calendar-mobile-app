package dev.abhinav.tenniscalendar.ui.theme

import androidx.compose.ui.graphics.Color
import dev.abhinav.tenniscalendar.model.Surface
import dev.abhinav.tenniscalendar.model.Tour

/**
 * Tier / tour design tokens ported from the web `TournamentLogo.LEVEL_STYLES` and `App.jsx`.
 * The 1500 tier is tour-aware ("ATP Finals" / "WTA Finals").
 */
object TennisTokens {

    // Tier accent colors, tuned per theme: the light-theme variants are darker so they stay legible
    // on white chip backgrounds (the dark-theme tones washed out there).
    private val GrandSlamDark = Color(0xFFA78BFA) // 2000 & 1500 — violet
    private val MastersDark = Color(0xFFF59E0B)   // 1000 — amber
    private val FiveDark = Color(0xFF60A5FA)      // 500 — blue
    private val TwoFiftyDark = Color(0xFF9CA3AF)  // 250 — grey
    private val GrandSlamLight = Color(0xFF7C3AED)
    private val MastersLight = Color(0xFFB45309)
    private val FiveLight = Color(0xFF2563EB)
    private val TwoFiftyLight = Color(0xFF4B5563)

    // Tour accents.
    val AtpAccent = Color(0xFF0066CC)
    val WtaAccent = Color(0xFFBE398D)

    fun tierColor(level: Int, dark: Boolean = true): Color = when (level) {
        2000, 1500 -> if (dark) GrandSlamDark else GrandSlamLight
        1000 -> if (dark) MastersDark else MastersLight
        500 -> if (dark) FiveDark else FiveLight
        else -> if (dark) TwoFiftyDark else TwoFiftyLight
    }

    fun tourAccent(tour: Tour): Color = if (tour == Tour.ATP) AtpAccent else WtaAccent

    // Surface accent colors: clay red, grass green, hard light blue, indoor dark blue.
    fun surfaceColor(surface: Surface): Color = when (surface) {
        Surface.CLAY -> Color(0xFFEF4444)
        Surface.GRASS -> Color(0xFF22C55E)
        Surface.HARD -> Color(0xFF38BDF8)
        Surface.INDOOR_HARD -> Color(0xFF1E40AF)
    }

    /** Human label for a tier, tour-aware for the 1500 Finals. */
    fun levelLabel(level: Int, tour: Tour): String = when (level) {
        2000 -> "Grand Slam"
        1500 -> if (tour == Tour.ATP) "ATP Finals" else "WTA Finals"
        1000 -> if (tour == Tour.ATP) "Masters 1000" else "WTA 1000"
        500 -> "${tour.label} 500"
        250 -> "${tour.label} 250"
        else -> "$level"
    }

    /**
     * 2–3 letter fallback abbreviation for a tournament with no logo asset, mirroring the web
     * fallback badge: initials of the significant words, or the first three letters of a single word.
     */
    fun abbreviation(name: String): String {
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(3).uppercase()
            else -> words.take(3).joinToString("") { it.first().uppercase() }
        }
    }
}
