package com.expensetracker.data.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets up default categories and keyword mappings
 */
@Singleton
class DefaultCategorySetup @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val categorizationRepository: CategorizationRepository
) {
    
    suspend fun initializeDefaultCategories() {
        categoryRepository.initializeDefaultCategories()
        categorizationRepository.initializeDefaultKeywords()
    }
    
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(1, "Food & Dining", "restaurant", "#FF9800", true),
            Category(2, "Shopping", "shopping_cart", "#2196F3", true),
            Category(3, "Transportation", "directions_car", "#4CAF50", true),
            Category(4, "Bills & Utilities", "receipt", "#F44336", true),
            Category(5, "Entertainment", "movie", "#9C27B0", true),
            Category(6, "Healthcare", "local_hospital", "#E91E63", true),
            Category(7, "Investment", "trending_up", "#009688", true),
            Category(8, "Income", "attach_money", "#8BC34A", true),
            Category(9, "Transfer", "swap_horiz", "#607D8B", true),
            Category(10, "Uncategorized", "help_outline", "#9E9E9E", true)
        )
        
        val DEFAULT_KEYWORD_MAPPINGS = mapOf(
            // Food & Dining
            "restaurant" to 1L,
            "cafe" to 1L,
            "coffee" to 1L,
            "pizza" to 1L,
            "burger" to 1L,
            "food" to 1L,
            "dining" to 1L,
            "kitchen" to 1L,
            "bakery" to 1L,
            "swiggy" to 1L,
            "zomato" to 1L,
            "dominos" to 1L,
            "mcdonalds" to 1L,
            "kfc" to 1L,
            "subway" to 1L,
            
            // Shopping
            "amazon" to 2L,
            "flipkart" to 2L,
            "myntra" to 2L,
            "shopping" to 2L,
            "mall" to 2L,
            "store" to 2L,
            "market" to 2L,
            "retail" to 2L,
            "supermarket" to 2L,
            "grocery" to 2L,
            "walmart" to 2L,
            "target" to 2L,
            "costco" to 2L,
            
            // Transportation
            "uber" to 3L,
            "ola" to 3L,
            "taxi" to 3L,
            "bus" to 3L,
            "metro" to 3L,
            "train" to 3L,
            "flight" to 3L,
            "airline" to 3L,
            "fuel" to 3L,
            "petrol" to 3L,
            "gas" to 3L,
            "parking" to 3L,
            "toll" to 3L,
            "transport" to 3L,
            
            // Bills & Utilities
            "electricity" to 4L,
            "water" to 4L,
            "gas" to 4L,
            "internet" to 4L,
            "phone" to 4L,
            "mobile" to 4L,
            "broadband" to 4L,
            "cable" to 4L,
            "insurance" to 4L,
            "rent" to 4L,
            "mortgage" to 4L,
            "loan" to 4L,
            "emi" to 4L,
            "bill" to 4L,
            "utility" to 4L,
            
            // Entertainment
            "movie" to 5L,
            "cinema" to 5L,
            "theater" to 5L,
            "netflix" to 5L,
            "spotify" to 5L,
            "youtube" to 5L,
            "gaming" to 5L,
            "game" to 5L,
            "entertainment" to 5L,
            "music" to 5L,
            "concert" to 5L,
            "event" to 5L,
            
            // Healthcare
            "hospital" to 6L,
            "doctor" to 6L,
            "medical" to 6L,
            "pharmacy" to 6L,
            "medicine" to 6L,
            "health" to 6L,
            "clinic" to 6L,
            "dental" to 6L,
            "lab" to 6L,
            "test" to 6L,
            
            // Investment
            "mutual" to 7L,
            "fund" to 7L,
            "stock" to 7L,
            "share" to 7L,
            "investment" to 7L,
            "trading" to 7L,
            "sip" to 7L,
            "fd" to 7L,
            "deposit" to 7L,
            "zerodha" to 7L,
            "groww" to 7L,
            
            // Income
            "salary" to 8L,
            "income" to 8L,
            "bonus" to 8L,
            "refund" to 8L,
            "cashback" to 8L,
            "reward" to 8L,
            "interest" to 8L,
            "dividend" to 8L,
            
            // Transfer
            "transfer" to 9L,
            "upi" to 9L,
            "paytm" to 9L,
            "gpay" to 9L,
            "phonepe" to 9L,
            "neft" to 9L,
            "rtgs" to 9L,
            "imps" to 9L
        )
    }
}