package com.expensetracker.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.dao.AccountDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.SmsPatternDao
import com.expensetracker.data.local.dao.CategoryRuleDao
import com.expensetracker.data.local.dao.MerchantInfoDao
import com.expensetracker.data.local.dao.KeywordMappingDao
import com.expensetracker.data.local.dao.NotificationDao
import com.expensetracker.data.local.dao.SettingsDao
import com.expensetracker.data.local.entities.TransactionEntity
import com.expensetracker.data.local.entities.AccountEntity
import com.expensetracker.data.local.entities.CategoryEntity
import com.expensetracker.data.local.entities.SmsPatternEntity
import com.expensetracker.data.local.entities.CategoryRuleEntity
import com.expensetracker.data.local.entities.MerchantInfoEntity
import com.expensetracker.data.local.entities.KeywordMappingEntity
import com.expensetracker.data.local.entities.AccountEntity
import com.expensetracker.data.local.entity.NotificationEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.data.local.entity.AccountNotificationSettingsEntity
import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.local.entity.DataManagementSettingsEntity
import com.expensetracker.data.local.entity.PrivacySettingsEntity
import com.expensetracker.data.local.entity.AccountNotificationPreferencesEntity
import com.expensetracker.data.local.converters.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database for the Expense Tracker app
 */
@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        SmsPatternEntity::class,
        CategoryRuleEntity::class,
        MerchantInfoEntity::class,
        KeywordMappingEntity::class,
        NotificationEntity::class,
        NotificationPreferencesEntity::class,
        AccountNotificationSettingsEntity::class,
        AppSettingsEntity::class,
        DataManagementSettingsEntity::class,
        PrivacySettingsEntity::class,
        AccountNotificationPreferencesEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun smsPatternDao(): SmsPatternDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun merchantInfoDao(): MerchantInfoDao
    abstract fun keywordMappingDao(): KeywordMappingDao
    abstract fun notificationDao(): NotificationDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
        
        /**
         * Database callback to populate default data
         */
        val databaseCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert default categories
                insertDefaultCategories(db)
                // Insert default SMS patterns
                insertDefaultSmsPatterns(db)
            }
        }
        
        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
            // Insert default "Uncategorized" category with ID 1 for SET_DEFAULT constraint
            db.execSQL("""
                INSERT INTO categories (id, name, icon, color, isDefault, parentCategoryId) 
                VALUES (1, 'Uncategorized', 'help_outline', '#9E9E9E', 1, NULL)
            """)
            
            // Insert other default categories
            val defaultCategories = listOf(
                "(2, 'Food & Dining', 'restaurant', '#FF9800', 1, NULL)",
                "(3, 'Shopping', 'shopping_cart', '#2196F3', 1, NULL)",
                "(4, 'Transportation', 'directions_car', '#4CAF50', 1, NULL)",
                "(5, 'Bills & Utilities', 'receipt', '#F44336', 1, NULL)",
                "(6, 'Entertainment', 'movie', '#9C27B0', 1, NULL)",
                "(7, 'Healthcare', 'local_hospital', '#E91E63', 1, NULL)",
                "(8, 'Investment', 'trending_up', '#00BCD4', 1, NULL)",
                "(9, 'Income', 'attach_money', '#8BC34A', 1, NULL)",
                "(10, 'Transfer', 'swap_horiz', '#607D8B', 1, NULL)"
            )
            
            defaultCategories.forEach { categoryValues ->
                db.execSQL("""
                    INSERT INTO categories (id, name, icon, color, isDefault, parentCategoryId) 
                    VALUES $categoryValues
                """)
            }
        }
        
        private fun insertDefaultSmsPatterns(db: SupportSQLiteDatabase) {
            val defaultPatterns = listOf(
                // HDFC Bank
                "('HDFC Bank', 'HDFCBK', 'Rs\\.([0-9,]+\\.?[0-9]*)', 'at ([A-Z0-9\\s]+)', 'on ([0-9]{2}-[0-9]{2}-[0-9]{4})', '(debited|credited)', 'A/c \\*([0-9]{4})', 1)",
                // ICICI Bank
                "('ICICI Bank', 'ICICI', 'INR ([0-9,]+\\.?[0-9]*)', 'to ([A-Z\\s]+)', '([0-9]{2}/[0-9]{2}/[0-9]{4})', '(Dr|Cr)', 'XX([0-9]{4})', 1)",
                // SBI
                "('State Bank of India', 'SBIINB', 'Rs ([0-9,]+\\.?[0-9]*)', 'at ([A-Z0-9\\s]+)', 'on ([0-9]{2}-[A-Z]{3}-[0-9]{2})', '(debited|credited)', 'XX([0-9]{4})', 1)",
                // Axis Bank
                "('Axis Bank', 'AXISBK', 'INR ([0-9,]+\\.?[0-9]*)', 'at ([A-Z0-9\\s]+)', 'on ([0-9]{2}-[0-9]{2}-[0-9]{4})', '(debited|credited)', 'XX([0-9]{4})', 1)"
            )
            
            defaultPatterns.forEach { patternValues ->
                db.execSQL("""
                    INSERT INTO sms_patterns (bankName, senderPattern, amountPattern, merchantPattern, datePattern, typePattern, accountPattern, isActive) 
                    VALUES $patternValues
                """)
            }
        }
    }
}