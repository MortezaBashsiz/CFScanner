package ir.filternet.cfscanner.scanner

import android.content.Context
import android.os.Build
import android.util.Log
import go.Seq
import ir.filternet.cfscanner.CFScannerApplication
import ir.filternet.cfscanner.utils.userAssetPath
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy

class V2RayClient constructor(context: Context, val config: String) {

    private val v2rayPoint: V2RayPoint = Libv2ray.newV2RayPoint(V2RayCallback(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)

    private var scanDomain = "scan.sudoer.net"

    private var downloadFile = "data.100k"


    private class V2RayCallback : V2RayVPNServiceSupportsSet {
        override fun shutdown(): Long {
            println("V2RayCallback shutdown")
            return 0
        }

        override fun prepare(): Long {
            println("V2RayCallback prepare")
            return 0
        }

        override fun protect(l: Long): Boolean {
            println("V2RayCallback protect $l")
            return true
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            println("V2RayCallback onEmitStatus $l   $s")
            return 0
        }

        override fun setup(s: String): Long {
            println("V2RayCallback setup $s")
            return 0
        }
    }

    fun connect(host: String) {
        v2rayPoint.configureFileContent = config
        v2rayPoint.domainName = host
        try {
            v2rayPoint.runLoop(false)
        } catch (e: Exception) {
            println("Error system fucked up: " + e.message)
        }
    }

    fun disconnect() {
        try {
            v2rayPoint.stopLoop()
        } catch (e: Exception) {
            println("Error system fucked up: " + e.message)
        }
    }

    fun isRunning(): Boolean {
        return v2rayPoint.isRunning
    }

    fun measureDelay(): Long {
        return try {
            v2rayPoint.measureDelay()
        } catch (e: Exception) {
            -1
        }
    }


}