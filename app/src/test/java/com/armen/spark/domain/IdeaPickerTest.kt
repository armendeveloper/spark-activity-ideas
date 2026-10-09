package com.armen.spark.domain

import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdeaPickerTest {
    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val picker = IdeaPicker(Random(42)) { now }

    private fun pickedIds(ideas: List<Idea>, filters: Filters, excludeId: Long? = null): Set<Long> =
        (1..300).mapNotNull { picker.pick(ideas, filters, emptyMap(), excludeId)?.id }.toSet()

    @Test
    fun returnsNullWhenNothingMatches() {
        val ideas = listOf(testIdea(1, energy = Energy.CHILL))
        assertNull(picker.pick(ideas, Filters(energy = Energy.ACTIVE), emptyMap()))
    }

    @Test
    fun durationFilterIncludesShorterIdeas() {
        val ideas = listOf(
            testIdea(1, duration = Duration.QUICK),
            testIdea(2, duration = Duration.HOUR),
            testIdea(3, duration = Duration.LONG),
        )
        assertEquals(setOf(1L, 2L), pickedIds(ideas, Filters(duration = Duration.HOUR)))
        assertEquals(setOf(1L), pickedIds(ideas, Filters(duration = Duration.QUICK)))
    }

    @Test
    fun costFilterIncludesCheaperIdeas() {
        val ideas = listOf(
            testIdea(1, cost = Cost.FREE),
            testIdea(2, cost = Cost.CHEAP),
            testIdea(3, cost = Cost.PRICEY),
        )
        assertEquals(setOf(1L), pickedIds(ideas, Filters(maxCost = Cost.FREE)))
        assertEquals(setOf(1L, 2L), pickedIds(ideas, Filters(maxCost = Cost.CHEAP)))
    }

    @Test
    fun anywhereIdeasMatchEitherPlace() {
        val ideas = listOf(
            testIdea(1, place = Place.INDOOR),
            testIdea(2, place = Place.OUTDOOR),
            testIdea(3, place = Place.ANYWHERE),
        )
        assertEquals(setOf(1L, 3L), pickedIds(ideas, Filters(place = Place.INDOOR)))
        assertEquals(setOf(2L, 3L), pickedIds(ideas, Filters(place = Place.OUTDOOR)))
    }

    @Test
    fun anyPeopleIdeasMatchSoloAndGroup() {
        val ideas = listOf(
            testIdea(1, people = People.SOLO),
            testIdea(2, people = People.GROUP),
            testIdea(3, people = People.ANY),
        )
        assertEquals(setOf(1L, 3L), pickedIds(ideas, Filters(people = People.SOLO)))
        assertEquals(setOf(2L, 3L), pickedIds(ideas, Filters(people = People.GROUP)))
    }

    @Test
    fun energyMatchesExactly() {
        val ideas = listOf(testIdea(1, energy = Energy.CHILL), testIdea(2, energy = Energy.ACTIVE))
        assertEquals(setOf(2L), pickedIds(ideas, Filters(energy = Energy.ACTIVE)))
    }

    @Test
    fun neverRepeatsTheExcludedIdea() {
        val ideas = listOf(testIdea(1), testIdea(2))
        assertEquals(setOf(2L), pickedIds(ideas, Filters(), excludeId = 1L))
    }

    @Test
    fun returnsExcludedIdeaWhenItIsTheOnlyMatch() {
        val ideas = listOf(testIdea(1))
        assertEquals(1L, picker.pick(ideas, Filters(), emptyMap(), excludeId = 1L)?.id)
    }

    @Test
    fun recentlyDoneIdeasAreAboutFiveTimesLessLikely() {
        val ideas = listOf(testIdea(1), testIdea(2))
        val lastDone = mapOf(1L to now - day)
        val picks = (1..10_000).map { picker.pick(ideas, Filters(), lastDone)!!.id }
        val ones = picks.count { it == 1L }
        // Expected share 0.2 / 1.2 ≈ 1667 of 10 000.
        assertTrue("picked recent idea $ones times", ones in 1400..1950)
    }

    @Test
    fun ideasDoneMoreThanTwoWeeksAgoAreNotPenalized() {
        val ideas = listOf(testIdea(1), testIdea(2))
        val lastDone = mapOf(1L to now - 15 * day)
        val ones = (1..10_000).count { picker.pick(ideas, Filters(), lastDone)!!.id == 1L }
        assertTrue("picked old idea $ones times", ones in 4600..5400)
    }
}
