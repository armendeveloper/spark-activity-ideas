package com.armen.spark.domain

enum class Duration(val label: String) { QUICK("15 min"), HOUR("About 1 h"), LONG("2 h+") }
enum class Cost(val label: String) { FREE("Free"), CHEAP("Cheap"), PRICEY("Pricey") }
enum class Place(val label: String) { INDOOR("Indoor"), OUTDOOR("Outdoor"), ANYWHERE("Anywhere") }
enum class People(val label: String) { SOLO("Alone"), GROUP("With others"), ANY("Alone or together") }
enum class Energy(val label: String) { CHILL("Chill"), ACTIVE("Active") }

val CATEGORIES = listOf("Creative", "Outdoors", "Social", "Learning", "Food", "Mindful", "Games", "Home")

data class Idea(
    val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,
    val duration: Duration,
    val cost: Cost,
    val place: Place,
    val people: People,
    val energy: Energy,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
)

data class HistoryItem(val entryId: Long, val idea: Idea, val doneAt: Long)
