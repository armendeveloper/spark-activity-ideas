package com.armen.spark.domain

import kotlin.random.Random

class IdeaPicker(
    private val random: Random = Random.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Picks a random idea that matches [filters], weighting down ideas done in the last 14 days.
     * [excludeId] (the idea currently shown) is skipped unless it is the only match.
     */
    fun pick(
        candidates: List<Idea>,
        filters: Filters,
        lastDone: Map<Long, Long>,
        excludeId: Long? = null,
    ): Idea? {
        val matching = candidates.filter(filters::matches)
        if (matching.isEmpty()) return null
        val pool = matching.filter { it.id != excludeId }.ifEmpty { matching }

        val cutoff = now() - RECENT_WINDOW_MS
        val weights = pool.map { idea ->
            val doneAt = lastDone[idea.id]
            if (doneAt != null && doneAt >= cutoff) RECENT_WEIGHT else 1.0
        }
        var roll = random.nextDouble() * weights.sum()
        for (i in pool.indices) {
            roll -= weights[i]
            if (roll < 0) return pool[i]
        }
        return pool.last()
    }

    companion object {
        const val RECENT_WEIGHT = 0.2
        const val RECENT_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }
}
