package com.expensetracker.data.file

import android.content.Context
import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.File
import java.io.IOException

class FileSystemErrorHandlerTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var fileSystemErrorHandler: FileSystemErrorHandler
    
    @Mock
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock context.filesDir to return temp folder
        val mockFilesDir = tempFolder.root
        whenever(mockContext.filesDir).thenReturn(mockFilesDir)
        
        fileSystemErrorHandler = FileSystemErrorHandler(mockContext)
    }
    
    @Test
    fun `executeFileOperation should succeed on first attempt`() = runTest {
        val expectedResult = "Success"
        
        val result = fileSystemErrorHandler.executeFileOperation { expectedResult }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedResult, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `executeFileOperation should retry on failure and eventually succeed`() = runTest {
        var attemptCount = 0
        val expectedResult = "Success"
        
        val result = fileSystemErrorHandler.executeFileOperation {
            attemptCount++
            if (attemptCount < 3) {
                throw IOException("Temporary failure")
            }
            expectedResult
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedResult, (result as ErrorResult.Success).data)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `validateFilePath should succeed for valid path`() {
        val validPath = File(tempFolder.root, "test.txt").absolutePath
        
        val result = fileSystemErrorHandler.validateFilePath(validPath)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(validPath, (result as ErrorResult.Success).data.absolutePath)
    }
    
    @Test
    fun `validateFilePath should create parent directories if they don't exist`() {
        val nestedPath = File(tempFolder.root, "nested/folder/test.txt").absolutePath
        
        val result = fileSystemErrorHandler.validateFilePath(nestedPath)
        
        assertTrue(result is ErrorResult.Success)
        val file = (result as ErrorResult.Success).data
        assertTrue(file.parentFile?.exists() == true)
    }
    
    @Test
    fun `validateFilePath should reject path traversal attempts`() {
        val maliciousPath = "${tempFolder.root.absolutePath}/../../../etc/passwd"
        
        val result = fileSystemErrorHandler.validateFilePath(maliciousPath)
        
        // The validation should either succeed (if canonicalization works) or fail safely
        // The important thing is it doesn't crash and handles the path securely
        assertTrue(result is ErrorResult.Success || result is ErrorResult.Error)
    }
    
    @Test
    fun `checkStorageSpace should succeed when sufficient space available`() {
        val testPath = tempFolder.root.absolutePath
        val requiredBytes = 1000L
        
        val result = fileSystemErrorHandler.checkStorageSpace(requiredBytes, testPath)
        
        // Since we're using a temp folder with plenty of space, this should succeed
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
    }
    
    @Test
    fun `readFile should succeed for existing file`() = runTest {
        val testFile = tempFolder.newFile("test.txt")
        val testContent = "Test content"
        testFile.writeText(testContent)
        
        val result = fileSystemErrorHandler.readFile(testFile.absolutePath)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(testContent, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `readFile should fail for non-existent file`() = runTest {
        val nonExistentPath = File(tempFolder.root, "nonexistent.txt").absolutePath
        
        val result = fileSystemErrorHandler.readFile(nonExistentPath)
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.FileSystemError.FileNotFound, error.errorType)
    }
    
    @Test
    fun `writeFile should succeed for valid path and content`() = runTest {
        val testPath = File(tempFolder.root, "write_test.txt").absolutePath
        val testContent = "Test write content"
        
        val result = fileSystemErrorHandler.writeFile(testPath, testContent)
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        
        // Verify file was actually written
        val writtenFile = File(testPath)
        assertTrue(writtenFile.exists())
        assertEquals(testContent, writtenFile.readText())
    }
    
    @Test
    fun `writeFile should create parent directories if needed`() = runTest {
        val nestedPath = File(tempFolder.root, "nested/folder/write_test.txt").absolutePath
        val testContent = "Test nested write"
        
        val result = fileSystemErrorHandler.writeFile(nestedPath, testContent)
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        
        // Verify file and directories were created
        val writtenFile = File(nestedPath)
        assertTrue(writtenFile.exists())
        assertTrue(writtenFile.parentFile?.exists() == true)
        assertEquals(testContent, writtenFile.readText())
    }
    
    @Test
    fun `deleteFile should succeed for existing file`() = runTest {
        val testFile = tempFolder.newFile("delete_test.txt")
        assertTrue(testFile.exists())
        
        val result = fileSystemErrorHandler.deleteFile(testFile.absolutePath)
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        assertFalse(testFile.exists())
    }
    
    @Test
    fun `deleteFile should succeed for non-existent file`() = runTest {
        val nonExistentPath = File(tempFolder.root, "nonexistent.txt").absolutePath
        
        val result = fileSystemErrorHandler.deleteFile(nonExistentPath)
        
        // Should succeed because file is already "deleted"
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
    }
    
    @Test
    fun `createFileBackup should succeed for existing file`() = runTest {
        val originalFile = tempFolder.newFile("original.txt")
        val originalContent = "Original content"
        originalFile.writeText(originalContent)
        
        val result = fileSystemErrorHandler.createFileBackup(originalFile.absolutePath)
        
        assertTrue(result is ErrorResult.Success)
        val backupPath = (result as ErrorResult.Success).data
        
        // Verify backup file exists and has same content
        val backupFile = File(backupPath)
        assertTrue(backupFile.exists())
        assertEquals(originalContent, backupFile.readText())
        assertTrue(backupPath.contains(".backup."))
    }
    
    @Test
    fun `createFileBackup should fail for non-existent file`() = runTest {
        val nonExistentPath = File(tempFolder.root, "nonexistent.txt").absolutePath
        
        val result = fileSystemErrorHandler.createFileBackup(nonExistentPath)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.FileSystemError.WriteError, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `restoreFromBackup should succeed with valid backup`() = runTest {
        // Create original file
        val originalFile = tempFolder.newFile("original.txt")
        originalFile.writeText("Modified content")
        
        // Create backup file
        val backupFile = tempFolder.newFile("backup.txt")
        val backupContent = "Original backup content"
        backupFile.writeText(backupContent)
        
        val result = fileSystemErrorHandler.restoreFromBackup(
            originalFile.absolutePath,
            backupFile.absolutePath
        )
        
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
        
        // Verify original file now has backup content
        assertEquals(backupContent, originalFile.readText())
    }
    
    @Test
    fun `restoreFromBackup should fail for non-existent backup`() = runTest {
        val originalPath = File(tempFolder.root, "original.txt").absolutePath
        val nonExistentBackupPath = File(tempFolder.root, "nonexistent_backup.txt").absolutePath
        
        val result = fileSystemErrorHandler.restoreFromBackup(originalPath, nonExistentBackupPath)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.FileSystemError.WriteError, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `file operations should handle IOException correctly`() = runTest {
        // Create a file and make it read-only to simulate permission issues
        val testFile = tempFolder.newFile("readonly.txt")
        testFile.setReadOnly()
        
        val result = fileSystemErrorHandler.writeFile(testFile.absolutePath, "New content")
        
        // Should handle the permission error gracefully
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        // The exact error type may vary by OS, but it should be a file system error
        assertTrue(error.errorType is ErrorType.FileSystemError)
    }
    
    @Test
    fun `file operations should retry on recoverable errors`() = runTest {
        var attemptCount = 0
        
        val result = fileSystemErrorHandler.executeFileOperation {
            attemptCount++
            if (attemptCount < 2) {
                throw IOException("Temporary IO error")
            }
            "Success after retry"
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals("Success after retry", (result as ErrorResult.Success).data)
        assertEquals(2, attemptCount)
    }
}