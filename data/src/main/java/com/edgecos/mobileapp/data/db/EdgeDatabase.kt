package com.edgecos.mobileapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * EdgeCOS 本地数据库。
 *
 * 实体：[FileNodeEntity]、[UploadTaskEntity]、[AnnouncementEntity]、[TransferRecordEntity]
 * 后续阶段将补充：OfflineTaskEntity、WorkOrderEntity、WalletBillEntity
 */
@Database(
    entities = [
        FileNodeEntity::class,
        UploadTaskEntity::class,
        AnnouncementEntity::class,
        TransferRecordEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class EdgeDatabase : RoomDatabase() {
    abstract fun fileNodeDao(): FileNodeDao
    abstract fun uploadTaskDao(): UploadTaskDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun transferRecordDao(): TransferRecordDao

    companion object {
        @Volatile
        private var instance: EdgeDatabase? = null

        fun get(context: Context): EdgeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EdgeDatabase::class.java,
                "edgecos.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
