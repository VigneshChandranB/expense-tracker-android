package com.expensetracker.data.local.optimization

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.local.database.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database optimizer for improving query performance and managing indexes
 */
@Singleton
class DatabaseOptimizer @Inject constructor(
    private val database: ExpenseDatabase
) {
    
    companion object {
        // Index creation queries for optimal performance
        private val PERFORMANCE_INDEXES = listOf(
            // Transaction indexes for common queries
            "CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(accountId)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(categoryId)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_amount ON transactions(amount)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_date_account ON transactions(date, accountId)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_account_category ON transactions(accountId, categoryId)",
            
            // Account indexes
            "CREATE INDEX IF NOT EXISTS idx_accounts_bank_name ON accounts(bankName)",
            "CREATE INDEX IF NOT EXISTS idx_accounts_is_active ON accounts(isActive)",
            "CREATE INDEX IF NOT EXISTS idx_accounts_account_type ON accounts(accountType)",
            
            // Category indexes
            "CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name)",
            "CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories(parentCategoryId)",
            
            // SMS pattern indexes
            "CREATE INDEX IF NOT EXISTS idx_sms_patterns_bank ON sms_patterns(bankName)",
            "CREATE INDEX IF NOT EXISTS idx_sms_patterns_active ON sms_patterns(isActive)",
            
            // Settings indexes
            "CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(settingKey)",
            
            // Notification indexes
            "CREATE INDEX IF NOT EXISTS idx_notifications_account ON notifications(accountId)",
            "CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type)",
            "CREATE INDEX IF NOT EXISTS idx_notifications_enabled ON notifications(isEnabled)"
        )
        
        // Analyze queries for query planner optimization
        private val ANALYZE_QUERIES = listOf(
            "ANALYZE transactions",
            "ANALYZE accounts", 
            "ANALYZE categories",
            "ANALYZE sms_patterns"
        )
    }
    
    /**
     * Create performance indexes for optimal query execution
     */
    suspend fun createPerformanceIndexes() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.apply {
            beginTransaction()
            try {
                PERFORMANCE_INDEXES.forEach { indexQuery ->
                    execSQL(indexQuery)
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }
    
    /**
     * Update database statistics for query optimizer
     */
    suspend fun updateDatabaseStatistics() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.apply {
            ANALYZE_QUERIES.forEach { analyzeQuery ->
                execSQL(analyzeQuery)
            }
        }
    }
    
    /**
     * Optimize database by running VACUUM and updating statistics
     */
    suspend fun optimizeDatabase() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.apply {
            // VACUUM to reclaim space and defragment
            execSQL("VACUUM")
            
            // Update statistics
            updateDatabaseStatistics()
        }
    }
    
    /**
     * Get database size and performance metrics
     */
    suspend fun getDatabaseMetrics(): DatabaseMetrics = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        
        val pageCount = db.rawQuery("PRAGMA page_count", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        
        val pageSize = db.rawQuery("PRAGMA page_size", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        
        val freePages = db.rawQuery("PRAGMA freelist_count", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        
        val transactionCount = db.rawQuery("SELECT COUNT(*) FROM transactions", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        
        val accountCount = db.rawQuery("SELECT COUNT(*) FROM accounts", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        
        DatabaseMetrics(
            totalSizeBytes = pageCount * pageSize,
            freeSpaceBytes = freePages * pageSize,
            transactionCount = transactionCount,
            accountCount = accountCount,
            pageSize = pageSize,
            pageCount = pageCount
        )
    }
    
    /**
     * Check if indexes exist and are being used effectively
     */
    suspend fun validateIndexes(): IndexValidationResult = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        
        val existingIndexes = mutableListOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='index'", null).use { cursor ->
            while (cursor.moveToNext()) {
                existingIndexes.add(cursor.getString(0))
            }
        }
        
        val missingIndexes = PERFORMANCE_INDEXES.mapNotNull { indexQuery ->
            val indexName = extractIndexName(indexQuery)
            if (!existingIndexes.contains(indexName)) indexName else null
        }
        
        IndexValidationResult(
            existingIndexes = existingIndexes,
            missingIndexes = missingIndexes,
            recommendedIndexes = PERFORMANCE_INDEXES
        )
    }
    
    private fun extractIndexName(indexQuery: String): String {
        return indexQuery.substringAfter("INDEX IF NOT EXISTS ").substringBefore(" ON")
    }
}

/**
 * Database performance metrics
 */
data class DatabaseMetrics(
    val totalSizeBytes: Long,
    val freeSpaceBytes: Long,
    val transactionCount: Long,
    val accountCount: Long,
    val pageSize: Long,
    val pageCount: Long
) {
    val usedSpaceBytes: Long get() = totalSizeBytes - freeSpaceBytes
    val fragmentationPercentage: Double get() = (freeSpaceBytes.toDouble() / totalSizeBytes) * 100
}

/**
 * Index validation results
 */
data class IndexValidationResult(
    val existingIndexes: List<String>,
    val missingIndexes: List<String>,
    val recommendedIndexes: List<String>
) {
    val isOptimal: Boolean get() = missingIndexes.isEmpty()
}

/**
 * Database callback for creating indexes on database creation
 */
class OptimizedDatabaseCallback(
    private val optimizer: DatabaseOptimizer
) : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Create performance indexes immediately after database creation
        DatabaseOptimizer.PERFORMANCE_INDEXES.forEach { indexQuery ->
            db.execSQL(indexQuery)
        }
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        
        // Update statistics when database is opened
        DatabaseOptimizer.ANALYZE_QUERIES.forEach { analyzeQuery ->
            db.execSQL(analyzeQuery)
        }
    }
}