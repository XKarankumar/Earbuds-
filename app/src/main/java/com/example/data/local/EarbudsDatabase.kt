package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SoundProfile
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sound_profiles")
data class SoundProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val bandsCsv: String,
    val bassBoostPercent: Int,
    val isBuiltIn: Boolean,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): SoundProfile {
        val parsed = bandsCsv.split(",").mapNotNull { it.toFloatOrNull() }
        val normalized = if (parsed.size == 10) parsed else List(10) { 0f }
        return SoundProfile(
            id = id,
            name = name,
            subtitle = subtitle,
            bandsDb = normalized,
            bassBoostPercent = bassBoostPercent,
            isBuiltIn = isBuiltIn
        )
    }

    companion object {
        fun fromDomain(profile: SoundProfile): SoundProfileEntity {
            return SoundProfileEntity(
                id = profile.id,
                name = profile.name,
                subtitle = profile.subtitle,
                bandsCsv = profile.bandsDb.joinToString(","),
                bassBoostPercent = profile.bassBoostPercent,
                isBuiltIn = profile.isBuiltIn
            )
        }
    }
}

@Entity(tableName = "security_audit_logs")
data class SecurityAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val title: String,
    val detail: String,
    val deviceLabel: String,
    val isBlockedAttempt: Boolean
) {
    fun toDomain(): SecurityAuditLog = SecurityAuditLog(
        id = id,
        timestampMs = timestampMs,
        title = title,
        detail = detail,
        deviceLabel = deviceLabel,
        isBlockedAttempt = isBlockedAttempt
    )
}

@Dao
interface SoundProfileDao {
    @Query("SELECT * FROM sound_profiles ORDER BY isBuiltIn DESC, createdAtMs ASC")
    fun observeAllProfiles(): Flow<List<SoundProfileEntity>>

    @Query("SELECT COUNT(*) FROM sound_profiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<SoundProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SoundProfileEntity)

    @Query("DELETE FROM sound_profiles WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomProfile(id: String)
}

@Dao
interface SecurityAuditDao {
    @Query("SELECT * FROM security_audit_logs ORDER BY timestampMs DESC LIMIT 40")
    fun observeLogs(): Flow<List<SecurityAuditEntity>>

    @Query("SELECT COUNT(*) FROM security_audit_logs WHERE isBlockedAttempt = 1")
    fun observeBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_audit_logs")
    suspend fun getCount(): Int

    @Insert
    suspend fun insertLog(log: SecurityAuditEntity)

    @Query("DELETE FROM security_audit_logs")
    suspend fun clearLogs()
}

@Database(
    entities = [SoundProfileEntity::class, SecurityAuditEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EarbudsDatabase : RoomDatabase() {
    abstract fun soundProfileDao(): SoundProfileDao
    abstract fun securityAuditDao(): SecurityAuditDao

    companion object {
        @Volatile
        private var INSTANCE: EarbudsDatabase? = null

        fun getInstance(context: Context): EarbudsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EarbudsDatabase::class.java,
                    "aether_buds.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
