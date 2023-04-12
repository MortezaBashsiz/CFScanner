package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.db.entity.ConfigEntity
import ir.filternet.cfscanner.mapper.mapToConfig
import ir.filternet.cfscanner.mapper.mapToConfigEntity
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfig
import ir.filternet.cfscanner.utils.AppConfig
import ir.filternet.cfscanner.utils.ConnectionUtils
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(private val retrofit: OkHttpClient) : BasicRepository() {

    private val configDao by lazy { db.configDao() }

    suspend fun getDefaultConfig(): Config? {
        return getDefaultConfigFromGithub()
    }

    suspend fun getAllConfig(): List<Config> {
        return configDao.getAllConfigs().map { it.mapToConfig() }.reversed()
    }

    suspend fun getConfigById(id: Int): Config {
        return configDao.loadAllByIds(intArrayOf(id)).first().mapToConfig()
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
            val request = Request.Builder()
                .url(AppConfig.Config_Address)
                .build()
            val response = okHttp.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val obj = Json.decodeFromString<JsonObject>(body)
            val id = obj.get("id")?.jsonPrimitive?.content
            val host = obj.get("host")?.jsonPrimitive?.content
            val port = obj.get("port")?.jsonPrimitive?.content
            val path = obj.get("path")?.jsonPrimitive?.content
            val serverName = obj.get("serverName")?.jsonPrimitive?.content
            val config = "vless://${id}@${host}:${port}?encryption=none&security=tls&sni=${serverName}&type=ws&host=${host}&path=${path}#Default"
            Config(config, "Default", uid = 1324)
        } catch (e: Exception) {
            println("An error occurred: " + e.message)
            e.printStackTrace()
            null
        }
    }
}