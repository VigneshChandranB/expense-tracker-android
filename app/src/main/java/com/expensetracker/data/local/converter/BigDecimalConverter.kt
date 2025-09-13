package com.expensetracker.data.local.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * Room type converter for BigDecimal
 */
class BigDecimalConverter {
    
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
}