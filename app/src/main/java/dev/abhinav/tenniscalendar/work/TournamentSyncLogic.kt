package dev.abhinav.tenniscalendar.work

/** Pure decision logic for the sync worker, kept separate from Android APIs so it can be unit-tested. */
object TournamentSyncLogic {

    data class Outcome(
        /** Tournament ids to post a notification for. */
        val toNotify: List<String>,
        /** True on the very first run: record current state without notifying. */
        val seedOnly: Boolean,
    )

    fun decide(completedIds: List<String>, seen: Set<String>, initialized: Boolean): Outcome =
        if (!initialized) {
            Outcome(toNotify = emptyList(), seedOnly = true)
        } else {
            Outcome(toNotify = completedIds.filter { it !in seen }, seedOnly = false)
        }
}
