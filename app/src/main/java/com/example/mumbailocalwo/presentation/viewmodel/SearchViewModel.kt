package com.example.mumbailocalwo.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mumbailocalwo.data.model.SearchResult
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.model.StopEvent
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.data.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository.getInstance(application)

    // Station selection
    private val _fromStation = MutableStateFlow<Station?>(null)
    val fromStation: StateFlow<Station?> = _fromStation.asStateFlow()

    private val _toStation = MutableStateFlow<Station?>(null)
    val toStation: StateFlow<Station?> = _toStation.asStateFlow()

    // Station picker
    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _stationSearchQuery = MutableStateFlow("")
    val stationSearchQuery: StateFlow<String> = _stationSearchQuery.asStateFlow()

    // Search results
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Train detail
    private val _selectedTrain = MutableStateFlow<Train?>(null)
    val selectedTrain: StateFlow<Train?> = _selectedTrain.asStateFlow()

    private val _trainSchedule = MutableStateFlow<List<StopEvent>>(emptyList())
    val trainSchedule: StateFlow<List<StopEvent>> = _trainSchedule.asStateFlow()

    init {
        loadAllStations()
    }

    fun loadAllStations() {
        viewModelScope.launch {
            _stations.value = repository.getAllStations()
        }
    }

    fun searchStations(query: String) {
        _stationSearchQuery.value = query
        viewModelScope.launch {
            _stations.value = repository.searchStations(query)
        }
    }

    fun selectFromStation(station: Station) {
        _fromStation.value = station
    }

    fun selectToStation(station: Station) {
        _toStation.value = station
    }

    fun swapStations() {
        val temp = _fromStation.value
        _fromStation.value = _toStation.value
        _toStation.value = temp
    }

    fun searchTrains() {
        val from = _fromStation.value ?: return
        val to = _toStation.value ?: return

        viewModelScope.launch {
            _isSearching.value = true
            val cal = Calendar.getInstance()
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

            _searchResults.value = repository.findDirectTrains(
                fromStationId = from.id,
                toStationId = to.id
            )
            _isSearching.value = false
        }
    }

    fun loadTrainDetail(trainId: Int) {
        viewModelScope.launch {
            _selectedTrain.value = repository.getTrainById(trainId)
            _trainSchedule.value = repository.getTrainStops(trainId)
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _stationSearchQuery.value = ""
    }
}
