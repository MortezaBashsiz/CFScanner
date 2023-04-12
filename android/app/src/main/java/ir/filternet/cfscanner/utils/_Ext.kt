package ir.filternet.cfscanner.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.ISP
import java.net.URI
import java.net.URLConnection


val URLConnection.responseLength: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) contentLengthLong else contentLength.toLong()

val URI.idnHost: String
    get() = (host!!).replace("[", "").replace("]", "")


fun ISP.parseToCommonName(context: Context): String {
    val nameMap = mapOf(
        "Mobile Communication Company of Iran PLC" to R.string.mci,
        "Iran Cell" to R.string.irancell,
        "Shatel" to R.string.shatel,
        "Iran Telecommunication Company" to R.string.mokhaberat,
        "Information Technology Company" to R.string.fanavari_etteleat,
        "Rightel" to R.string.righTel,
        "Fars Telecommunication Company" to R.string.fars,
        "Pars Fonoun Ofogh" to R.string.pars_ofough,
        "Mobin Net" to R.string.mobin_net,
        "Pishgaman Toseeh" to R.string.pishgaman,
        "GOSTARESH" to R.string.gostaresh,
        "Tarahan" to R.string.tarahan,
        "Aryan" to R.string.aryan,
        "Afranet" to R.string.afranet,
        "Spadana" to R.string.spadana,
        "Rayaneh" to R.string.rayaneh,
        "Soroush" to R.string.sorough,
        "IRIB" to R.string.irib,
        "Pasargad" to R.string.pasargad,
        "University of Tehran" to R.string.tu,
        "IRNA" to R.string.irna,
        "Pars Online" to R.string.pars_online,
        "Dadeh Gostar" to R.string.hi_web,
        "Asiatech" to R.string.asiatech,
        "Padidar" to R.string.paidar,
        "Raya Sepehr" to R.string.rayan_sepehr,
        "Fanava" to R.string.fanava,
        "DATAK" to R.string.datak,
        "Shabdiz" to R.string.shabdiz,
        "Boomerang" to R.string.boomrang,
    )
    nameMap.forEach {
        if (this.name.contains(it.key,true)) {
            return context.getString(it.value)
        }
    }
    return this.name.split(" ").firstOrNull()?:"Unknown"
}


fun Context.openBrowser(link:String){
    val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(link))
    ContextCompat.startActivity(this,intent,null)
}
