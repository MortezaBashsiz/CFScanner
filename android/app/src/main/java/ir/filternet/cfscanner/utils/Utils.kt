package ir.filternet.cfscanner.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import kotlin.math.pow


/**
 * create qrcode using zxing
 */
fun createQRCode(text: String, size: Int = 800): Bitmap? {
    try {
        val hints = HashMap<EncodeHintType, String>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        val bitMatrix = QRCodeWriter().encode(text,
            BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (bitMatrix.get(x, y)) {
                    pixels[y * size + x] = 0xff000000.toInt()
                } else {
                    pixels[y * size + x] = 0xffffffff.toInt()
                }

            }
        }
        val bitmap = Bitmap.createBitmap(size, size,
            Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        return null
    }
}



/**
 * get remote dns servers from preference
 */
fun getDomesticDnsServers(domesticDns:String): List<String> {
    val ret = domesticDns.split(",").filter { isPureIpAddress(it) || isCoreDNSAddress(it) }
    if (ret.isEmpty()) {
        return listOf(AppConfig.DNS_DIRECT)
    }
    return ret
}

/**
 * get remote dns servers from preference
 */
fun getRemoteDnsServers(remoteDns:String = AppConfig.DNS_AGENT): List<String> {
    val ret = remoteDns.split(",").filter { isPureIpAddress(it) || isCoreDNSAddress(it) }
    if (ret.isEmpty()) {
        return listOf(AppConfig.DNS_AGENT)
    }
    return ret
}

// try PREF_VPN_DNS then PREF_REMOTE_DNS
fun getVpnDnsServers(vpnDns:String=AppConfig.DNS_AGENT): List<String> {
    return vpnDns.split(",").filter { isPureIpAddress(it) }
    // allow empty, in that case dns will use system default
}

private fun isCoreDNSAddress(s: String): Boolean {
    return s.startsWith("https") || s.startsWith("tcp") || s.startsWith("quic")
}


fun isPureIpAddress(value: String): Boolean {
    return (isIpv4Address(value) || isIpv6Address(value))
}

/**
 * get text from clipboard
 */
fun getClipboard(context: Context): String {
    return try {
        val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cmb.primaryClip?.getItemAt(0)?.text.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

/**
 * is ip address
 */
fun isIpAddress(value: String): Boolean {
    try {
        var addr = value
        if (addr.isEmpty() || addr.isBlank()) {
            return false
        }
        //CidrEntity
        if (addr.indexOf("/") > 0) {
            val arr = addr.split("/")
            if (arr.count() == 2 && Integer.parseInt(arr[1]) > 0) {
                addr = arr[0]
            }
        }

        // "::ffff:192.168.173.22"
        // "[::ffff:192.168.173.22]:80"
        if (addr.startsWith("::ffff:") && '.' in addr) {
            addr = addr.drop(7)
        } else if (addr.startsWith("[::ffff:") && '.' in addr) {
            addr = addr.drop(8).replace("]", "")
        }

        // addr = addr.toLowerCase()
        val octets = addr.split('.').toTypedArray()
        if (octets.size == 4) {
            if (octets[3].indexOf(":") > 0) {
                addr = addr.substring(0, addr.indexOf(":"))
            }
            return isIpv4Address(addr)
        }

        // Ipv6addr [2001:abc::123]:8080
        return isIpv6Address(addr)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * set text to clipboard
 */
fun setClipboard(context: Context, content: String) {
    try {
        val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(null, content)
        cmb.setPrimaryClip(clipData)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readTextFromAssets(context: Context, fileName: String): String {
    val content = context.assets.open(fileName).bufferedReader().use {
        it.readText()
    }
    return content
}

fun userAssetPath(context: Context?): String {
    if (context == null)
        return ""
    val extDir = context.getExternalFilesDir("assets") ?: return context.getDir("assets", 0).absolutePath
    return extDir.absolutePath
}

/**
 * parseInt
 */
fun parseInt(str: String): Int {
    return parseInt(str, 0)
}

fun parseInt(str: String?, default: Int): Int {
    str ?: return default
    return try {
        Integer.parseInt(str)
    } catch (e: Exception) {
        e.printStackTrace()
        default
    }
}


/**
 * base64 decode
 */
fun decode(text: String): String {
    tryDecodeBase64(text)?.let { return it }
    if (text.endsWith('=')) {
        // try again for some loosely formatted base64
        tryDecodeBase64(text.trimEnd('='))?.let { return it }
    }
    return ""
}

fun tryDecodeBase64(text: String): String? {
    try {
        return Base64.decode(text, Base64.NO_WRAP).toString(charset("UTF-8"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    try {
        return Base64.decode(text, Base64.NO_WRAP.or(Base64.URL_SAFE)).toString(charset("UTF-8"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

/**
 * base64 encode
 */
fun encode(text: String): String {
    return try {
        Base64.encodeToString(text.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun urlDecode(url: String): String {
    return try {
        URLDecoder.decode(URLDecoder.decode(url), "utf-8")
    } catch (e: Exception) {
        e.printStackTrace()
        url
    }
}

fun urlEncode(url: String): String {
    return try {
        URLEncoder.encode(url, "UTF-8")
    } catch (e: Exception) {
        e.printStackTrace()
        url
    }
}

fun fixIllegalUrl(str: String): String {
    return str
        .replace(" ","%20")
        .replace("|","%7C")
}

fun getUuid(): String {
    return try {
        UUID.randomUUID().toString().replace("-", "")
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}


fun isIpv4Address(value: String): Boolean {
    val regV4 = Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
    return regV4.matches(value)
}

fun isIpv6Address(value: String): Boolean {
    var addr = value
    if (addr.indexOf("[") == 0 && addr.lastIndexOf("]") > 0) {
        addr = addr.drop(1)
        addr = addr.dropLast(addr.count() - addr.lastIndexOf("]"))
    }
    val regV6 = Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")
    return regV6.matches(addr)
}


fun calculateIpCountBySubnetMask(mask: Int): Int {
    return 2.0.pow((32 - mask) * 1.0).toInt()
}

fun ipAddressToLong(ipAddress: String): Long {
    val octets = ipAddress.split(".").map { it.toLong() }
    return (octets[0] shl 24) or (octets[1] shl 16) or (octets[2] shl 8) or octets[3]
}

fun longToIpAddress(ipAddress: Long): String {
    return "${(ipAddress shr 24) and 0xFF}.${(ipAddress shr 16) and 0xFF}.${(ipAddress shr 8) and 0xFF}.${ipAddress and 0xFF}"
}

fun downloadTestLinks(size:Float):String{
    return when{
        size >= 2560f -> "samples/audio/hcom/sample4.hcom"
        size >= 2048f -> "samples/image/xpm/sample_1280%C3%97853.xpm"
        size >= 1524f -> "samples/image/exr/sample_640%C3%97426.exr"
        size >= 1024f -> "samples/image/pgm/sample_1280%C3%97853.pgm"
        size >= 500f -> "samples/image/dds/sample_1280%C3%97853.dds"
        size >= 300f -> "samples/image/wbmp/sample_1920%C3%971280.wbmp"
        size >= 100f -> "samples/document/rtf/sample1.rtf"
        else -> ""
    }
}

// function to calculate usable host ip count for mask
// for example: mask=24 return 254
fun calculateUsableHostCountBySubnetMask(mask: Int): Int {
    return 2.0.pow((32 - mask) * 1.0).toInt() - 2
}



/** get ip address and subnet mask and Index then return deserve ip address
 * for example: address="185.146.172.0" mask=23 index=0 return "185.146.172.1"
 * for example: address="185.146.172.0" mask=23 index=509 return "185.146.173.254"
 **/
fun getIpAddressByIndex(address: String, mask: Int, index: Int): String? {
    val usableHostCount = calculateUsableHostCountBySubnetMask(mask)
    if (index > usableHostCount) {
        return null
    }
    val ipLong = ipAddressToLong(address)
    val ipLongWithMask = ipLong and (0xFFFFFFFF shl (32 - mask))
    val ipLongWithMaskAndIndex = ipLongWithMask + index + 1
    return longToIpAddress(ipLongWithMaskAndIndex)
}


/**
 * get ip address beside network address and mask then return index of ip address
 * example: ip="185.201.139.10" address="185.201.139.0" mask=24 return 9
 * example: ip="185.146.173.254" address="185.146.172.0" mask=23 return 509
 */
fun getIndexByIpAddress(ip: String, address: String, mask: Int): Int {
    val ipLong = ipAddressToLong(ip)
    val ipLongWithMask = ipAddressToLong(address) and (0xFFFFFFFF shl (32 - mask))
    return (ipLong - ipLongWithMask - 1).toInt()
}

