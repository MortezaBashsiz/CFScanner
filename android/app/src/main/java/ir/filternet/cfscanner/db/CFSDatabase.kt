package ir.filternet.cfscanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.filternet.cfscanner.db.convertor.DateConvertor
import ir.filternet.cfscanner.db.convertor.ProgressConvertor
import ir.filternet.cfscanner.db.convertor.ScanStatusConvertor
import ir.filternet.cfscanner.db.dao.*
import ir.filternet.cfscanner.db.entity.*


@Database(
    entities = [
        CidrEntity::class,
        ConfigEntity::class,
        ISPEntity::class,
        ConnectionEntity::class,
        ScanEntity::class,
    ], version = 1
)
@TypeConverters(DateConvertor::class, ScanStatusConvertor::class, ProgressConvertor::class)
abstract class CFSDatabase : RoomDatabase() {

    companion object {
        private var instance: CFSDatabase? = null

        fun getInstance(context: Context): CFSDatabase {
            if (instance != null) return instance!!
            instance = Room.databaseBuilder(context, CFSDatabase::class.java, "cfscanner.db").build()
            return instance!!
        }
    }

    abstract fun CIDRDao(): CIDRDao

    abstract fun configDao(): ConfigDao

    abstract fun ispDao(): ISPDao

    abstract fun connectionDao(): ConnectionDao

    abstract fun scanDao(): ScanDao

}