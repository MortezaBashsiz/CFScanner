package ir.filternet.cfscanner.contracts


import ir.filternet.cfscanner.db.CFSDatabase
import ir.filternet.cfscanner.offline.TinyStorage
import okhttp3.OkHttpClient
import javax.inject.Inject

open class BasicRepository @Inject constructor()  {

    @Inject
    protected lateinit var db : CFSDatabase

    @Inject
    protected lateinit var okHttp : OkHttpClient

    @Inject
    protected lateinit var tinyStorage: TinyStorage

}