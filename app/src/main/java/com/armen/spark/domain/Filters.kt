package com.armen.spark.domain

/** A null field means "any". Durations and costs match anything up to the selected value. */
data class Filters(
    val duration: Duration? = null,
    val maxCost: Cost? = null,
    val place: Place? = null,
    val people: People? = null,
    val energy: Energy? = null,
) {
    fun matches(idea: Idea): Boolean =
        (duration == null || idea.duration <= duration) &&
            (maxCost == null || idea.cost <= maxCost) &&
            (place == null || idea.place == place || idea.place == Place.ANYWHERE) &&
            (people == null || idea.people == people || idea.people == People.ANY) &&
            (energy == null || idea.energy == energy)
}
