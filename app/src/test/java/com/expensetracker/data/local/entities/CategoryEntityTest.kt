package com.expensetracker.data.local.entities

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CategoryEntity data class
 */
class CategoryEntityTest {
    
    @Test
    fun `category entity creation with all fields`() {
        val category = CategoryEntity(
            id = 1L,
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true,
            parentCategoryId = null
        )
        
        assertEquals(1L, category.id)
        assertEquals("Food & Dining", category.name)
        assertEquals("restaurant", category.icon)
        assertEquals("#FF9800", category.color)
        assertTrue(category.isDefault)
        assertNull(category.parentCategoryId)
    }
    
    @Test
    fun `category entity with parent category`() {
        val subCategory = CategoryEntity(
            id = 2L,
            name = "Fast Food",
            icon = "fastfood",
            color = "#FF5722",
            isDefault = false,
            parentCategoryId = 1L
        )
        
        assertEquals("Fast Food", subCategory.name)
        assertFalse(subCategory.isDefault)
        assertEquals(1L, subCategory.parentCategoryId)
    }
    
    @Test
    fun `category entity with custom properties`() {
        val customCategory = CategoryEntity(
            id = 3L,
            name = "Pet Expenses",
            icon = "pets",
            color = "#4CAF50",
            isDefault = false,
            parentCategoryId = null
        )
        
        assertEquals("Pet Expenses", customCategory.name)
        assertEquals("pets", customCategory.icon)
        assertEquals("#4CAF50", customCategory.color)
        assertFalse(customCategory.isDefault)
    }
    
    @Test
    fun `category entity with default id`() {
        val category = CategoryEntity(
            name = "Transportation",
            icon = "directions_car",
            color = "#2196F3",
            isDefault = true,
            parentCategoryId = null
        )
        
        assertEquals(0L, category.id) // Default value
        assertEquals("Transportation", category.name)
    }
}