package com.edgecos.mobileapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FileNodeDao {

    @Upsert
    suspend fun upsertAll(nodes: List<FileNodeEntity>)

    @Query("SELECT * FROM file_nodes WHERE path = :path ORDER BY type DESC, basename ASC")
    fun observeByPath(path: String): Flow<List<FileNodeEntity>>

    @Query("SELECT * FROM file_nodes WHERE path = :path ORDER BY type DESC, basename ASC")
    suspend fun getByPath(path: String): List<FileNodeEntity>

    @Query("DELETE FROM file_nodes WHERE path = :path OR path LIKE :pathPrefix")
    suspend fun deleteByPath(path: String, pathPrefix: String)

    @Query("DELETE FROM file_nodes")
    suspend fun clear()
}

@Dao
interface UploadTaskDao {

    @Upsert
    suspend fun upsert(task: UploadTaskEntity)

    @Query("SELECT * FROM upload_tasks WHERE taskId = :taskId")
    suspend fun get(taskId: String): UploadTaskEntity?

    @Query("SELECT * FROM upload_tasks WHERE status IN ('pending', 'uploading') ORDER BY createdAt ASC")
    fun observePending(): Flow<List<UploadTaskEntity>>

    @Query("SELECT * FROM upload_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UploadTaskEntity>>

    @Query("UPDATE upload_tasks SET progress = :progress, status = :status, updatedAt = :timestamp WHERE taskId = :taskId")
    suspend fun updateProgress(taskId: String, progress: Int, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM upload_tasks WHERE taskId = :taskId")
    suspend fun delete(taskId: String)
}

@Dao
interface AnnouncementDao {

    @Upsert
    suspend fun upsertAll(items: List<AnnouncementEntity>)

    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnnouncementEntity>>

    @Query("SELECT COUNT(*) FROM announcements WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE announcements SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("DELETE FROM announcements")
    suspend fun clear()
}

@Dao
interface TransferRecordDao {

    @Upsert
    suspend fun upsert(record: TransferRecordEntity)

    /** 按最后更新时间升序（与进程内列表行为一致：较新的记录靠后）。 */
    @Query("SELECT * FROM transfer_records ORDER BY updatedAt ASC")
    suspend fun getAll(): List<TransferRecordEntity>

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun delete(id: String)
}
