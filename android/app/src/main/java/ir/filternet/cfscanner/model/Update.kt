package ir.filternet.cfscanner.model

data class Update(
    val versionName:String,
    val versionCode:Int,
    val downloadLink:String,
    val size:Long,
    val changes:List<String> = emptyList(),
    val force:Boolean = false,
    val state:UpdateState = UpdateState.Idle,
)
