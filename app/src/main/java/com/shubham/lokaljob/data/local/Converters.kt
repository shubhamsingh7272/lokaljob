package com.shubham.lokaljob.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shubham.lokaljob.data.model.PrimaryDetails

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPrimaryDetails(value: PrimaryDetails?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPrimaryDetails(value: String?): PrimaryDetails? {
        return value?.let {
            val type = object : TypeToken<PrimaryDetails>() {}.type
            gson.fromJson(it, type)
        }
    }
} 