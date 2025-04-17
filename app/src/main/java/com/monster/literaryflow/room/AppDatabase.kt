package com.monster.literaryflow.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monster.literaryflow.bean.AutoInfo

@Database(entities = [AutoInfo::class], version = 4, exportSchema = false)
@TypeConverters(AutoInfoConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun autoInfoDao(): AutoInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 在现有表上添加新的列 monitor_list
                database.execSQL("ALTER TABLE auto_info ADD COLUMN monitor_list TEXT")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 在现有表上添加新的列
                database.execSQL("ALTER TABLE auto_info ADD COLUMN loop_type TEXT")
                database.execSQL("ALTER TABLE auto_info ADD COLUMN week_data TEXT")
                database.execSQL("ALTER TABLE auto_info ADD COLUMN sleep_time INTEGER")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auto_info_database"
                )
                    .addMigrations(*MIGRATIONS)  // 添加迁移逻辑
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
