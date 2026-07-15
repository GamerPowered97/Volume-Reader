package com.example.volumereader

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumereader.engine.AudioEngine
import com.example.volumereader.engine.CalibrationLogic
import com.example.volumereader.engine.PhoneModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class UpdateAvailable(val version: String, val downloadUrl: String) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class VolumeReaderViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine()

    private val _selectedModel = MutableStateFlow(CalibrationLogic.defaultModel)
    val selectedModel: StateFlow<PhoneModel> = _selectedModel.asStateFlow()

    // Current version in app. Hardcoded to match our v2.3.0 release.
    val currentVersion = "v2.3.0"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

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

    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Checking
            try {
                val url = URL("https://api.github.com/repos/GamerPowered97/Volume-Reader/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "VolumeReaderApp")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val latestVersion = json.getString("tag_name")
                    
                    if (isNewerVersion(currentVersion, latestVersion)) {
                        val assets = json.getJSONArray("assets")
                        var downloadUrl = ""
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                        if (downloadUrl.isNotEmpty()) {
                            _updateState.value = UpdateState.UpdateAvailable(latestVersion, downloadUrl)
                        } else {
                            _updateState.value = UpdateState.Error("No APK found in the latest release.")
                        }
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                } else {
                    _updateState.value = UpdateState.Error("Check failed (HTTP ${connection.responseCode}).")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val curNum = current.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
            val latNum = latest.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
            
            for (i in 0 until minOf(curNum.size, latNum.size)) {
                if (latNum[i] > curNum[i]) return true
                if (latNum[i] < curNum[i]) return false
            }
            return latNum.size > curNum.size
        } catch (e: Exception) {
            return false
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Downloading(0f)
            try {
                val context = getApplication<Application>().applicationContext
                val destination = File(context.externalCacheDir ?: context.cacheDir, "update.apk")
                if (destination.exists()) {
                    destination.delete()
                }

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val fileLength = connection.contentLength
                
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (fileLength > 0) {
                                val progress = totalBytesRead.toFloat() / fileLength
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                    }
                }
                
                _updateState.value = UpdateState.ReadyToInstall(destination)
                installApk(destination)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Download failed: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(file: File) {
        try {
            val context = getApplication<Application>().applicationContext
            val authority = "com.example.volumereader.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to launch installer: ${e.localizedMessage}")
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopRecording()
    }
}
