package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(): Result<File> = runCatching {
        stopRecordingCleanup()
        val file = File(context.cacheDir, "aura_voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        isRecording = true
        file
    }

    fun stopAndReadBytes(): Result<ByteArray> = runCatching {
        val activeRecorder = recorder
            ?: throw IllegalStateException("No active recording session.")
        try {
            activeRecorder.stop()
        } catch (e: Exception) {
            Log.w("AudioRecorderHelper", "MediaRecorder stop warning", e)
        } finally {
            activeRecorder.release()
            recorder = null
            isRecording = false
        }

        val file = outputFile ?: throw IllegalStateException("Recording file missing.")
        if (!file.exists() || file.length() == 0L) {
            throw IllegalStateException("Recorded audio file is empty.")
        }
        val bytes = file.readBytes()
        file.delete()
        bytes
    }

    fun stopRecordingCleanup() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
    }
}
