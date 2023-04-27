package ir.filternet.cfscanner.repository

import ir.filternet.cfscanner.BuildConfig
import ir.filternet.cfscanner.contracts.BasicRepository
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.utils.AppConfig.APPLICATION_REPOSITORY
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UpdateRepository @Inject constructor(private val client: OkHttpClient) : BasicRepository() {


    suspend fun checkUpdate():Update? {
        try {
            var endOfScan = false
            var page = 0
            while (endOfScan.not()) {
                page++
                delay(500)
                Timber.d("Start request to get update page $page")
                val document: Document = Jsoup.connect(APPLICATION_REPOSITORY + "?page=${page}").get()
                val containers = document.getElementsByAttribute("data-hpc")
                val sections = document.getElementsByTag("section")

                if (sections.size == 0) {
                    break
                }


                for (sec in sections) {
                    val title = sec.getElementsByTag("h2").first().text()

                    if (!title.contains("android", true))
                        continue
                    else
                        endOfScan = true

                    val versionName = title.substringAfter("V").substringBefore("-C")
                    val versionCode = title.substringAfter("-C").trim().toIntOrNull() ?: BuildConfig.VERSION_CODE


                    if (versionCode <= BuildConfig.VERSION_CODE)
                        continue

                    val changeItemsContainer = sec.getElementsByClass("Box-body").first()
                    val changeItems = changeItemsContainer.getElementsByTag("li").map { it.text() }
                    val tag = sec.getElementsByTag("span").first().text()
                    val apkName = "CFScanner-V$versionName-C$versionCode-release-all.apk"
                    val apkDownloadUrl = "$APPLICATION_REPOSITORY/download/$tag/$apkName"


                    delay(1000)
                    val request = Request.Builder().url(apkDownloadUrl).build()
                    val response = client.newBuilder().followRedirects(true).build().newCall(request).execute()
                    val length = response.body?.contentLength()


                    return Update(versionName,versionCode,apkDownloadUrl,length?:-1,changeItems)

//                        val aos = response.body?.byteStream()
//                        val reader = BufferedReader(InputStreamReader(aos))
//                        var result: String?
//                        var line = reader.readLine()
//                        result = line
//                        while (reader.readLine().also { line = it } != null) {
//                            result += line
//                        }
//                        println(result)
//                        response.body?.close()

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}