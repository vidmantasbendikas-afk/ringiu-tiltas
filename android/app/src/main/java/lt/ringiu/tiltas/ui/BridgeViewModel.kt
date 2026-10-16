package lt.ringiu.tiltas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lt.ringiu.tiltas.data.MeteoService
import lt.ringiu.tiltas.data.Reading

class BridgeViewModel : ViewModel() {
    private val service = MeteoService

    private val _reading = MutableStateFlow<Reading?>(null)
    val reading: StateFlow<Reading?> = _reading.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fresh = withContext(Dispatchers.IO) { service.fetchLatestReading() }
                _reading.value = fresh
                _errorMessage.value = null
            } catch (e: Exception) {
                if (_reading.value == null) {
                    _errorMessage.value = "Nepavyko gauti duomenų iš meteo.lt"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
