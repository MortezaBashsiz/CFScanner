package ir.filternet.cfscanner.scanner

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.BuildConfig
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.*
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.CIDRRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil
import ir.filternet.cfscanner.utils.calculateUsableHostCountBySubnetMask
import ir.filternet.cfscanner.utils.getIndexByIpAddress
import ir.filternet.cfscanner.utils.getIpAddressByIndex
import kotlinx.coroutines.*
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CFScanner @Inject constructor(
    @ApplicationContext val context: Context,
    val tinyStorage: TinyStorage,
    val cidrRepository: CIDRRepository,
    val connectionRepository: ConnectionRepository,
    val logger: CFSLogger,
    val rawClient: OkHttpClient,
    val v2rarUtils: V2rayConfigUtil,
) : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    private var frontDomain = BuildConfig.FrontingAddress
    private var scanDomain = "filesamples.com/"
    private var downloadFile = "samples/image/wbmp/sample_1920%C3%971280.wbmp"

    private var discoveryJob: Job? = null
    private var listener: CFScannerListener? = null

    private val cidrs: ArrayList<CIDR> = arrayListOf()
    private var scan: Scan? = null

    private var skipCurrentRange: Boolean = false

    fun startScan(scan: Scan, options: ScanOptions = ScanOptions()) {
        discoveryJob?.cancel()
        discoveryJob = launch(Dispatchers.IO) {

            listener?.onScanStart(scan, options)

            logger.add(Log("process", context.getString(R.string.start_scan_process)))

            this@CFScanner.scan = scan

            Timber.d("CFScanner: Start ...")
            startScan(scan, options)
        }
    }

    fun stopScan(byUser: Boolean = true, reason: String = "") {
        logger.add(Log("process", context.getString(R.string.scan_stoping), STATUS.INPROGRESS))
        Timber.d("CFScanner: Stopped!")
        discoveryJob?.cancel()
        listener?.onScanPaused(scan!!, reason, byUser)
        if (byUser) {
            logger.add(Log("process", context.getString(R.string.scan_stopped_successfully), STATUS.SUCCESS))
        } else {
            logger.add(Log("process", context.getString(R.string.scan_stopped), STATUS.FAILED))
        }
    }


    fun setListener(listener: CFScannerListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    fun isRunning(): Boolean {
        return discoveryJob?.isActive ?: false
    }


    fun skipCurrentRange() {
        if (!skipCurrentRange) {
            skipCurrentRange = true
        }
    }

    private suspend fun CoroutineScope.startScan(scan: Scan, options: ScanOptions) {
        val scope = this
        val config = scan.config
        val isp = scan.isp


        val allCIDR = getScanCidrs(scan, options)
        this@CFScanner.scan = scan.copy(scanCidrOrder = allCIDR.joinToString(" ") { it.uid.toString() })

        delay(1000)

        /* set options */
        val parallel = options.parallel
        frontDomain = options.frontingDomain
        val autoSkip = options.autoSkipPortion

        /* load previous scan history*/
        val (lastCidr, lastConnection) = getLastDetails(scan)
        var scanned = scan.progress.checkConnectionCount
        var founded = scan.progress.successConnectionCount
        val indexOfLastConnectionCIDR = allCIDR.indexOfFirst { it.uid == (lastCidr as? CIDR?)?.uid }
        /* ================= */

        /* Start Process Log */
        logger.add(Log("worker", context.getString(R.string.worker_initialized, parallel), STATUS.SUCCESS))
        Timber.d("CFScanner: Start Scan by $parallel worker")
        /* ================= */

        if (allCIDR.isEmpty()) {
            logger.add(Log("cidr", context.getString(R.string.can_not_get_cidr), STATUS.FAILED))
            logger.add(Log("cidr1", context.getString(R.string.operation_will_end_in_5_sec), STATUS.FAILED))
            delay(5000)
            this@CFScanner.stopScan(true, context.getString(R.string.can_not_get_cidr))
            yield()
        }
        val ipCount = allCIDR.map { calculateUsableHostCountBySubnetMask(it.subnetMask) }.reduce { a, i -> a + i }
        logger.add(Log("ips", context.getString(R.string.ip_found_to_check, ipCount), STATUS.SUCCESS))

        delay(1000)

        runBlocking {
            // worker controller
            val semaphore = Semaphore(parallel)
            var skipped = 0
            allCIDR.let {

                // delete scanned cidr
                if (indexOfLastConnectionCIDR >= 0)
                    it.drop(indexOfLastConnectionCIDR)
                else
                    it

            }.forEachIndexed { cidrIndex, cidr ->

                val count = calculateUsableHostCountBySubnetMask(cidr.subnetMask)

                // calculate index of last connection in this cidr
                val skipIndex = lastCidr?.let { it as? CIDR? }?.let { cidrI ->
                    return@let if (cidrI.uid == cidr.uid) {
                        lastConnection?.let { it as? Connection? }?.let { conn ->
                            getIndexByIpAddress(conn.ip, cidrI.address, cidrI.subnetMask)
                        } ?: -1
                    } else -1
                } ?: -1

                /* Start Scan Log */
                logger.add(Log(cidr.address, context.getString(R.string.start_scan_for, cidr.address, cidr.subnetMask), STATUS.INPROGRESS))
                Timber.d("CFScanner: Start Scan for ${cidr.address}/${cidr.subnetMask} by $count ips")
                /* ============== */

                var failed = 0
                skipped = 0
                repeat(count) { index ->

                    val ip = getIpAddressByIndex(cidr.address, cidr.subnetMask, index)

                    // skip connection from this range
                    if (skipIndex >= index) {
                        return@repeat
                    }

                    // auto skip
                    if (autoSkip >= 1 && !skipCurrentRange) {
                        val shouldSkip = (((failed*1f) / count)*100f) >= autoSkip
                        if (shouldSkip){
                            logger.add(Log(ip+ Random(100).toString(), context.getString(R.string.auto_skip_triggered_for,cidr.address), STATUS.SUCCESS))
                            skipCurrentRange()
                        }
                    }

                    // skip range check
                    if (skipCurrentRange) {
                        skipped++
                        return@repeat
                    }


                    semaphore.acquire()
                    scope.launch {
                        ip?.let {
                            try {
                                scanned++
                                logger.add(Log(it, it, STATUS.INPROGRESS))
                                Timber.d("CFScanner: check status for $it")
                                yield()
                                var delay = checkIpDelay(config, it)
                                yield()
                                if (delay > 0) {

                                    // apply ping limit filter on result
                                    if (delay > options.pingFilter) {
                                        logger.add(Log(it, context.getString(R.string.ping_error, it), STATUS.FAILED))
                                        failed++
                                        delay = -1
                                    } else {
                                        // reset failed count to avoid auto skip
                                        failed = 0

                                        founded++
                                        logger.add(Log(it, "$it ($delay ms)", STATUS.SUCCESS))
                                        Timber.d("CFScanner: result status for $it ==> Success (${delay}ms)")
                                    }

                                } else {
                                    failed++
                                    Timber.d("CFScanner: result status for $it ==> Failed!")
                                }

                                val progress = ScanProgress(ipCount, scanned, founded)
                                val updatedScan = scan.copy(progress = progress)
                                this@CFScanner.scan = updatedScan
                                val connection = Connection(it, updatedScan, cidr, delay = delay)
                                listener?.onConnectionUpdate(updatedScan, connection)

                            } catch (e: Exception) {
                                logger.add(Log(it, "$it (${e.javaClass.simpleName})", STATUS.FAILED))
                                Timber.e(e)
                            }

                        }
                        semaphore.release()
                    }
                }

                skipCurrentRange = false

                if (skipped > 0) {
                    logger.add(Log(cidr.address + Random(100).toString(), context.getString(R.string.skipped_range, skipped, cidr.address), STATUS.SUCCESS))
                } else {
                    logger.add(Log(cidr.address, context.getString(R.string.end_scan_for, cidr.address, cidr.subnetMask), STATUS.SUCCESS))
                }
            }

            semaphore.tryAcquire(parallel, 10, TimeUnit.SECONDS)
        }

        if (this.isActive) {
            logger.add(Log("process", context.getString(R.string.scan_finished_successfully), STATUS.SUCCESS))
            listener?.onScanFinished(scan.copy(progress = ScanProgress(ipCount, scanned, founded)))
        }
    }


    private suspend fun getScanCidrs(scan: Scan, options: ScanOptions): List<CIDR> {
        // 1. get scan cidrs order
        val cidrIdList = scan.scanCidrOrder.ifBlank { null }?.split(" ")?.map { it.trim().toInt() }

        // 2. convert cidrs order to cidr list
        val cidrList = cidrRepository.getAllCIDR(options.autoFetch)
            // filter custom range
            .let { if (options.customRange) it.filter { it.custom } else it }

        return if (!cidrIdList.isNullOrEmpty()) {
            cidrList.sortedBy { cidrIdList.indexOf(it.uid) }
        } else {
            cidrList.let { if (options.shuffle) it.shuffled() else it.sortedBy { it.position } }
        }
    }

    private suspend fun getLastDetails(scan: Scan): Array<*> {
        val connection = tinyStorage.getLastConnection(scan.uid)
        val cidr = cidrRepository.getById(connection?.cidr?.uid ?: 0)
        return arrayOf(cidr, connection)
    }


    private suspend fun checkIpDelay(config: Config, address: String, port: Int = 443): Long {
        // 1. check port opening
        if (!isPortOpen(address, port, 2000)) {
            logger.add(Log(address, context.getString(R.string.port_close, address), STATUS.FAILED))
            Timber.d("CFScanner: check status for $address ==> Port $port is not Open!")
            return -1
        }

        // 2. check domain fronting
        if (!checkDomainFronting(address)) {
            logger.add(Log(address, context.getString(R.string.fronting_error, address), STATUS.FAILED))
            Timber.d("CFScanner: check status for $address ==> Fronting not Ok!")
            return -1
        }

        // 3. start connectionEntity
        return startConnection(config.config, address, port)
    }


    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            return true
        } catch (e: IOException) {
            // Port is not open
        } finally {
            socket.close()
        }
        return false
    }

    private fun checkDomainFronting(ip: String): Boolean {
        val dnsResolver = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return listOf(InetAddress.getByName(ip))
            }
        }

        val client = rawClient.newBuilder()
            .dns(dnsResolver)
            .connectTimeout(1000, TimeUnit.MILLISECONDS)
            .readTimeout(1000, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .build()

        val request = Request.Builder()
            .url("https://$frontDomain")
            .addHeader("Host", frontDomain)
            .get()
            .build()

        return try {
            val result = client.newCall(request).execute()
            val statusCode = result.code
            result.body?.close()
            result.close()
            statusCode in 200..299
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            false
        }
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


    private suspend fun startConnection(config: String, ip: String, port: Int): Long {
        val ports = "3${getGeneratedPort(ip)}".toInt()
        val conf = v2rarUtils.createServerConfig(config)?.fullConfig?.getByCustomVnextOutbound(port, ip)?.getByCustomInbound(ports)?.toPrettyPrinting()!!
        val client = V2RayClient(context, conf)
        client.connect("$ip:$port")
        logger.add(Log(ip, context.getString(R.string.v2ray_cooldown, ip), STATUS.INPROGRESS))
        delay(2000)
        val delay = client.measureDelay()

        try {
            val speed = downloadTest(ports)

            if (client.isRunning()) {
                client.disconnect()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            logger.add(Log(ip, context.getString(R.string.v2ray_failed, ip), STATUS.FAILED))
        }


        if (delay < 0) {
            logger.add(Log(ip, context.getString(R.string.v2ray_failed, ip), STATUS.FAILED))
        }
        return delay
    }


    private fun downloadTest(port: Int): Long {
        try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
            val client = rawClient.newBuilder().proxy(proxy).build()
            val request = Request.Builder().url("https://$scanDomain/$downloadFile").build()

            val start_time = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream()
            val size = inputStream?.readBytes()?.size
            inputStream?.close()
            val end_time = System.currentTimeMillis()
            val diff_time = end_time - start_time

            val speed = ((size?.toLong() ?: 0L) / diff_time) * 1000 / 1024

            response.body?.close()
            response.close()

            return speed
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }


    interface CFScannerListener {

        fun onScanStart(scan: Scan, options: ScanOptions)

        fun onConnectionUpdate(scan: Scan, connection: Connection)

        fun onScanFinished(scan: Scan)

        fun onScanPaused(scan: Scan, reason: String, byUser: Boolean)
    }

    data class ScanOptions(
        val parallel: Int = 4,
        val frontingDomain: String = "",
        val pingFilter: Float = 2900f,
        val autoSkipPortion: Float = 0f,
        val autoFetch: Boolean = true,
        val shuffle: Boolean = false,
        val customRange: Boolean = false,
    )


}