package ir.filternet.cfscanner.db.convertor

import androidx.room.TypeConverter
import ir.filternet.cfscanner.model.ScanResultStatus
import ir.filternet.cfscanner.model.getScanResultStatus

class ScanStatusConvertor {

    @TypeConverter
    fun fromTimestamp(value: Int?): ScanResultStatus {
        return getScanResultStatus(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: ScanResultStatus?): Int? {
        return date?.value
    }
}