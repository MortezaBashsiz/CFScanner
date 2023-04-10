package ir.filternet.cfscanner.scanner

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.model.ScanSettings
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ScanRepository
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil
import ir.filternet.cfscanner.utils.downloadTestLinks
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject


class CFSpeed @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanRepository: ScanRepository,
    private val tinyStorage: TinyStorage,
    private val rawClient:OkHttpClient,
) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    private var scanDomain = "filesamples.com/"
    private val v2rarUtils = V2rayConfigUtil(context)
    private var listener: CFSpeedListener? = null
    private var job: Job? = null

    fun startCheck(connections: List<Connection>) {
        job?.cancel()
        job = launch(Dispatchers.IO) {
            startCheckProcess(connections)
        }

    }

    fun setListener(listener: CFSpeedListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    fun stopCheck() {
        listener?.onStopChecking()
        job?.cancel()
        job = null
    }

    private suspend fun startCheckProcess(connections: List<Connection>) {
        val port = 443
        val downloadSize = downloadTestLinks((tinyStorage.scanSettings ?: ScanSettings()).speedTestSize)
        val config = connections.firstOrNull()?.scan?.config?.config ?: throw IllegalStateException("No config found!")
        val count = connections.size
        var checked = 0
        var duration = 0L
        listener?.onStartChecking(connections.size)
        connections.forEachIndexed { index, connection ->
            val estimatedTimeInSecond = (duration/(checked.coerceAtLeast(1))) * (count-(index))
            listener?.onCheckProcess(connection.copy(delay = 0, speed = 0, updating = true), index / (count * 1f),count-(index),estimatedTimeInSecond)
            yield()
            val startTime = System.currentTimeMillis()
            val (delay, speed) = startConnection(config, connection.ip, port, downloadSize)
            val diff = (System.currentTimeMillis() - startTime)/1000
            yield()
            listener?.onCheckProcess(connection.copy(delay = delay, speed = speed, updating = false), (index + 1) / (count * 1f),count-(index+1),estimatedTimeInSecond)
            duration += diff
            checked++
        }
        yield()
        listener?.onFinishChecking(connections.size)
        listener?.onStopChecking()
    }


    private suspend fun startConnection(config: String, ip: String, port: Int, file: String): Pair<Long, Long> {
        val ports = "4${getGeneratedPort(ip)}".toInt()
        val conf = v2rarUtils.createServerConfig(config)?.fullConfig?.getByCustomVnextOutbound(port, ip)?.getByCustomInbound(ports)?.toPrettyPrinting()!!
        val client = V2RayClient(context, conf)
        client.connect("$ip:$port")
        yield()
        delay(2000)
        yield()
        val delay = client.measureDelay()
        yield()
        val speed = downloadTest(ports, file)
        if (client.isRunning()) {
            client.disconnect()
        }
        return delay to speed
    }

    private fun getGeneratedPort(ip: String): Int {
        val ipParts = ip.split('.')
        val ipO1 = ipParts[0].toInt()
        val ipO2 = ipParts[1].toInt()
        val ipO3 = ipParts[2].toInt()
        val ipO4 = ipParts[3].toInt()
        val port = ipO1 + ipO2 + ipO3 + ipO4
        return port
    }

    private suspend fun downloadTest(port: Int, file: String): Long {
        try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))

            val client = rawClient.newBuilder()
                .proxy(proxy)
                .build()

            val request = Request.Builder()
                .url("https://$scanDomain/$file")
                .get()
                .build()

            yield()
            val start_time = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream()
            yield()
            val size = inputStream?.readBytes()?.size
            inputStream?.close()
            val end_time = System.currentTimeMillis()
            val diff_time = end_time - start_time
            yield()
            val speed = ((size?.toLong()?:0L) / diff_time)* 1000 / 1024

            response.body?.close()
            response.close()

            return speed
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    interface CFSpeedListener {
        fun onStartChecking(count:Int)
        fun onCheckProcess(connection: Connection, progress: Float , remainItem:Int , estimatedTime:Long)
        fun onFinishChecking(count:Int)
        fun onStopChecking()
    }
}