package com.monster.literaryflow.data.db2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity2::class,
        StepEntity2::class,
        ActionEntity2::class,
        ConditionEntity2::class,
        RunRecordEntity2::class,
        StepRecordEntity2::class,
        AppProfileEntity2::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(LiteraryFlow2TypeConverters::class)
abstract class LiteraryFlow2Database : RoomDatabase() {
    abstract fun dao(): LiteraryFlow2Dao

    companion object {
        @Volatile
        private var INSTANCE: LiteraryFlow2Database? = null

        fun getDatabase(context: Context): LiteraryFlow2Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiteraryFlow2Database::class.java,
                    "literary_flow_2.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lf2_tasks ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE lf2_tasks ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE lf2_tasks ADD COLUMN estimated_duration_ms INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE lf2_tasks ADD COLUMN today_run_count INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lf2_conditions ADD COLUMN excluded_text TEXT")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lf2_app_profiles ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lf2_steps ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE lf2_steps ADD COLUMN wait_before_ms INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE lf2_steps ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 3")
            }
        }
    }
}
