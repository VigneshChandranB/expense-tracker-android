package com.expensetracker.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room type converter for Map<String, String>
 */
class MapConverter {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        return if (value == null) {
            emptyMap()
        } else {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        }
    }
}