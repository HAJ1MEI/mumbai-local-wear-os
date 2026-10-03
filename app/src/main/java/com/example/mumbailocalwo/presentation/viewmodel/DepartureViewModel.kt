package com.example.mumbailocalwo.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.model.StopEvent
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.data.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DepartureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository.getInstance(application)

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _departures = MutableStateFlow<List<Pair<StopEvent, Train>>>(emptyList())
    val departures: StateFlow<List<Pair<StopEvent, Train>>> = _departures.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    init {
        loadAllStations()
    }

    fun loadAllStations() {
        viewModelScope.launch {
            _stations.value = repository.getAllStations()
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            _stations.value = repository.searchStations(query)
        }
    }

    fun selectStation(station: Station) {
        _selectedStation.value = station
        loadDepartures()
    }

    fun loadDepartures(direction: String? = null, trainType: String? = null) {
        val station = _selectedStation.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _departures.value = emptyList()
            _isLoading.value = false
        }
    }
}
