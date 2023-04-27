package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.mapper.mapToISP
import ir.filternet.cfscanner.mapper.mapToIspEntity
import ir.filternet.cfscanner.model.ISP
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ISPRepository @Inject constructor() : BasicRepository() {

    private val ispDao by lazy { db.ispDao() }

    suspend fun getAllISPs(): List<ISP> {
        return ispDao.getAllISP().map { it.mapToISP() }
    }

    suspend fun getIspByName(name:String):ISP?{
        return ispDao.findIspByName(name)?.mapToISP()
    }

    suspend fun addISP(isp:ISP):ISP{
        val oldISP = getIspByName(isp.name)
        if(oldISP!=null){
            return oldISP
        }
        val id = ispDao.insert(isp.mapToIspEntity())
        return ispDao.findIspById(id.toInt())?.mapToISP()!!
    }

    suspend fun addAndGetAll(isp:ISP):List<ISP>{
        val list = arrayListOf<ISP>()
        val listOfISP = getAllISPs().toMutableList()

        //check isp existence
        val oldISP = listOfISP.find { it.name == isp.name  }
        if(oldISP==null){
            list.add(isp)
        }else{
            list.add(oldISP)
            listOfISP.remove(oldISP)
            list.addAll(listOfISP)
        }
        return list
    }
}