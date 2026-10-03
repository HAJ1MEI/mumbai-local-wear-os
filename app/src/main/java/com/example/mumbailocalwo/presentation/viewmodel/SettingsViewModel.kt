package com.example.mumbailocalwo.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mumbailocalwo.data.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimetableRepository.getInstance(application)

    private val _timetableVersion = MutableStateFlow("Loading...")
    val timetableVersion: StateFlow<String> = _timetableVersion.asStateFlow()

    private val _trainCount = MutableStateFlow(0)
    val trainCount: StateFlow<Int> = _trainCount.asStateFlow()

    private val _stationCount = MutableStateFlow(0)
    val stationCount: StateFlow<Int> = _stationCount.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    init {
        loadInfo()
    }

    fun loadInfo() {
        viewModelScope.launch {
            _timetableVersion.value = repository.getTimetableVersion()
            _trainCount.value = repository.getTrainCount()
            _stationCount.value = repository.getStationCount()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            // TODO: Implement in Phase 5 — Update Engine
            // For now, simulate a check
            kotlinx.coroutines.delay(1500)
            _updateStatus.value = UpdateStatus.UpToDate
        }
    }

    sealed class UpdateStatus {
        data object Idle : UpdateStatus()
        data object Checking : UpdateStatus()
        data object UpToDate : UpdateStatus()
        data class NewVersionAvailable(val version: String) : UpdateStatus()
        data object Downloading : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
        data object Updated : UpdateStatus()
    }
}
