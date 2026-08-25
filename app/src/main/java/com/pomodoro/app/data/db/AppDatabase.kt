package com.pomodoro.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pomodoro.app.data.model.PomodoroSession
import com.pomodoro.app.data.model.Task

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tasks ADD COLUMN focusDuration INTEGER NOT NULL DEFAULT 25")
        database.execSQL("ALTER TABLE tasks ADD COLUMN shortBreakDuration INTEGER NOT NULL DEFAULT 5")
        database.execSQL("ALTER TABLE tasks ADD COLUMN longBreakDuration INTEGER NOT NULL DEFAULT 15")
        database.execSQL("ALTER TABLE tasks ADD COLUMN sessionsBeforeLongBreak INTEGER NOT NULL DEFAULT 4")
        database.execSQL("ALTER TABLE tasks ADD COLUMN presetName TEXT")
    }
}

@Database(entities = [Task::class, PomodoroSession::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pomodoro_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
}
