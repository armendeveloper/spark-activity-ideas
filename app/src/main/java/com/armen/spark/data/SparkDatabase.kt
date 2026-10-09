package com.armen.spark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IdeaEntity::class, DoneEntryEntity::class], version = 1, exportSchema = false)
abstract class SparkDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao
    abstract fun doneDao(): DoneDao

    companion object {
        fun build(context: Context): SparkDatabase =
            Room.databaseBuilder(context.applicationContext, SparkDatabase::class.java, "spark.db").build()
    }
}
