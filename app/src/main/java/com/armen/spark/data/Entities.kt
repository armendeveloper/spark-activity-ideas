package com.armen.spark.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,
    val duration: Duration,
    val cost: Cost,
    val place: Place,
    val people: People,
    val energy: Energy,
    val isCustom: Boolean,
    val isFavorite: Boolean,
)

@Entity(
    tableName = "done_entries",
    foreignKeys = [
        ForeignKey(
            entity = IdeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ideaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ideaId")],
)
data class DoneEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideaId: Long,
    val doneAt: Long,
)

data class LastDone(val ideaId: Long, val lastDoneAt: Long)

data class HistoryRow(
    val entryId: Long,
    val doneAt: Long,
    @Embedded val idea: IdeaEntity,
)

fun IdeaEntity.toDomain() = Idea(
    id = id, title = title, description = description, emoji = emoji, category = category,
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)

fun Idea.toEntity() = IdeaEntity(
    id = id, title = title, description = description, emoji = emoji, category = category,
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)
