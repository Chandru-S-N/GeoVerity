package org.geoverity.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineCaptureEntity::class, EvidenceHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoVerityDatabase : RoomDatabase() {

    abstract fun offlineCaptureDao(): OfflineCaptureDao
    abstract fun evidenceHistoryDao(): EvidenceHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: GeoVerityDatabase? = null

        fun getDatabase(context: Context): GeoVerityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoVerityDatabase::class.java,
                    "geoverity_evidence_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
