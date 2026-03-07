package com.sekusarisu.yanami.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sekusarisu.yanami.data.local.dao.ServerInstanceDao
import com.sekusarisu.yanami.data.local.entity.ServerInstanceEntity

/** v2→v3 迁移：添加 API Key 认证支持 */
val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                        "ALTER TABLE server_instances ADD COLUMN auth_type TEXT NOT NULL DEFAULT 'PASSWORD'"
                )
                db.execSQL("ALTER TABLE server_instances ADD COLUMN encrypted_api_key TEXT")
            }
        }

/** Yanami Room 数据库 — v3：添加 API Key 认证模式 */
@Database(entities = [ServerInstanceEntity::class], version = 3, exportSchema = false)
abstract class YanamiDatabase : RoomDatabase() {
    abstract fun serverInstanceDao(): ServerInstanceDao
}
