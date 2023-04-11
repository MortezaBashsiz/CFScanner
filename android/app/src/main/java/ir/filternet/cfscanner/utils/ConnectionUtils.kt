package ir.filternet.cfscanner.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import ir.filternet.cfscanner.model.ISP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.InetAddress


object ConnectionUtils : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private val client = OkHttpClient.Builder().build()

    // check VPN connectionEntity
    fun ConnectivityManager.isVpnConnection(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getNetworkCapabilities(this.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        } else {
            val activeNetworkInfo = this.activeNetworkInfo
            activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_VPN
        }
    }

    // check this host use Cloudflare CDN for access providing
    suspend fun isCloudflareCDN(address: String): Boolean {
        return try {
            val client = client
            val formBody: RequestBody = FormBody.Builder()
                .add("ip", address)
                .build()
            val request = Request.Builder().url("https://iplookup.flagfox.net/").post(formBody).build()
            val response = client.newCall(request).execute()
            yield()
            val inputText = response.body?.charStream()?.readText() ?: ""
            val rawtext = inputText.substringAfter("ISP\t\t\t\t\t</td>").substringBefore("</td>").substringAfter("width=\"33%\">").trim().run {
                if (this.contains("<span class=\"smallfont\">")) {
                    return@run this.substringAfter("<span class=\"smallfont\">").substringBefore("</span>").trim()
                }
                return@run this
            }
            response.body?.close()
            response.close()
            rawtext.contains("CLOUDFLARE",true)
           } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // get current internet provider service
    suspend fun getCurrentIspName(): ISP? {
        return try {
            val client = client
            val request = Request.Builder().url("https://iplookup.flagfox.net/").build()
            val response = client.newCall(request).execute()
            yield()
            val inputText = response.body?.charStream()?.readText() ?: ""
            val rawtext = inputText.substringAfter("ISP\t\t\t\t\t</td>").substringBefore("</td>").substringAfter("width=\"33%\">").trim().run {
                if (this.contains("<span class=\"smallfont\">")) {
                    return@run this.substringAfter("<span class=\"smallfont\">").substringBefore("</span>").trim()
                }
                return@run this
            }
            yield()
            val name = rawtext
            val location = getCurrentLocation()
            response.body?.close()
            response.close()
            ISP(name, location?.first, location?.second)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentLocation(): Pair<String?, String?>? {
        return try {

            val client = client
            val request = Request.Builder().url("https://ipnumberia.com/").build()
            val response = client.newCall(request).execute()
            yield()
            val inputText = response.body?.charStream()?.readText() ?: ""
            val strter = "<section class=\"w_px_re_1\">"
            val ender = "</section>"
            val text = inputText.substringAfter(strter).substringBefore(ender)

            val regionText = text.substringAfter("استان").substringBefore("</tr>")
            val cityText = text.substringAfter("شهر").substringBefore("</tr>")
            val patter = Regex("<td>(.*?)<\\/td>")
            val region = patter.find(regionText)?.groupValues?.last() ?: "UnK"
            val city = patter.find(cityText)?.groupValues?.last()
            response.body?.close()
            response.close()
            return region to city
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // check internet access by using ConnectivityManager
    fun hasNetworkConnection(context: Context, timeout: Int): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }
    }

    // check internet access by getting address
    suspend fun canPingAddress(address: String = "https://clients3.google.com/generate_204", timeout: Int = 2500): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val client = client
                val request  = Request.Builder().url(address).build()
                val response = client.newCall(request).execute()
                response.body?.close()
                response.close()
                response.code == 204
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


}