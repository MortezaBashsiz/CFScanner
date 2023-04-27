package ir.filternet.cfscanner

import android.app.Application
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import dagger.hilt.android.HiltAndroidApp
import go.Seq
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import ir.filternet.cfscanner.utils.userAssetPath
import libv2ray.Libv2ray
import timber.log.Timber

@HiltAndroidApp
class CFScannerApplication : Application() {

    companion object {
        init {
            System.loadLibrary("nativelib")
        }
    }

    external fun DisableFDSAN()

    override fun onCreate() {
        super.onCreate()

        DisableFDSAN()

        RxJavaPlugins.setErrorHandler {
            YandexMetrica.reportError("RxJava",it)
        }

        // init V2ray
        Seq.setContext(this)
        Libv2ray.initV2Env(userAssetPath(this))


        // init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // firebase init
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.d("Fetching FCM registration token failed")
            }else{
                val token = task.result
                Timber.d("Fetched FCM token: $token")
            }

        })


        // yandex init
        val config = YandexMetricaConfig.newConfigBuilder(BuildConfig.YandexID)
            .withSessionTimeout(10)
            .build()
        YandexMetrica.activate(this, config)
        YandexMetrica.enableActivityAutoTracking(this)
    }
}