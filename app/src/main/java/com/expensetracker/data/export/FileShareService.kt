package com.expensetracker.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.expensetracker.domain.model.ShareOption
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Service for sharing exported files through various channels
 */
class FileShareService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun shareFile(file: File, shareOption: ShareOption): Boolean {
        return try {
            when (shareOption) {
                ShareOption.EMAIL -> shareViaEmail(file)
                ShareOption.CLOUD_STORAGE -> shareViaCloudStorage(file)
                ShareOption.LOCAL_SAVE -> true // File is already saved locally
                ShareOption.SHARE_INTENT -> shareViaIntent(file)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun shareViaEmail(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Export")
                putExtra(Intent.EXTRA_TEXT, "Please find attached your expense tracker export.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(emailIntent, "Send via Email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun shareViaCloudStorage(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Filter for cloud storage apps
            val cloudApps = context.packageManager.queryIntentActivities(shareIntent, 0)
                .filter { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    packageName.contains("drive") || 
                    packageName.contains("dropbox") || 
                    packageName.contains("onedrive") ||
                    packageName.contains("cloud")
                }
            
            if (cloudApps.isNotEmpty()) {
                val chooser = Intent.createChooser(shareIntent, "Save to Cloud Storage")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                true
            } else {
                // Fallback to general share if no cloud apps found
                shareViaIntent(file)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun shareViaIntent(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Export")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            else -> "application/octet-stream"
        }
    }
}