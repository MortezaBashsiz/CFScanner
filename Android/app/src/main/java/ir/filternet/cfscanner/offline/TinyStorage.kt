package ir.filternet.cfscanner.offline

import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gson
import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.google.gson.Gson
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.ScanSettings
import java.util.*

class TinyStorage(context: Context, gson: Gson) : KotprefModel(context) {

    init {
        Kotpref.gson = gson
    }

    // save last CIDR update time by timestamp format
    var updateCIDR by longPref()

    var scanSettings by gsonNullablePref(default = ScanSettings())

    // save last connection by using scan id and connection id
    private var lastConnection by gsonNullablePref<MutableMap<Int, Connection>>(key = "lastconn")

    fun addLastConnection(connection: Connection) {
        val lastConnection = lastConnection ?: mutableMapOf()
        lastConnection[connection.scan!!.uid] = connection
        this.lastConnection = lastConnection
    }

    fun getLastConnection(scanId: Int): Connection? {
        val lastConnection = lastConnection ?: mutableMapOf()
        return lastConnection[scanId]
    }
}