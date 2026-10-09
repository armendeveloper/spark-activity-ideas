package com.armen.spark

import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

fun testIdea(
    id: Long,
    title: String = "Idea $id",
    duration: Duration = Duration.QUICK,
    cost: Cost = Cost.FREE,
    place: Place = Place.ANYWHERE,
    people: People = People.ANY,
    energy: Energy = Energy.CHILL,
    isCustom: Boolean = false,
    isFavorite: Boolean = false,
) = Idea(
    id = id, title = title, description = "Something to do", emoji = "🎯", category = "Games",
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)
