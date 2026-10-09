package com.armen.spark.data

import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

object SeedParser {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SeedIdea(
        val title: String,
        val description: String,
        val emoji: String,
        val category: String,
        val duration: Duration,
        val cost: Cost,
        val place: Place,
        val people: People,
        val energy: Energy,
    )

    /** Throws if [text] is not a JSON array; skips individual entries that are malformed. */
    fun parse(text: String): List<Idea> =
        json.parseToJsonElement(text).jsonArray
            .mapNotNull { element -> runCatching { json.decodeFromJsonElement(SeedIdea.serializer(), element) }.getOrNull() }
            .filter { it.title.isNotBlank() }
            .map {
                Idea(
                    title = it.title.trim(), description = it.description.trim(), emoji = it.emoji,
                    category = it.category, duration = it.duration, cost = it.cost,
                    place = it.place, people = it.people, energy = it.energy,
                )
            }
}
