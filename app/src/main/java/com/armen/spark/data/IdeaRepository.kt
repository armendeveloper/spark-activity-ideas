package com.armen.spark.data

import android.util.Log
import androidx.room.withTransaction
import com.armen.spark.domain.HistoryItem
import com.armen.spark.domain.Idea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface IdeaRepository {
    val ideas: Flow<List<Idea>>
    /** Favorites plus all custom ideas. */
    val saved: Flow<List<Idea>>
    val history: Flow<List<HistoryItem>>
    suspend fun allIdeas(): List<Idea>
    suspend fun getIdea(id: Long): Idea?
    suspend fun lastDoneMap(): Map<Long, Long>
    suspend fun toggleFavorite(id: Long)
    suspend fun markDone(id: Long)
    suspend fun saveCustom(idea: Idea)
    suspend fun deleteCustom(id: Long)
    suspend fun seedIfEmpty()
}

class RoomIdeaRepository(
    private val db: SparkDatabase,
    private val loadSeed: () -> List<Idea>,
    private val now: () -> Long = System::currentTimeMillis,
) : IdeaRepository {
    private val ideaDao = db.ideaDao()
    private val doneDao = db.doneDao()
    private val seedMutex = Mutex()
    @Volatile private var seeded = false

    override val ideas: Flow<List<Idea>> = ideaDao.observeAll().map { list -> list.map { it.toDomain() } }
    override val saved: Flow<List<Idea>> = ideaDao.observeSaved().map { list -> list.map { it.toDomain() } }
    override val history: Flow<List<HistoryItem>> = doneDao.observeHistory().map { rows ->
        rows.map { HistoryItem(entryId = it.entryId, idea = it.idea.toDomain(), doneAt = it.doneAt) }
    }

    override suspend fun allIdeas(): List<Idea> = ideaDao.getAll().map { it.toDomain() }

    override suspend fun getIdea(id: Long): Idea? = ideaDao.getById(id)?.toDomain()

    override suspend fun lastDoneMap(): Map<Long, Long> =
        doneDao.lastDone().associate { it.ideaId to it.lastDoneAt }

    override suspend fun toggleFavorite(id: Long) {
        ideaDao.toggleFavorite(id)
    }

    override suspend fun markDone(id: Long) {
        doneDao.insert(DoneEntryEntity(ideaId = id, doneAt = now()))
    }

    override suspend fun saveCustom(idea: Idea) {
        ideaDao.upsert(idea.copy(isCustom = true).toEntity())
    }

    override suspend fun deleteCustom(id: Long) {
        db.withTransaction {
            doneDao.deleteForIdea(id)
            ideaDao.deleteCustom(id)
        }
    }

    override suspend fun seedIfEmpty() {
        seedMutex.withLock {
            if (seeded) return
            if (ideaDao.count() == 0) {
                val seed = withContext(Dispatchers.IO) {
                    runCatching(loadSeed)
                        .onFailure { Log.e(TAG, "Could not load bundled ideas", it) }
                        .getOrDefault(emptyList())
                }
                ideaDao.insertAll(seed.map { it.toEntity() })
            }
            seeded = true
        }
    }

    private companion object {
        const val TAG = "IdeaRepository"
    }
}
