package ir.filternet.cfscanner.db.convertor

import androidx.room.TypeConverter
import ir.filternet.cfscanner.model.ScanProgress
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProgressConvertor {

    @TypeConverter
    fun fromTimestamp(value: String): ScanProgress? {
        if(value.isEmpty()) return null
        return value.let{  Json.decodeFromString(value) }
    }

    @TypeConverter
    fun dateToTimestamp(date: ScanProgress?): String {
        return date?.let{  Json.encodeToString(it) }?: ""
    }
}