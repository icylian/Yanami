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

/** v3→v4 迁移：添加自定义请求头支持 */
val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE server_instances ADD COLUMN encrypted_custom_headers TEXT")
            }
        }

/** Yanami Room 数据库 — v4：添加自定义请求头支持 */
@Database(entities = [ServerInstanceEntity::class], version = 4, exportSchema = false)
abstract class YanamiDatabase : RoomDatabase() {
    abstract fun serverInstanceDao(): ServerInstanceDao
}
