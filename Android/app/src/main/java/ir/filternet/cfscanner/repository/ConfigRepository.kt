package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.db.entity.ConfigEntity
import ir.filternet.cfscanner.mapper.mapToConfig
import ir.filternet.cfscanner.mapper.mapToConfigEntity
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfig
import ir.filternet.cfscanner.utils.ConnectionUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor() : BasicRepository() {

    private val configDao by lazy { db.configDao() }

    suspend fun getAllConfig(): List<Config> {
       return configDao.getAllConfigs().map { it.mapToConfig() }.reversed()
    }

    suspend fun getConfigById(id:Int): Config{
        return configDao.loadAllByIds(intArrayOf(id)).first().mapToConfig()
    }

    suspend fun addConfig(config:Config){
        configDao.insert(config.mapToConfigEntity())
    }

    suspend fun deleteConfig(config:Config){
        configDao.delete(config.mapToConfigEntity())
    }

    suspend fun checkConfigIsCloudflare(config:V2rayConfig):Boolean{
       config.outbounds.filter { it.tag=="proxy" }.firstOrNull()?.settings?.vnext?.firstOrNull()?.run { address }?.let {
            if(ConnectionUtils.isCloudflareCDN(it)){
                return true
            }
        }
        return false
    }

    suspend fun updateConfig(config: Config) {
        configDao.insert(config.mapToConfigEntity())
    }
}