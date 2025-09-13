package com.expensetracker.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration strategies for the Expense Tracker app
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2 - adds foreign key constraint for transferAccountId and default categories
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // First, insert default "Uncategorized" category if it doesn't exist
            database.execSQL("""
                INSERT OR IGNORE INTO categories (id, name, icon, color, isDefault, parentCategoryId) 
                VALUES (1, 'Uncategorized', 'help_outline', '#9E9E9E', 1, NULL)
            """)
            
            // Create new transactions table with updated foreign key constraints
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `transactions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `amount` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `merchant` TEXT NOT NULL,
                    `description` TEXT,
                    `date` INTEGER NOT NULL,
                    `source` TEXT NOT NULL,
                    `transferAccountId` INTEGER,
                    `transferTransactionId` INTEGER,
                    `isRecurring` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE SET DEFAULT,
                    FOREIGN KEY(`transferAccountId`) REFERENCES `accounts`(`id`) ON DELETE SET NULL
                )
            """.trimIndent())
            
            // Copy data from old table to new table
            database.execSQL("""
                INSERT INTO transactions_new SELECT * FROM transactions
            """)
            
            // Drop old table
            database.execSQL("DROP TABLE transactions")
            
            // Rename new table
            database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            
            // Create indices for transactions
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transferAccountId` ON `transactions` (`transferAccountId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transferTransactionId` ON `transactions` (`transferTransactionId`)")
        }
    }
    
    /**
     * Migration from version 2 to 3 - adds additional indices for performance
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add additional indices for better query performance
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_merchant` ON `transactions` (`merchant`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_source` ON `transactions` (`source`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_bankName` ON `accounts` (`bankName`)")
        }
    }
    
    /**
     * Get all migrations for the database
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3
        )
    }
}