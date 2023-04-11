package ir.filternet.cfscanner.service

import android.content.Context
import android.net.*
import androidx.core.content.ContextCompat
import ir.filternet.cfscanner.model.ISP
import ir.filternet.cfscanner.utils.ConnectionUtils
import ir.filternet.cfscanner.utils.ConnectionUtils.isVpnConnection
import kotlinx.coroutines.*
import timber.log.Timber

class NetworkManager(context: Context) : ConnectivityManager.NetworkCallback() {

    private var lastState: NetworkState = NetworkState.DISCONNECTED
    private var lastIsp: ISP? = null

    private var ispCheckJob:Job? = null
    private var retryingJob:Job? = null

    private var monitorEnabled = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java) as ConnectivityManager
    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
        .build()
    private var listener: NetworkManagerListener? = null



    init {
        connectivityManager.requestNetwork(networkRequest, this)
        checkConnection()
    }

    fun startMonitor(){
        monitorEnabled = true
        checkConnection()
    }

    fun stopMonitor(){
        monitorEnabled = false
        checkConnection()
    }

    fun setListener(listener: NetworkManagerListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    fun getNetworkState():NetworkState = lastState

    fun isConnectedToVpn():Boolean = connectivityManager.isVpnConnection()

    fun getNetworkISP():ISP? = lastIsp


    override fun onAvailable(network: Network) {
        Timber.d("NetworkManager: onAvailable $network")
        setConnectionState(NetworkState.WAITING)
        retryingJob?.cancel()
    }

    override fun onLosing(network: Network, maxMsToLive: Int) {
        Timber.d("NetworkManager: onLosing $network $maxMsToLive")
        setConnectionState(NetworkState.WAITING)
        onWaitingForNewConnectionOrDisconnect()
    }

    override fun onLost(network: Network) {
        Timber.d("NetworkManager: onLost $network")
        setConnectionState(NetworkState.WAITING)
        onWaitingForNewConnectionOrDisconnect()
    }

    override fun onUnavailable() {
        Timber.d("NetworkManager: onUnavailable")
        setConnectionState(NetworkState.DISCONNECTED)
    }

    // call when user connectionEntity change (also may change isp)
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
//        Timber.d("NetworkManager: onCapabilitiesChanged $network $networkCapabilities")

//        if(connectivityManager.isVpnConnection()){
//            setConnectionState(NetworkState.DISCONNECTED)
//            Timber.d("NetworkManager: onCapabilitiesChanged Vpn Detected !")
//        }

        checkConnection()
    }


    private fun setConnectionState(state: NetworkState) {
        if(this.lastState == state) return

        Timber.d("NetworkManager: State Changed from ${this.lastState} to $state")
        this.lastState = state
        when (state) {
            NetworkState.CONNECTED -> listener?.onNetworkStatusUpdate(state)
            NetworkState.DISCONNECTED -> listener?.onNetworkStatusUpdate(state)
            NetworkState.WAITING -> listener?.onNetworkStatusUpdate(state)
        }
    }

    /* check real connectionEntity to google.com and get Dns if connectionEntity change !*/
    private fun checkConnection(){
        ispCheckJob?.cancel()
        ispCheckJob =  scope.launch {
            var waitingCounter = 0
            while (monitorEnabled) {
                delay(500)
                if (getNetworkState() != NetworkState.DISCONNECTED) {
                    if (!ConnectionUtils.canPingAddress()) {
                        if (getNetworkState() != NetworkState.DISCONNECTED && waitingCounter>3) {
                            setConnectionState(NetworkState.WAITING)
                        }
                        waitingCounter++
                    }else{
                        waitingCounter = 0
                        if(getNetworkState() != NetworkState.CONNECTED){
                            // check new state with new isp then continue
                            getCurrentIsp()
                            setConnectionState(NetworkState.CONNECTED)
                        }

                    }
                }
                delay(500)
            }
        }
    }


    private suspend fun getCurrentIsp(){
        val lastIsp = this.lastIsp
        val currentIsp = ConnectionUtils.getCurrentIspName()
        if(lastIsp!=currentIsp){
            listener?.onNetworkIspChanged(currentIsp)
            Timber.d("NetworkManager: onIspChanged from ${lastIsp?:"NONE"} to $currentIsp")
            this.lastIsp = currentIsp
        }

    }


    /**
     * when connectionEntity lost , timer will trigger .
     * if connect to stable connectionEntity then Disconnect state will dispose
     * @param timeout Long
     */
    private fun onWaitingForNewConnectionOrDisconnect(timeout:Long = 10000){
        retryingJob?.cancel()
        retryingJob = scope.launch {
            delay(timeout)
            setConnectionState(NetworkState.DISCONNECTED)
        }
    }


    interface NetworkManagerListener {

        fun onNetworkStatusUpdate(status:NetworkState)

        fun onNetworkIspChanged(isp: ISP?)
    }

    enum class NetworkState {
        CONNECTED, DISCONNECTED, WAITING
    }
}