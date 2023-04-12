package ir.filternet.cfscanner.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.filternet.cfscanner.offline.TinyStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @Provides
    @Singleton
    fun getGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun getStorage(@ApplicationContext context: Context, gson: Gson): TinyStorage = TinyStorage(context,gson)


}