package dev.abhinav.tenniscalendar.domain

import dev.abhinav.tenniscalendar.model.Tournament
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * The Monday–Sunday week containing a given day, and the tournaments overlapping it. Shared by the
 * in-app "This week" card and the home-screen widget so both agree on what "this week" means.
 */
data class WeekWindow(val start: LocalDate, val end: LocalDate) {

    /** e.g. "Sep 14 – Sep 20". */
    val label: String get() = "${start.format(LABEL_FORMAT)} – ${end.format(LABEL_FORMAT)}"

    /** Tournaments whose [start, end] range overlaps this week, earliest start first. */
    fun tournamentsIn(list: List<Tournament>): List<Tournament> = list
        .mapNotNull { t -> parse(t.start)?.let { s -> parse(t.end)?.let { e -> Triple(t, s, e) } } }
        .filter { (_, s, e) -> !s.isAfter(end) && !e.isBefore(start) }
        .sortedBy { it.second }
        .map { it.first }

    companion object {
        private val LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d")

        fun containing(day: LocalDate): WeekWindow {
            val start = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return WeekWindow(start, start.plusDays(6))
        }

        private fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()
    }
}
