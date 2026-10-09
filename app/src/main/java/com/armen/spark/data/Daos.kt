package com.armen.spark.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<IdeaEntity>>

    /** Saved tab: favorites plus every custom idea, so your own ideas never get lost. */
    @Query("SELECT * FROM ideas WHERE isFavorite = 1 OR isCustom = 1 ORDER BY title COLLATE NOCASE")
    fun observeSaved(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas")
    suspend fun getAll(): List<IdeaEntity>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getById(id: Long): IdeaEntity?

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(ideas: List<IdeaEntity>)

    @Upsert
    suspend fun upsert(idea: IdeaEntity)

    @Query("UPDATE ideas SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM ideas WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: Long)
}

@Dao
interface DoneDao {
    @Insert
    suspend fun insert(entry: DoneEntryEntity)

    @Query("SELECT ideaId, MAX(doneAt) AS lastDoneAt FROM done_entries GROUP BY ideaId")
    suspend fun lastDone(): List<LastDone>

    @Query(
        "SELECT d.id AS entryId, d.doneAt AS doneAt, i.* FROM done_entries d " +
            "JOIN ideas i ON i.id = d.ideaId ORDER BY d.doneAt DESC",
    )
    fun observeHistory(): Flow<List<HistoryRow>>

    @Query("DELETE FROM done_entries WHERE ideaId = :ideaId")
    suspend fun deleteForIdea(ideaId: Long)
}
