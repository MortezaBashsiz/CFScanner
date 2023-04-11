package ir.filternet.cfscanner.ui.page.sub.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import ir.filternet.cfscanner.contracts.BaseViewModel
import ir.filternet.cfscanner.offline.TinyStorage
import ir.filternet.cfscanner.repository.ConfigRepository
import ir.filternet.cfscanner.repository.ConnectionRepository
import ir.filternet.cfscanner.repository.ISPRepository
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class ScanSettingsScreenVM @Inject constructor(
    private val configRepository: ConfigRepository,
    private val connectionRepository: ConnectionRepository,
    private val ispRepository: ISPRepository,
    private val tinyStorage: TinyStorage,
) : BaseViewModel<ScanSettingsContract.Event, ScanSettingsContract.State, ScanSettingsContract.Effect>() {

    init {
        tinyStorage.scanSettings?.let {
            setState { copy(settings = it) }
        }
    }

    override fun setInitialState(): ScanSettingsContract.State {
        return ScanSettingsContract.State()
    }

    override fun handleEvents(event: ScanSettingsContract.Event) {
        when (event) {
            is ScanSettingsContract.Event.UpdateSettings -> {
                tinyStorage.scanSettings = event.settings
                setState { copy(settings = settings) }
            }
        }
    }

    override fun onCleared() {
        Timber.d("SettingsScreenVM onCleared")
        super.onCleared()
    }


}