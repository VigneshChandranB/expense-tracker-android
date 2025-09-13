package com.expensetracker.data.export

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Transaction
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * CSV export implementation for transaction data
 */
class CsvExporter @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    suspend fun exportTransactions(
        transactions: List<Transaction>,
        accounts: Map<Long, Account>,
        outputFile: File
    ): Boolean {
        return try {
            FileWriter(outputFile).use { fileWriter ->
                CSVWriter(fileWriter).use { csvWriter ->
                    // Write header
                    csvWriter.writeNext(CSV_HEADERS)
                    
                    // Write transaction data
                    transactions.forEach { transaction ->
                        val account = accounts[transaction.accountId]
                        val transferAccount = transaction.transferAccountId?.let { accounts[it] }
                        
                        csvWriter.writeNext(arrayOf(
                            transaction.id.toString(),
                            transaction.date.format(dateFormatter),
                            transaction.amount.toString(),
                            transaction.type.name,
                            transaction.category.name,
                            transaction.merchant,
                            transaction.description ?: "",
                            transaction.source.name,
                            account?.bankName ?: "",
                            account?.nickname ?: "",
                            account?.accountType?.name ?: "",
                            transferAccount?.nickname ?: "",
                            transaction.isRecurring.toString()
                        ))
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        private val CSV_HEADERS = arrayOf(
            "ID",
            "Date",
            "Amount",
            "Type",
            "Category",
            "Merchant",
            "Description",
            "Source",
            "Bank Name",
            "Account",
            "Account Type",
            "Transfer Account",
            "Is Recurring"
        )
    }
}