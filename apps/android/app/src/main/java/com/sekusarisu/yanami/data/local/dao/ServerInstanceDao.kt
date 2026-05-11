package com.sekusarisu.yanami.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sekusarisu.yanami.data.local.entity.ServerInstanceEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO — 服务端实例数据访问 */
@Dao
interface ServerInstanceDao {

    /** 获取所有实例（按创建时间降序），返回 Flow 自动感知变化 */
    @Query("SELECT * FROM server_instances ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<ServerInstanceEntity>>

    /** 获取所有实例（一次性） */
    @Query("SELECT * FROM server_instances ORDER BY created_at DESC")
    suspend fun getAll(): List<ServerInstanceEntity>

    /** 获取当前激活的实例 */
    @Query("SELECT * FROM server_instances WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): ServerInstanceEntity?

    /** 获取当前激活的实例（Flow） */
    @Query("SELECT * FROM server_instances WHERE is_active = 1 LIMIT 1")
    fun getActiveFlow(): Flow<ServerInstanceEntity?>

    /** 根据 ID 获取实例 */
    @Query("SELECT * FROM server_instances WHERE id = :id")
    suspend fun getById(id: Long): ServerInstanceEntity?

    /** 插入新实例 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ServerInstanceEntity): Long

    /** 更新实例 */
    @Update suspend fun update(entity: ServerInstanceEntity)

    /** 删除实例 */
    @Delete suspend fun delete(entity: ServerInstanceEntity)

    /** 将所有实例设为非激活 */
    @Query("UPDATE server_instances SET is_active = 0") suspend fun deactivateAll()

    /** 将指定实例设为激活（先全部取消，再激活目标） */
    @Query("UPDATE server_instances SET is_active = 1 WHERE id = :id")
    suspend fun activateById(id: Long)

    /** 更新缓存的 session_token */
    @Query("UPDATE server_instances SET encrypted_session_token = :encryptedToken WHERE id = :id")
    suspend fun updateSessionToken(id: Long, encryptedToken: String?)
}
