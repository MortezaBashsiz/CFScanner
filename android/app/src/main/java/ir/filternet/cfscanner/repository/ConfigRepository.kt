package ir.filternet.cfscanner.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.db.entity.ConfigEntity
import ir.filternet.cfscanner.mapper.mapToConfig
import ir.filternet.cfscanner.mapper.mapToConfigEntity
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfig
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil
import ir.filternet.cfscanner.utils.AppConfig
import ir.filternet.cfscanner.utils.ConnectionUtils
import ir.filternet.cfscanner.utils.generateString
import ir.filternet.cfscanner.utils.getFromGithub
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    @ApplicationContext private val context:Context,
    private val retrofit: OkHttpClient,
    private val scanRepository: ScanRepository,
    private val v2rayUtils: V2rayConfigUtil,
) : BasicRepository() {


    private val configDao by lazy { db.configDao() }

    suspend fun getDefaultConfig(): Config? {
        return getDefaultConfigFromGithub()?.fillByV2rayConfig()
    }

    suspend fun getAllConfig(): List<Config> {
        return configDao.getAllConfigs().map { it.mapToConfig().fillByV2rayConfig() }.reversed()
    }

    suspend fun getConfigById(id: Int): Config {
        return configDao.loadAllByIds(intArrayOf(id)).first().mapToConfig().fillByV2rayConfig()
    }

    suspend fun addConfig(config: Config) {
        configDao.insert(config.mapToConfigEntity())
    }

    suspend fun deleteConfig(config: Config) {
        configDao.delete(config.mapToConfigEntity())
    }

    suspend fun checkConfigIsCloudflare(config: V2rayConfig): Boolean {
        config.outbounds.filter { it.tag == "proxy" }.firstOrNull()?.settings?.vnext?.firstOrNull()?.run { address }?.let {
            if (ConnectionUtils.isCloudflareCDN(it)) {
                return true
            }
        }
        return false
    }

    suspend fun updateConfig(config: Config) {
        configDao.insert(config.mapToConfigEntity())
    }

    /**
     * get default config from github
     * @return Config?
     */
    private suspend fun getDefaultConfigFromGithub(): Config? {
        return try {
            val body = getFromGithub(AppConfig.Config_Address)
            val obj = Json.decodeFromString<JsonObject>(body)
            val id = obj.get("id")?.jsonPrimitive?.content
            val host = obj.get("host")?.jsonPrimitive?.content
            val port = obj.get("port")?.jsonPrimitive?.content
            val path = obj.get("path")?.jsonPrimitive?.content
            val serverName = (generateString()+"."+ host?.substringAfter(".")) ?: ""
            val config = v2rayUtils.getDefaultConfigTemplate()
                .replace("IDID", id ?: "")
                .replace("IP.IP.IP.IP", host ?: "")
                .replace("CFPORTCFPORT", port ?: "")
                .replace("HOSTHOST", host ?: "")
                .replace("ENDPOINTENDPOINT", path?: "")
                .replace("RANDOMHOST", serverName ?: "")
            Config(config, context.getString(R.string.default_config), uid = 1324)
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private fun Config.fillByV2rayConfig():Config = this.copy(v2rayConfig = v2rayUtils.createV2rayConfig(this.config))
}