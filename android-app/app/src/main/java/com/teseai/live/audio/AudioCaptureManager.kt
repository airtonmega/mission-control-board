package com.teseai.live.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(): File {
        val outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
        currentFile = outputFile
        recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        return outputFile
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            currentFile
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun release() {
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        recorder = null
    }

    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
