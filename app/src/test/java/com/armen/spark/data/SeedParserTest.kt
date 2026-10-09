package com.armen.spark.data

import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedParserTest {
    private fun entry(title: String, duration: String = "HOUR") =
        """{"title":"$title","description":"d","emoji":"📚","category":"Learning",
            "duration":"$duration","cost":"FREE","place":"INDOOR","people":"SOLO","energy":"CHILL"}"""

    @Test
    fun parsesValidEntries() {
        val ideas = SeedParser.parse("[${entry("Read a chapter")}]")
        assertEquals(1, ideas.size)
        with(ideas.single()) {
            assertEquals("Read a chapter", title)
            assertEquals(Duration.HOUR, duration)
            assertEquals(Cost.FREE, cost)
            assertEquals(Place.INDOOR, place)
            assertEquals(People.SOLO, people)
            assertEquals(Energy.CHILL, energy)
            assertFalse(isCustom)
            assertFalse(isFavorite)
        }
    }

    @Test
    fun skipsMalformedEntriesAndKeepsTheRest() {
        val missingTitle = """{"description":"d","emoji":"x","category":"Games","duration":"QUICK",
            "cost":"FREE","place":"INDOOR","people":"SOLO","energy":"CHILL"}"""
        val text = "[${entry("Good")}, ${entry("Bad enum", duration = "FOREVER")}, $missingTitle, ${entry("  ")}]"
        assertEquals(listOf("Good"), SeedParser.parse(text).map { it.title })
    }

    @Test
    fun bundledSeedIsCompleteAndValid() {
        val text = File("src/main/assets/activities.json").readText()
        val rawCount = Json.parseToJsonElement(text).jsonArray.size
        val ideas = SeedParser.parse(text)

        assertEquals("every bundled entry must parse", rawCount, ideas.size)
        assertTrue("need at least 150 ideas, have ${ideas.size}", ideas.size >= 150)
        assertEquals("titles must be unique", ideas.size, ideas.map { it.title.lowercase() }.toSet().size)
        assertEquals(CATEGORIES.toSet(), ideas.map { it.category }.toSet())
        assertTrue(ideas.all { it.emoji.isNotBlank() && it.description.isNotBlank() })

        val uiFilters = listOf(
            Filters(duration = Duration.QUICK), Filters(duration = Duration.HOUR),
            Filters(maxCost = Cost.FREE), Filters(maxCost = Cost.CHEAP),
            Filters(place = Place.INDOOR), Filters(place = Place.OUTDOOR),
            Filters(people = People.SOLO), Filters(people = People.GROUP),
            Filters(energy = Energy.CHILL), Filters(energy = Energy.ACTIVE),
        )
        uiFilters.forEach { f ->
            val count = ideas.count(f::matches)
            assertTrue("$f matches only $count ideas", count >= 10)
        }
    }
}
