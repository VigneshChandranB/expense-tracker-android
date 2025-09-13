package com.expensetracker.data.backup

import android.content.Context
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling backup and restore operations
 */
@Singleton
class BackupService @Inject constructor(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .setPrettyPrinting()
        .create()
) {
    
    companion object {
        private const val BACKUP_VERSION = 1
        private const val BACKUP_EXTENSION = ".etb" // Expense Tracker Backup
        private const val BACKUP_DIR = "backups"
    }
    
    private val _backupProgress = MutableStateFlow(0)
    val backupProgress: Flow<Int> = _backupProgress.asStateFlow()
    
    private val _restoreProgress = MutableStateFlow(0)
    val restoreProgress: Flow<Int> = _restoreProgress.asStateFlow()
    
    /**
     * Creates a backup of all application data
     */
    suspend fun createBackup(): BackupResult {
        return try {
            _backupProgress.value = 0
            
            // Collect all data
            _backupProgress.value = 10
            val accounts = account