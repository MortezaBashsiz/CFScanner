package ir.filternet.cfscanner.scanner

import ir.filternet.cfscanner.model.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CFSLogger @Inject constructor(){

    private var capacity = 35
    private val buffer = ArrayList<Log>()
    private val logBuffer = MutableStateFlow(buffer.toTypedArray())

    fun getBuffer() = logBuffer

    fun add(log:Log){

        synchronized(this) {
//            // check if exist just replace it
            val index = buffer.indexOfFirst { it.uid == log.uid }
            if(index>=0){
                buffer.removeAt(index)
                buffer.add(index,log)
            }else{
                val diff = (buffer.size+1) - capacity
                if(diff>0){
                    buffer.removeAt(0)
                }
                buffer.add(log)
            }
            logBuffer.tryEmit(buffer.toTypedArray())
        }
    }

    fun clear(){
        buffer.clear()
        logBuffer.tryEmit(buffer.toTypedArray())
    }

}