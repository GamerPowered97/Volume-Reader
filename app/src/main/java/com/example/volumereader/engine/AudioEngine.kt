package com.example.volumereader.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class AudioEngine {

    private val _currentDbSpl = MutableStateFlow(0f)
    val currentDbSpl: StateFlow<Float> = _currentDbSpl.asStateFlow()

    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    // Base calibration offset to convert dBFS to an approximate dB SPL
    // The user can modify this offset in settings based on their device.
    var calibrationOffset = 85f

    @SuppressLint("MissingPermission")
    suspend fun startRecording() {
        if (isRecording) return

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return
        }

        // UNPROCESSED is ideal, but VOICE_RECOGNITION is a safe fallback to bypass some AGC
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            return
        }

        audioRecord?.startRecording()
        isRecording = true

        withContext(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readResult > 0) {
                    var sumOfSquares = 0.0
                    for (i in 0 until readResult) {
                        val sample = buffer[i].toDouble()
                        sumOfSquares += sample * sample
                    }
                    val rms = sqrt(sumOfSquares / readResult)
                    val dbfs = if (rms > 0) 20.0 * log10(rms / 32767.0) else -100.0

                    // Convert to SPL using offset
                    val dbSpl = (dbfs + calibrationOffset).toFloat().coerceAtLeast(0f)
                    
                    // Simple smoothing (Exponential moving average)
                    val smoothed = (_currentDbSpl.value * 0.8f) + (dbSpl * 0.2f)
                    _currentDbSpl.value = smoothed
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
