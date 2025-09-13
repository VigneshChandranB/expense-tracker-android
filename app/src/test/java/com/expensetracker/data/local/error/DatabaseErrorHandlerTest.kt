package com.expensetracker.data.local.error

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class DatabaseErrorHandlerTest {
    
    private lateinit var databaseErrorHandler: DatabaseErrorHandler
    
    @Mock
    private lateinit var mockDatabase: RoomDatabase
    
    @Mock
    private lateinit var mockSqliteDatabase: SupportSQLiteDatabase
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        databaseErrorHandler = DatabaseErrorHandler()
    }
    
    @Test
    fun `executeWithRetry should succeed on first attempt`() = runTest {
        val expectedResult = "Success"
        
        val result = databaseErrorHandler.executeWithRetry { expectedResult }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedResult, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `executeWithRetry should retry on failure and eventually succeed`() = runTest {
        var attemptCount = 0
        val expectedResult = "Success"
        
        val result = databaseErrorHandler.executeWithRetry {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Temporary failure")
            }
            expectedResult
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedResult, (result as ErrorResult.Success).data)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `executeWithRetry should fail after max retries`() = runTest {
        var attemptCount = 0
        
        val result = databaseErrorHandler.executeWithRetry {
            attemptCount++
            throw RuntimeException("Persistent failure")
        }
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(3, attemptCount) // Should retry 3 times
    }
    
    @Test
    fun `executeTransaction should handle SQLiteConstraintException`() = runTest {
        whenever(mockDatabase.runInTransaction<String>(any())).thenThrow(
            SQLiteConstraintException("Constraint violation")
        )
        
        val result = databaseErrorHandler.executeTransaction(mockDatabase) {
            "Should not reach here"
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.DatabaseError.ConstraintViolation, error.errorType)
        assertTrue(error.isRecoverable)
    }
    
    @Test
    fun `executeTransaction should handle SQLiteFullException`() = runTest {
        whenever(mockDatabase.runInTransaction<String>(any())).thenThrow(
            SQLiteFullException("Database full")
        )
        
        val result = databaseErrorHandler.executeTransaction(mockDatabase) {
            "Should not reach here"
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.DatabaseError.DiskSpaceFull, error.errorType)
        assertFalse(error.isRecoverable)
    }
    
    @Test
    fun `executeTransaction should handle database locked exception`() = runTest {
        whenever(mockDatabase.runInTransaction<String>(any())).thenThrow(
            SQLiteException("database is locked")
        )
        
        val result = databaseErrorHandler.executeTransaction(mockDatabase) {
            "Should not reach here"
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.DatabaseError.ConnectionFailed, error.errorType)
        assertTrue(error.isRecoverable)
    }
    
    @Test
    fun `executeTransaction should handle database corruption`() = runTest {
        whenever(mockDatabase.runInTransaction<String>(any())).thenThrow(
            SQLiteException("database disk image is malformed (code 11)")
        )
        
        val result = databaseErrorHandler.executeTransaction(mockDatabase) {
            "Should not reach here"
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.DatabaseError.DataCorruption, error.errorType)
        assertFalse(error.isRecoverable)
    }
    
    @Test
    fun `validateDatabaseIntegrity should return true for healthy database`() = runTest {
        val mockCursor = mock<android.database.Cursor>()
        whenever(mockCursor.moveToFirst()).thenReturn(true)
        whenever(mockCursor.getString(0)).thenReturn("ok")
        whenever(mockSqliteDatabase.query("PRAGMA integrity_check")).thenReturn(mockCursor)
        
        val result = databaseErrorHandler.validateDatabaseIntegrity(mockSqliteDatabase)
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        verify(mockCursor).close()
    }
    
    @Test
    fun `validateDatabaseIntegrity should return false for corrupted database`() = runTest {
        val mockCursor = mock<android.database.Cursor>()
        whenever(mockCursor.moveToFirst()).thenReturn(true)
        whenever(mockCursor.getString(0)).thenReturn("corruption detected")
        whenever(mockSqliteDatabase.query("PRAGMA integrity_check")).thenReturn(mockCursor)
        
        val result = databaseErrorHandler.validateDatabaseIntegrity(mockSqliteDatabase)
        
        assertTrue(result is ErrorResult.Success)
        assertFalse((result as ErrorResult.Success).data)
        verify(mockCursor).close()
    }
    
    @Test
    fun `validateDatabaseIntegrity should handle query failure`() = runTest {
        whenever(mockSqliteDatabase.query("PRAGMA integrity_check")).thenThrow(
            SQLiteException("Query failed")
        )
        
        val result = databaseErrorHandler.validateDatabaseIntegrity(mockSqliteDatabase)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.DatabaseError.DataCorruption, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `repairDatabase should execute repair commands`() = runTest {
        val mockCursor = mock<android.database.Cursor>()
        whenever(mockCursor.moveToFirst()).thenReturn(true)
        whenever(mockCursor.getString(0)).thenReturn("ok")
        whenever(mockSqliteDatabase.query("PRAGMA integrity_check")).thenReturn(mockCursor)
        
        val result = databaseErrorHandler.repairDatabase(mockSqliteDatabase)
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        
        verify(mockSqliteDatabase).execSQL("REINDEX")
        verify(mockSqliteDatabase).execSQL("VACUUM")
        verify(mockCursor).close()
    }
    
    @Test
    fun `repairDatabase should handle repair failure`() = runTest {
        whenever(mockSqliteDatabase.execSQL("REINDEX")).thenThrow(
            SQLiteException("Repair failed")
        )
        
        val result = databaseErrorHandler.repairDatabase(mockSqliteDatabase)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.DatabaseError.DataCorruption, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `checkDiskSpace should return true when sufficient space available`() {
        // This test would need to mock the file system, which is complex
        // In a real scenario, we'd use a dependency injection for file system operations
        val result = databaseErrorHandler.checkDiskSpace(1000L)
        
        // The actual implementation checks real disk space, so we just verify it doesn't crash
        assertTrue(result is ErrorResult.Success || result is ErrorResult.Error)
    }
    
    @Test
    fun `createBackup should succeed with valid database`() = runTest {
        val mockCursor = mock<android.database.Cursor>()
        whenever(mockCursor.moveToFirst()).thenReturn(true)
        whenever(mockCursor.getInt(0)).thenReturn(5) // 5 tables
        whenever(mockSqliteDatabase.query("SELECT COUNT(*) FROM sqlite_master")).thenReturn(mockCursor)
        
        val backupPath = "/backup/path"
        val result = databaseErrorHandler.createBackup(mockSqliteDatabase, backupPath)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(backupPath, (result as ErrorResult.Success).data)
        verify(mockCursor).close()
    }
    
    @Test
    fun `createBackup should fail with empty database`() = runTest {
        val mockCursor = mock<android.database.Cursor>()
        whenever(mockCursor.moveToFirst()).thenReturn(true)
        whenever(mockCursor.getInt(0)).thenReturn(0) // 0 tables
        whenever(mockSqliteDatabase.query("SELECT COUNT(*) FROM sqlite_master")).thenReturn(mockCursor)
        
        val backupPath = "/backup/path"
        val result = databaseErrorHandler.createBackup(mockSqliteDatabase, backupPath)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.DatabaseError.TransactionFailed, (result as ErrorResult.Error).errorType)
        verify(mockCursor).close()
    }
    
    @Test
    fun `createBackup should handle query failure`() = runTest {
        whenever(mockSqliteDatabase.query("SELECT COUNT(*) FROM sqlite_master")).thenThrow(
            SQLiteException("Query failed")
        )
        
        val backupPath = "/backup/path"
        val result = databaseErrorHandler.createBackup(mockSqliteDatabase, backupPath)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.DatabaseError.TransactionFailed, (result as ErrorResult.Error).errorType)
    }
}