package ir.filternet.cfscanner.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.db.entity.CidrEntity
import ir.filternet.cfscanner.mapper.mapToCidr
import ir.filternet.cfscanner.mapper.mapToCidrEntity
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.model.Log
import ir.filternet.cfscanner.model.STATUS
import ir.filternet.cfscanner.scanner.CFSLogger
import ir.filternet.cfscanner.utils.AppConfig
import ir.filternet.cfscanner.utils.calculateUsableHostCountBySubnetMask
import ir.filternet.cfscanner.utils.getFromGithub
import kotlinx.coroutines.delay
import okhttp3.Request
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CIDRRepository @Inject constructor(
    @ApplicationContext val context:Context,
    val logger: CFSLogger,
) : BasicRepository() {

    private val cloudflareASNs = arrayOf("AS13335", "AS209242")
    private val cloudflareOkOctave = arrayOf(
        "23", "31", "45", "66", "80", "89", "103", "104", "108", "141",
        "147", "154", "159", "168", "170", "173", "185", "188", "191",
        "192", "193", "194", "195", "199", "203", "205", "212"
    )

    private val DEPRECATION_TIME = 1000 * 60 * 60 * 5 // 5h
    private val cidrDao by lazy { db.CIDRDao() }

    suspend fun getAllCIDR(networkFetch:Boolean = true): List<CIDR> {
        if (isDeprecated() && networkFetch) {
            logger.add(Log("cidrs", context.getString(R.string.get_cidr_from_network), STATUS.INPROGRESS))
            getDataFromNetwork()
            logger.add(Log("cidrs", context.getString(R.string.cidr_fetched_from_netwoek), STATUS.SUCCESS))
        } else {
            logger.add(Log("cidrs", context.getString(R.string.cidr_fetch_from_cache), STATUS.SUCCESS))
        }
        return cidrDao.getAll().map { it.mapToCidr() }
    }

    suspend fun getById(uid: Int): CIDR? {
        return cidrDao.findById(uid)?.mapToCidr()
    }

    suspend fun getByAddress(address: String): CIDR? {
        return cidrDao.findByAddress(address)?.mapToCidr()
    }

    suspend fun updateCidrPositions(list: List<CIDR>) {
        list.map { it.mapToCidrEntity() }.apply {
            cidrDao.insertForce(this)
        }
    }

    suspend fun deleteCIdr(cidr: CIDR) {
        cidrDao.delete(cidr.mapToCidrEntity())
    }

    suspend fun insertCidr(cidr: CIDR) {
        cidrDao.insert(cidr.mapToCidrEntity())
    }


    private suspend fun getDataFromNetwork() {
        val list = arrayListOf<String>()
        try {
            val result = getCIDRSGithub() ?: throw Exception("result is null")
            saveAllCIDR(result)
            saveLastUpdate()
        } catch (e: Exception) {
            logger.add(Log("cidrs", context.getString(R.string.cidr_fetch_failed), STATUS.FAILED))
            delay(4000)
            getDataFromNetwork()
        }
        // TODO check address status before request

    }


    private fun getCIDRSGithub(): List<String>? {
        return try {
            getFromGithub(AppConfig.CIDR_Address).lines().toList() ?: emptyList()
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private fun getCidrsListfromCF(): List<String>? {
        return try {
            val request = Request.Builder()
                .url("https://www.cloudflare.com/ips-v4")
                .build()
            val response = okHttp.newCall(request).execute()
            val body = response.body?.string()

            body?.lines()?.toList() ?: emptyList()
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private suspend fun saveAllCIDR(list: List<String>) {
        var ipCount = 0
        var lastPosition = cidrDao.getAll().maxByOrNull { it.position }?.position ?: -1
        list.mapIndexed { index, it ->
            val splitted = it.split("/")
            val addres = splitted.firstOrNull() ?: ""
            val mask = (splitted.lastOrNull() ?: "0").toInt()
            CidrEntity(addres, mask)
        }.forEach {
            val count = calculateUsableHostCountBySubnetMask(it.subnetMask)
            ipCount += (count)
//            Timber.d("CFScanner: save  ${it.address}/${it.subnetMask} Count: $count")

            val result = cidrDao.insert(it.copy(position = lastPosition + 1))
            if(result>=0){
                lastPosition++
            }
        }
        Timber.d("CFScanner: All ip Count is $ipCount")
    }

    private fun getLastUpdateDate(): Date {
        return Date(tinyStorage.updateCIDR)
    }

    private fun saveLastUpdate() {
        tinyStorage.updateCIDR = Date().time
    }

    private fun isDeprecated(): Boolean {
        return getLastUpdateDate().before(Date(System.currentTimeMillis() - DEPRECATION_TIME))
    }

}