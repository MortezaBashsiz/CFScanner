package ir.filternet.cfscanner.model

import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
}
