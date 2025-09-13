package com.expensetracker.data.security

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.database.DatabaseMigrations
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages database encryption using SQLCipher and Android Keystore
 */
@Singleton
class DatabaseEncryptionManager @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val securePreferencesManager: SecurePreferencesManager
) {
    
    companion object {
        private const val DATABASE_KEY_ALIAS = "expense_tracker_db_key"
        private const val PASSPHRASE_LENGTH = 32
    }
    
    /**
     * Creates an encrypted Room database instance
     */
    fun createEncryptedDatabase(context: Context): ExpenseDatabase {
        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(context)
        
        // Get or generate database encryption key
        val passphrase = getDatabasePassphrase()
        
        return Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            ExpenseDatabase.DATABASE_NAME
        )
            .openHelperFactory { configuration ->
                SupportFactory(passphrase).create(configuration)
            }
            .addCallback(ExpenseDatabase.databaseCallback)
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()
    }
    
    /**
     * Gets the database passphrase from Android Keystore
     */
    private fun getDatabasePassphrase(): ByteArray {
        return if (keystoreManager.keyExists(DATABASE_KEY_ALIAS)) {
            // Retrieve existing passphrase (stored as encrypted data)
            val encryptedPassphrase = getStoredPassphrase()
            if (encryptedPassphrase != null) {
                keystoreManager.decrypt(encryptedPassphrase, DATABASE_KEY_ALIAS)
            } else {
                // Generate new passphrase if stored one is missing
                val newPassphrase = generateSecurePassphrase()
                val encryptedPassphrase = keystoreManager.encrypt(newPassphrase, DATABASE_KEY_ALIAS)
                storePassphrase(encryptedPassphrase)
                newPassphrase
            }
        } else {
            // Generate new passphrase and store it encrypted
            val newPassphrase = generateSecurePassphrase()
            val encryptedPassphrase = keystoreManager.encrypt(newPassphrase, DATABASE_KEY_ALIAS)
            storePassphrase(encryptedPassphrase)
            newPassphrase
        }
    }
    
    /**
     * Generates a cryptographically secure passphrase
     */
    private fun generateSecurePassphrase(): ByteArray {
        val secureRandom = SecureRandom()
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        secureRandom.nextBytes(passphrase)
        return passphrase
    }
    
    /**
     * Rotates the database encryption key
     */
    fun rotateDatabaseKey(context: Context): Boolean {
        return try {
            // Generate new passphrase
            val newPassphrase = generateSecurePassphrase()
            
            // Encrypt and store new passphrase
            val newKey = keystoreManager.rotateKey(DATABASE_KEY_ALIAS)
            val encryptedPassphrase = keystoreManager.encrypt(newPassphrase, DATABASE_KEY_ALIAS)
            storePassphrase(encryptedPassphrase)
            
            // Re-key the database with new passphrase
            rekeyDatabase(context, newPassphrase)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Re-keys the SQLCipher database with a new passphrase
     */
    private fun rekeyDatabase(context: Context, newPassphrase: ByteArray) {
        val databasePath = context.getDatabasePath(ExpenseDatabase.DATABASE_NAME).absolutePath
        val database = SQLiteDatabase.openDatabase(
            databasePath,
            getDatabasePassphrase(),
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        
        database.use { db ->
            db.rawExecSQL("PRAGMA rekey = x'${newPassphrase.toHexString()}'")
        }
    }
    
    /**
     * Validates database integrity
     */
    fun validateDatabaseIntegrity(context: Context): DatabaseIntegrityResult {
        return try {
            val database = createEncryptedDatabase(context)
            
            // Perform integrity check
            val integrityCheck = database.openHelper.readableDatabase.use { db ->
                val cursor = db.rawQuery("PRAGMA integrity_check", null)
                cursor.use {
                    if (it.moveToFirst()) {
                        it.getString(0) == "ok"
                    } else {
                        false
                    }
                }
            }
            
            // Perform cipher integrity check
            val cipherCheck = database.openHelper.readableDatabase.use { db ->
                val cursor = db.rawQuery("PRAGMA cipher_integrity_check", null)
                cursor.use {
                    if (it.moveToFirst()) {
                        it.getString(0) == "ok"
                    } else {
                        false
                    }
                }
            }
            
            database.close()
            
            when {
                integrityCheck && cipherCheck -> DatabaseIntegrityResult.VALID
                !integrityCheck -> DatabaseIntegrityResult.CORRUPTED
                !cipherCheck -> DatabaseIntegrityResult.ENCRYPTION_COMPROMISED
                else -> DatabaseIntegrityResult.UNKNOWN_ERROR
            }
        } catch (e: Exception) {
            DatabaseIntegrityResult.ACCESS_ERROR
        }
    }
    
    /**
     * Securely wipes the database
     */
    fun secureWipeDatabase(context: Context): Boolean {
        return try {
            val databasePath = context.getDatabasePath(ExpenseDatabase.DATABASE_NAME)
            
            // Delete the database file
            if (databasePath.exists()) {
                databasePath.delete()
            }
            
            // Delete associated files
            val walFile = context.getDatabasePath("${ExpenseDatabase.DATABASE_NAME}-wal")
            val shmFile = context.getDatabasePath("${ExpenseDatabase.DATABASE_NAME}-shm")
            
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            
            // Delete stored passphrase
            clearStoredPassphrase()
            
            // Delete keystore key
            keystoreManager.deleteKey(DATABASE_KEY_ALIAS)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getStoredPassphrase(): EncryptedData? {
        return securePreferencesManager.getDatabasePassphrase()
    }
    
    private fun storePassphrase(encryptedPassphrase: EncryptedData) {
        securePreferencesManager.storeDatabasePassphrase(encryptedPassphrase)
    }
    
    private fun clearStoredPassphrase() {
        securePreferencesManager.clearDatabasePassphrase()
    }
    
    /**
     * Extension function to convert ByteArray to hex string
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

/**
 * SQLCipher support factory for Room
 */
class SupportFactory(private val passphrase: ByteArray) : androidx.sqlite.db.SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration): androidx.sqlite.db.SupportSQLiteOpenHelper {
        return SupportHelper(configuration, passphrase)
    }
}

/**
 * SQLCipher support helper for Room
 */
class SupportHelper(
    private val configuration: androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration,
    private val passphrase: ByteArray
) : androidx.sqlite.db.SupportSQLiteOpenHelper {
    
    private val delegate = object : SQLiteOpenHelper(
        configuration.context,
        configuration.name,
        null,
        configuration.version
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            configuration.callback.onCreate(SupportDatabase(db))
        }
        
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            configuration.callback.onUpgrade(SupportDatabase(db), oldVersion, newVersion)
        }
    }
    
    override val databaseName: String? = configuration.name
    override val readableDatabase: androidx.sqlite.db.SupportSQLiteDatabase
        get() = SupportDatabase(delegate.getReadableDatabase(passphrase))
    override val writableDatabase: androidx.sqlite.db.SupportSQLiteDatabase
        get() = SupportDatabase(delegate.getWritableDatabase(passphrase))
    
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }
    
    override fun close() {
        delegate.close()
    }
}

/**
 * SQLCipher database wrapper for Room
 */
class SupportDatabase(private val delegate: SQLiteDatabase) : androidx.sqlite.db.SupportSQLiteDatabase {
    // Implementation would wrap all SupportSQLiteDatabase methods to delegate to SQLCipher
    // This is a simplified version - full implementation would be extensive
    
    override fun compileStatement(sql: String): androidx.sqlite.db.SupportSQLiteStatement {
        TODO("Full implementation required")
    }
    
    override fun beginTransaction() = delegate.beginTransaction()
    override fun beginTransactionNonExclusive() = delegate.beginTransactionNonExclusive()
    override fun endTransaction() = delegate.endTransaction()
    override fun setTransactionSuccessful() = delegate.setTransactionSuccessful()
    override fun inTransaction(): Boolean = delegate.inTransaction()
    override fun isDbLockedByCurrentThread(): Boolean = delegate.isDbLockedByCurrentThread
    override fun yieldIfContendedSafely(): Boolean = delegate.yieldIfContendedSafely()
    override fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean = delegate.yieldIfContendedSafely(sleepAfterYieldDelay)
    override val version: Int get() = delegate.version
    override val maximumSize: Long get() = delegate.maximumSize
    override fun setMaximumSize(numBytes: Long): Long = delegate.setMaximumSize(numBytes)
    override val pageSize: Long get() = delegate.pageSize
    override fun setPageSize(numBytes: Long) = delegate.pageSize = numBytes
    override fun query(query: String): android.database.Cursor = delegate.rawQuery(query, null)
    override fun query(query: String, bindArgs: Array<out Any?>): android.database.Cursor = delegate.rawQuery(query, bindArgs.map { it?.toString() }?.toTypedArray())
    override fun query(query: androidx.sqlite.db.SupportSQLiteQuery): android.database.Cursor {
        TODO("Full implementation required")
    }
    override fun query(query: androidx.sqlite.db.SupportSQLiteQuery, cancellationSignal: android.os.CancellationSignal?): android.database.Cursor {
        TODO("Full implementation required")
    }
    override fun insert(table: String, conflictAlgorithm: Int, values: android.content.ContentValues): Long = delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = delegate.delete(table, whereClause, whereArgs?.map { it?.toString() }?.toTypedArray())
    override fun update(table: String, conflictAlgorithm: Int, values: android.content.ContentValues, whereClause: String?, whereArgs: Array<out Any?>?): Int = delegate.updateWithOnConflict(table, values, whereClause, whereArgs?.map { it?.toString() }?.toTypedArray(), conflictAlgorithm)
    override fun execSQL(sql: String) = delegate.execSQL(sql)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) = delegate.execSQL(sql, bindArgs)
    override fun isReadOnly(): Boolean = delegate.isReadOnly
    override fun isOpen(): Boolean = delegate.isOpen
    override fun needUpgrade(newVersion: Int): Boolean = delegate.needUpgrade(newVersion)
    override val path: String? get() = delegate.path
    override fun setLocale(locale: java.util.Locale) = delegate.setLocale(locale)
    override fun setMaxSqlCacheSize(cacheSize: Int) = delegate.setMaxSqlCacheSize(cacheSize)
    override fun setForeignKeyConstraintsEnabled(enable: Boolean) {
        if (enable) {
            delegate.execSQL("PRAGMA foreign_keys=ON")
        } else {
            delegate.execSQL("PRAGMA foreign_keys=OFF")
        }
    }
    override fun enableWriteAheadLogging(): Boolean = delegate.enableWriteAheadLogging()
    override fun disableWriteAheadLogging() = delegate.disableWriteAheadLogging()
    override fun isWriteAheadLoggingEnabled(): Boolean = delegate.isWriteAheadLoggingEnabled
    override val attachedDbs: List<android.util.Pair<String, String>>? get() = delegate.attachedDbs
    override fun isDatabaseIntegrityOk(): Boolean = delegate.isDatabaseIntegrityOk
    override fun close() = delegate.close()
}

/**
 * Database integrity check results
 */
enum class DatabaseIntegrityResult {
    VALID,
    CORRUPTED,
    ENCRYPTION_COMPROMISED,
    ACCESS_ERROR,
    UNKNOWN_ERROR
}