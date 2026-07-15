package com.example.volumereader.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.sqrt

class AudioEngine {

    private val _currentDbSpl = MutableStateFlow(0f)
    val currentDbSpl: StateFlow<Float> = _currentDbSpl.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val recording = AtomicBoolean(false)

    @Volatile
    var calibrationOffset = 85f

    private val lock = Any()
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    suspend fun startRecording() {
        if (recording.getAndSet(true)) return  // already recording

        _isRecording.value = true
        _error.value = null

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _error.value = "Microphone unavailable on this device."
            recording.set(false)
            _isRecording.value = false
            return
        }

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: SecurityException) {
            _error.value = "Audio permission was revoked."
            recording.set(false)
            _isRecording.value = false
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()  // Bug #2 fix: release on failed init
            _error.value = "Could not initialize microphone."
            recording.set(false)
            _isRecording.value = false
            return
        }

        synchronized(lock) { audioRecord = recorder }
        recorder.startRecording()

        try {
            withContext(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize)
                while (recording.get() && isActive) {  // Bug #4 fix: check coroutine active
                    val localRecorder = synchronized(lock) { audioRecord } ?: break
                    val readResult = localRecorder.read(buffer, 0, buffer.size)
                    if (readResult > 0) {
                        var sumOfSquares = 0.0
                        for (i in 0 until readResult) {
                            val sample = buffer[i].toDouble()
                            sumOfSquares += sample * sample
                        }
                        val rms = sqrt(sumOfSquares / readResult)
                        // Bug #18 fix: guard against subnormal near-zero
                        val dbfs = if (rms > 1e-10) 20.0 * log10(rms / 32767.0) else -100.0

                        val offset = calibrationOffset  // volatile read once
                        val dbSpl = (dbfs + offset).toFloat().coerceIn(0f, 150f)

                        // Smoothing (EMA with faster response: 0.6 old, 0.4 new)
                        val prev = _currentDbSpl.value
                        val smoothed = (prev * 0.6f) + (dbSpl * 0.4f)
                        _currentDbSpl.value = smoothed
                    } else if (readResult < 0) {
                        break  // read error, exit loop
                    }
                }
            }
        } finally {
            // Always clean up, even on cancellation
            releaseRecorder()
            recording.set(false)
            _isRecording.value = false
        }
    }

    fun stopRecording() {
        recording.set(false)
        releaseRecorder()
        _isRecording.value = false
    }

    private fun releaseRecorder() {
        synchronized(lock) {
            audioRecord?.let { rec ->
                try {
                    if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        rec.stop()  // Bug #3 fix: guard stop()
                    }
                } catch (_: IllegalStateException) { }
                try {
                    rec.release()
                } catch (_: Exception) { }
            }
            audioRecord = null
        }
    }
}
