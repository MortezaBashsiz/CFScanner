package ir.filternet.cfscanner.model

data class Log(val uid: String, val text: String = "", val status: STATUS = STATUS.IDLE, val any: List<Int> = emptyList())

enum class STATUS(val int: Int) {
    IDLE(0), INPROGRESS(1), FAILED(2), SUCCESS(3)
}


