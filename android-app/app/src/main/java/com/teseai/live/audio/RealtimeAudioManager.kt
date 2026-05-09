package com.teseai.live.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeAudioManager @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_MS = 100  // enviar chunks de 100ms
    }

    private val bufferSize: Int = maxOf(
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2,
        SAMPLE_RATE * 2 * CHUNK_SIZE_MS / 1000,
    )

    private var audioRecord: AudioRecord? = null
    var isCapturing: Boolean = false
        private set

    @Suppress("MissingPermission")
    suspend fun startCapture(onChunk: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )
        audioRecord = record
        isCapturing = true
        record.startRecording()

        val buffer = ByteArray(bufferSize)
        try {
            while (isActive && isCapturing) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    val base64 = Base64.encodeToString(chunk, Base64.NO_WRAP)
                    onChunk(base64)
                }
            }
        } finally {
            record.stop()
            record.release()
            audioRecord = null
            isCapturing = false
        }
    }

    fun stopCapture() {
        isCapturing = false
        audioRecord?.stop()
    }
}
