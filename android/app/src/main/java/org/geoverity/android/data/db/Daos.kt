package org.geoverity.android.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineCaptureDao {

    @Query("SELECT * FROM offline_captures ORDER BY createdAt DESC")
    fun getAllOfflineCaptures(): Flow<List<OfflineCaptureEntity>>

    @Query("SELECT * FROM offline_captures WHERE status = 'PENDING'")
    suspend fun getPendingCaptures(): List<OfflineCaptureEntity>

    @Query("SELECT COUNT(*) FROM offline_captures WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: OfflineCaptureEntity): Long

    @Update
    suspend fun update(capture: OfflineCaptureEntity)

    @Delete
    suspend fun delete(capture: OfflineCaptureEntity)

    @Query("DELETE FROM offline_captures WHERE status = 'REJECTED_ANOMALY' OR status = 'SYNCED'")
    suspend fun purgeCompletedAndRejected()
}

@Dao
interface EvidenceHistoryDao {

    @Query("SELECT * FROM evidence_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<EvidenceHistoryEntity>>

    @Query("SELECT * FROM evidence_history WHERE verificationId = :verificationId LIMIT 1")
    suspend fun getByVerificationId(verificationId: String): EvidenceHistoryEntity?

    @Query("SELECT COUNT(*) FROM evidence_history")
    fun getTotalAuthenticatedCount(): Flow<Int>

    @Query("SELECT * FROM evidence_history ORDER BY createdAt DESC LIMIT 5")
    fun getRecentEvidence(): Flow<List<EvidenceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: EvidenceHistoryEntity): Long

    @Delete
    suspend fun delete(evidence: EvidenceHistoryEntity)
}
