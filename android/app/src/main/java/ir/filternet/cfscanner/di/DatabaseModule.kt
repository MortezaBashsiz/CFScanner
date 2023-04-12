package ir.filternet.cfscanner.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.filternet.cfscanner.db.CFSDatabase
import ir.filternet.cfscanner.db.dao.CIDRDao
import ir.filternet.cfscanner.db.dao.ConfigDao
import ir.filternet.cfscanner.db.dao.ConnectionDao
import ir.filternet.cfscanner.db.dao.ISPDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {


    @Provides
    @Singleton
    fun getDatabase(@ApplicationContext context: Context): CFSDatabase = CFSDatabase.getInstance(context)

    @Provides
    @Singleton
    fun getCIDRDao(db: CFSDatabase): CIDRDao = db.CIDRDao()

    @Provides
    @Singleton
    fun getConfigDao(db: CFSDatabase): ConfigDao = db.configDao()

    @Provides
    @Singleton
    fun getIspDao(db: CFSDatabase): ISPDao = db.ispDao()

    @Provides
    @Singleton
    fun getConnectionDao(db: CFSDatabase): ConnectionDao = db.connectionDao()
}