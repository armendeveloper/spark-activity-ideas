package com.armen.spark.ui.edit

import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

data class IdeaDraft(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val emoji: String = DEFAULT_EMOJI,
    val category: String = CATEGORIES.first(),
    val duration: Duration = Duration.HOUR,
    val cost: Cost = Cost.FREE,
    val place: Place = Place.ANYWHERE,
    val people: People = People.ANY,
    val energy: Energy = Energy.CHILL,
    val isFavorite: Boolean = true,
) {
    val canSave: Boolean get() = title.isNotBlank()

    fun toIdeaOrNull(): Idea? = if (!canSave) null else Idea(
        id = id,
        title = title.trim(),
        description = description.trim(),
        emoji = emoji.trim().ifEmpty { DEFAULT_EMOJI },
        category = category,
        duration = duration,
        cost = cost,
        place = place,
        people = people,
        energy = energy,
        isCustom = true,
        isFavorite = isFavorite,
    )

    companion object {
        const val DEFAULT_EMOJI = "✨"

        fun from(idea: Idea) = IdeaDraft(
            id = idea.id, title = idea.title, description = idea.description, emoji = idea.emoji,
            category = idea.category, duration = idea.duration, cost = idea.cost, place = idea.place,
            people = idea.people, energy = idea.energy, isFavorite = idea.isFavorite,
        )
    }
}
