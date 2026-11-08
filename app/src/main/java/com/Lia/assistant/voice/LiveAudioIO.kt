package com.Lia.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private val isRecording = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private var volumeMultiplier: Float = 1.0f

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording.get()) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = Math.max(minBufferSize, 4096)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            audioRecord = record

            val audioSessionId = record.audioSessionId
            if (audioSessionId != 0) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
                try {
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
                try {
                    if (AutomaticGainControl.isAvailable()) {
                        gainControl = AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
            }

            record.startRecording()
            isRecording.set(true)

            recordThread = Thread({
                val buffer = ByteArray(2048)
                while (isRecording.get()) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val chunk = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                        val amp = calculateRms(chunk)
                        onMicAmplitude(amp)
                        onMicChunk(chunk)
                    }
                }
            }, "LiveAudioIO-RecordThread").apply { start() }

        } catch (_: Exception) {
            isRecording.set(false)
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordThread?.interrupt()
        recordThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        echoCanceler?.release()
        echoCanceler = null
        noiseSuppressor?.release()
        noiseSuppressor = null
        gainControl?.release()
        gainControl = null
    }

    private fun getOrCreateAudioTrack(): AudioTrack? {
        if (audioTrack != null) return audioTrack
        return try {
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBuf * 4

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()

            val track = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.setVolume(volumeMultiplier)
            track.play()
            audioTrack = track
            track
        } catch (_: Exception) {
            null
        }
    }

    fun playChunk(bytes: ByteArray): Float {
        val track = getOrCreateAudioTrack() ?: return 0f
        track.write(bytes, 0, bytes.size)
        return calculateRms(bytes)
    }

    fun flushPlayback() {
        try {
            audioTrack?.let {
                it.pause()
                it.flush()
                it.play()
            }
        } catch (_: Exception) {}
    }

    fun setVolume(volume: Float) {
        volumeMultiplier = volume.coerceIn(0f, 1.5f)
        try {
            audioTrack?.setVolume(volumeMultiplier)
        } catch (_: Exception) {}
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    companion object {
        fun calculateRms(pcmBytes: ByteArray): Float {
            if (pcmBytes.isEmpty()) return 0f
            var sumSquare = 0.0
            val samplesCount = pcmBytes.size / 2
            if (samplesCount == 0) return 0f

            for (i in 0 until samplesCount) {
                val b1 = pcmBytes[i * 2].toInt() and 0xFF
                val b2 = pcmBytes[i * 2 + 1].toInt()
                val sample = (b2 shl 8) or b1
                sumSquare += (sample * sample).toDouble()
            }
            val mean = sumSquare / samplesCount
            val rawRms = (sqrt(mean) / 32768.0).toFloat()
            return rawRms.coerceIn(0f, 1f)
        }
    }
}
