package com.expensetracker.data.export

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import java.io.File
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * PDF export implementation for transaction data with charts and formatted tables
 */
class PdfExporter @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    suspend fun exportTransactions(
        transactions: List<Transaction>,
        accounts: Map<Long, Account>,
        dateRange: DateRange,
        includeCharts: Boolean,
        outputFile: File
    ): Boolean {
        return try {
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Add title
            document.add(
                Paragraph("Expense Tracker Report")
                    .setFontSize(20f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
            )
            
            // Add date range
            document.add(
                Paragraph("Period: ${dateRange.startDate.format(dateFormatter)} to ${dateRange.endDate.format(dateFormatter)}")
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            document.add(Paragraph("\n"))
            
            // Add summary section
            addSummarySection(document, transactions, accounts)
            
            // Add category breakdown if charts are included
            if (includeCharts) {
                addCategoryBreakdown(document, transactions)
            }
            
            // Add transactions table
            addTransactionsTable(document, transactions, accounts)
            
            document.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun addSummarySection(
        document: Document,
        transactions: List<Transaction>,
        accounts: Map<Long, Account>
    ) {
        document.add(
            Paragraph("Summary")
                .setFontSize(16f)
                .setBold()
        )
        
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val netAmount = totalIncome - totalExpenses
        
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        summaryTable.addCell("Total Income:")
        summaryTable.addCell("₹${totalIncome}")
        summaryTable.addCell("Total Expenses:")
        summaryTable.addCell("₹${totalExpenses}")
        summaryTable.addCell("Net Amount:")
        summaryTable.addCell("₹${netAmount}")
        summaryTable.addCell("Total Transactions:")
        summaryTable.addCell(transactions.size.toString())
        
        document.add(summaryTable)
        document.add(Paragraph("\n"))
    }
    
    private fun addCategoryBreakdown(document: Document, transactions: List<Transaction>) {
        document.add(
            Paragraph("Category Breakdown")
                .setFontSize(16f)
                .setBold()
        )
        
        val categoryTotals = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        categoryTable.addHeaderCell("Category")
        categoryTable.addHeaderCell("Amount")
        
        categoryTotals.forEach { (category, amount) ->
            categoryTable.addCell(category.name)
            categoryTable.addCell("₹${amount}")
        }
        
        document.add(categoryTable)
        document.add(Paragraph("\n"))
    }
    
    private fun addTransactionsTable(
        document: Document,
        transactions: List<Transaction>,
        accounts: Map<Long, Account>
    ) {
        document.add(
            Paragraph("Transaction Details")
                .setFontSize(16f)
                .setBold()
        )
        
        val transactionTable = Table(UnitValue.createPercentArray(
            floatArrayOf(15f, 15f, 15f, 20f, 20f, 15f)
        )).setWidth(UnitValue.createPercentValue(100f))
        
        // Add headers
        transactionTable.addHeaderCell("Date")
        transactionTable.addHeaderCell("Amount")
        transactionTable.addHeaderCell("Type")
        transactionTable.addHeaderCell("Category")
        transactionTable.addHeaderCell("Merchant")
        transactionTable.addHeaderCell("Account")
        
        // Add transaction rows
        transactions.sortedByDescending { it.date }.forEach { transaction ->
            val account = accounts[transaction.accountId]
            
            transactionTable.addCell(transaction.date.format(dateTimeFormatter))
            transactionTable.addCell("₹${transaction.amount}")
            transactionTable.addCell(transaction.type.name)
            transactionTable.addCell(transaction.category.name)
            transactionTable.addCell(transaction.merchant)
            transactionTable.addCell(account?.nickname ?: "Unknown")
        }
        
        document.add(transactionTable)
    }
}