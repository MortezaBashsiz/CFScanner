package ir.filternet.cfscanner.ui.page.sub.scan

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.model.ScanButtonState
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.scanner.CFSLogger
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil
import ir.filternet.cfscanner.service.CloudScannerService
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ScanScreenVM @Inject constructor(
    private val configRepository: ConfigRepository,
    private val logger: CFSLogger,
) : BaseViewModel<ScanContract.Event, ScanContract.State, ScanContract.Effect>(), ServiceConnection, CloudScannerService.CloudScannerServiceListener {

    private var binder: CloudScannerService.CloudScannerServiceBinder? = null

    @Inject
    lateinit var v2rayUtils: V2rayConfigUtil

    init {
        Timber.d("ScanScreenVM: init")
        vmScope {
            logger.getBuffer().collect {
                setState {
                    this.copy(logs = it.toList())
                }
            }
        }
        vmScope {
            updateConfigs()
        }
    }

    override fun setInitialState(): ScanContract.State {
        return ScanContract.State(true)
    }

    override fun handleEvents(event: ScanContract.Event) {
        when (event) {
            is ScanContract.Event.AddConfig -> {
                addConfig(event.config)
            }
            is ScanContract.Event.SelectConfig -> {
                selectConfig(event.config)
            }
            is ScanContract.Event.DeleteConfig -> {
                deleteConfig(event.config)
            }
            is ScanContract.Event.UpdateConfig -> {
                updateConfig(event.config)
            }
            is ScanContract.Event.StartScan -> {
                startScan()
            }
            is ScanContract.Event.StopScan -> {
                stopScan()
            }

        }
    }

    private fun startScan() {
        viewState.value.configs.find { it.selected }?.let {
            logger.clear()
            binder?.startScan(it)
        }
    }

    private fun stopScan() {
        binder?.pauseScan()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Timber.d("ScanScreenVM: onServiceConnected")
        binder = service as CloudScannerService.CloudScannerServiceBinder
        binder?.setServiceListener(this)
        onServiceStatusChanged(binder?.getServiceStatus())
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Timber.d("ScanScreenVM: onServiceDisconnected")
        binder?.removeServiceListener()
        binder = null
    }

    private fun addConfig(config: String) = vmScope(Dispatchers.IO) {
        setState { copy(loading = true) }
        val serverConfig = v2rayUtils.createServerConfig(config)
        val v2rayConfig = serverConfig?.fullConfig

        if (v2rayConfig == null) {
            setState { copy(loading = false) }
            setEffect { ScanContract.Effect.Messenger.Toast(messageId = R.string.invalid_config) }
            return@vmScope
        }

        if (!v2rayConfig.isWSConnection()) {
            setState { copy(loading = false) }
            setEffect { ScanContract.Effect.Messenger.Toast(messageId = R.string.invalid_config_ws) }
            return@vmScope
        }

        if (!v2rayConfig.isVmessVlessConnection()) {
            setState { copy(loading = false) }
            setEffect { ScanContract.Effect.Messenger.Toast(messageId = R.string.invalid_config_type) }
            return@vmScope
        }

        if (!configRepository.checkConfigIsCloudflare(v2rayConfig)) {
            setState { copy(loading = false) }
            setEffect { ScanContract.Effect.Messenger.Toast(messageId = R.string.invalid_config_not_cloudflare) }
            return@vmScope
        }

        if (serverConfig.remarks.isEmpty()) {
            setState { copy(loading = false) }
            setEffect { ScanContract.Effect.Dialog.ConfigEditDialog(Config(config, serverConfig.remarks)) }
            return@vmScope
        }


        configRepository.addConfig(Config(config, serverConfig.remarks))
        updateConfigs()
    }


    private fun deleteConfig(config: Config) = vmScope(Dispatchers.IO) {
        configRepository.deleteConfig(config)
        updateConfigs()
    }

    private fun updateConfig(config: Config) = vmScope(Dispatchers.IO) {
        configRepository.updateConfig(config)
        updateConfigs()
    }

    private fun selectConfig(config: Config) {
        setState {
            val configs = this.configs.map { it.copy(selected = it.uid == config.uid) }
            copy(loading = false, configs = configs)
        }
        onServiceStatusChanged(binder?.getServiceStatus())
    }

    private fun updateConfigs() = vmScope(Dispatchers.IO) {
        val configs = configRepository.getAllConfig()
        setState {
            val selectedUID = this.configs.find { it.selected }?.uid ?: -1
            val confs = if (selectedUID >= 0) {
                configs.mapIndexed { index, config -> config.copy(selected = config.uid == selectedUID) }
            } else {
                configs.mapIndexed { index, config -> config.copy(selected = index == 0) }
            }
            copy(loading = false, configs = confs)
        }
        onServiceStatusChanged(binder?.getServiceStatus())
    }

    override fun onServiceStatusChanged(status: CloudScannerService.ServiceStatus?) {
        Timber.d("ScanScreenVM: onServiceStatusChanged: $status")
        when (status) {
            is CloudScannerService.ServiceStatus.Idle -> {
                val isSelectedConfig = !viewState.value.configs.none { it.selected }
                if (isSelectedConfig) {
                    setState { copy(buttonState = ScanButtonState.Ready) }
                } else {
                    setState { copy(buttonState = ScanButtonState.Disabled()) }
                }
            }
            is CloudScannerService.ServiceStatus.Scanning -> {
                setState { copy(buttonState = ScanButtonState.Scanning(status.scan, status.scan.progress)) }
            }
            is CloudScannerService.ServiceStatus.Paused -> {
                setState { copy(buttonState = ScanButtonState.Paused(status.message)) }
            }
            is CloudScannerService.ServiceStatus.Disabled -> {
                setState { copy(buttonState = ScanButtonState.Disabled(status.message)) }
            }
            else -> {}
        }

    }


}