package dev.abhinav.tenniscalendar

import dev.abhinav.tenniscalendar.work.TournamentSyncLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentSyncLogicTest {

    @Test
    fun `first run seeds and notifies nothing`() {
        val outcome = TournamentSyncLogic.decide(
            completedIds = listOf("a", "b", "c"),
            seen = emptySet(),
            initialized = false,
        )
        assertTrue(outcome.seedOnly)
        assertTrue(outcome.toNotify.isEmpty())
    }

    @Test
    fun `notifies only tournaments completed since last seen`() {
        val outcome = TournamentSyncLogic.decide(
            completedIds = listOf("a", "b", "c", "d"),
            seen = setOf("a", "b"),
            initialized = true,
        )
        assertEquals(listOf("c", "d"), outcome.toNotify)
        assertTrue(!outcome.seedOnly)
    }

    @Test
    fun `no new completions notifies nothing`() {
        val outcome = TournamentSyncLogic.decide(
            completedIds = listOf("a", "b"),
            seen = setOf("a", "b"),
            initialized = true,
        )
        assertTrue(outcome.toNotify.isEmpty())
    }
}
