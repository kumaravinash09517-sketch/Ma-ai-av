package com.sarkar.jarviscall.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class CallActionType {
    SCREEN_AUTOMATICALLY,
    ASK_CALLER_NAME,
    ASK_REASON_FOR_CALLING,
    DECLINE_AND_BLOCK,
    ACCEPT_AND_NOTIFY,
    TAKE_MESSAGE,
    CUSTOM_RESPONSE
}

enum class ContactFilterType {
    WHITELIST,
    BLACKLIST
}

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val actionTaken: String,
    val transcriptJson: String, // JSON array of turns: [{"sender": "Caller", "text": "..."}, {"sender": "AI", "text": "..."}]
    val categoryTag: String = "Inquiry", // Spam, Urgent, Inquiry, Personal, Delivery
    val isSpam: Boolean = false,
    val summaryNote: String = ""
)

@Entity(tableName = "dialogue_rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val keywordsCsv: String, // Comma separated, e.g., "loan,credit,bank,offer"
    val responseText: String,
    val actionType: CallActionType,
    val isEnabled: Boolean = true,
    val priority: Int = 1
)

@Entity(tableName = "contact_filters")
data class ContactFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String = "Unknown",
    val filterType: ContactFilterType,
    val note: String = ""
)

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity): Long

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM dialogue_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM dialogue_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getActiveRulesSync(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Query("DELETE FROM dialogue_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}

@Dao
interface ContactFilterDao {
    @Query("SELECT * FROM contact_filters ORDER BY id DESC")
    fun getAllContactFilters(): Flow<List<ContactFilterEntity>>

    @Query("SELECT * FROM contact_filters WHERE phoneNumber = :number LIMIT 1")
    suspend fun getFilterByNumber(number: String): ContactFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: ContactFilterEntity): Long

    @Query("DELETE FROM contact_filters WHERE id = :id")
    suspend fun deleteFilterById(id: Long)
}

@Database(
    entities = [CallLogEntity::class, RuleEntity::class, ContactFilterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun ruleDao(): RuleDao
    abstract fun contactFilterDao(): ContactFilterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sarkar_jarvis_call_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
