package com.example.volumereader

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumereader.engine.AudioEngine
import com.example.volumereader.engine.CalibrationLogic
import com.example.volumereader.engine.PhoneModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VolumeReaderViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine()

    private val _selectedModel = MutableStateFlow(CalibrationLogic.defaultModel)
    val selectedModel: StateFlow<PhoneModel> = _selectedModel.asStateFlow()

    init {
        // Auto-detect device and select matching calibration profile
        val detected = CalibrationLogic.autoDetect()
        if (detected != null) {
            _selectedModel.value = detected
            audioEngine.calibrationOffset = detected.offset
        }
    }

    fun selectModel(model: PhoneModel) {
        _selectedModel.value = model
        audioEngine.calibrationOffset = model.offset
    }

    fun startRecording() {
        viewModelScope.launch {
            audioEngine.startRecording()
        }
    }

    fun stopRecording() {
        audioEngine.stopRecording()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopRecording()
    }
}
