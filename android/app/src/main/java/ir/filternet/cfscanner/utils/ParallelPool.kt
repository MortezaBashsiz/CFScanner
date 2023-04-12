package ir.filternet.cfscanner.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.CoroutineContext

fun ParallelPool(counter: Int, workerCount: Int = 4, dispatcher: CoroutineContext = Dispatchers.IO, block: suspend (Int) -> Unit) {
    val scope = CoroutineScope(dispatcher)
    scope.launch {
        val pool = Semaphore(workerCount)
        repeat(counter) {
            pool.acquire()
            scope.launch {
                block(it)
                pool.release()
            }
        }
    }
}