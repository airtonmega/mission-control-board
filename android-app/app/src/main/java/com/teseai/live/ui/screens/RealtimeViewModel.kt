package com.teseai.live.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.audio.RealtimeAudioManager
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.data.repository.RealtimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed interface RealtimeUiState {
    data object Idle : RealtimeUiState
    data object RequestingToken : RealtimeUiState
    data object Connecting : RealtimeUiState
    data object Listening : RealtimeUiState
    data class Transcribing(val partial: String) : RealtimeUiState
    data class Responding(val userTranscript: String, val partial: String) : RealtimeUiState
    data class Done(val userTranscript: String, val aiResponse: String) : RealtimeUiState
    data class Error(val message: String, val offerFallback: Boolean = true) : RealtimeUiState
}

@HiltViewModel
class RealtimeViewModel @Inject constructor(
    private val realtimeRepo: RealtimeRepository,
    private val analyzeRepo: AnalyzeRepository,
    private val audioManager: RealtimeAudioManager,
) : ViewModel() {

    companion object {
        private const val TAG = "RealtimeVM"
        private const val REALTIME_WS_URL =
            "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-12-17"
    }

    private val _uiState = MutableStateFlow<RealtimeUiState>(RealtimeUiState.Idle)
    val uiState: StateFlow<RealtimeUiState> = _uiState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var audioJob: Job? = null
    private var userTranscript = ""
    private var aiPartial = ""

    // Latency tracking
    private var tokenRequestedAt = 0L
    private var wsOpenedAt = 0L
    private var firstTranscriptAt = 0L
    private var firstResponseAt = 0L

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // no read timeout for streaming
        .build()

    fun startSession() {
        viewModelScope.launch {
            tokenRequestedAt = System.currentTimeMillis()
            _uiState.value = RealtimeUiState.RequestingToken

            val sessionId = try {
                analyzeRepo.getSessionId()
            } catch (e: Exception) {
                _uiState.value = RealtimeUiState.Error("Sessão inválida: ${e.message}", offerFallback = true)
                return@launch
            }

            val sessionResp = try {
                realtimeRepo.createSession(sessionId)
            } catch (e: Exception) {
                _uiState.value = RealtimeUiState.Error(
                    "Não foi possível criar sessão realtime: ${e.message}",
                    offerFallback = true,
                )
                return@launch
            }

            // Mock mode → offer fallback immediately
            if (sessionResp.mock) {
                _uiState.value = RealtimeUiState.Error(
                    "Modo Realtime indisponível no modo mock. Use o modo Áudio padrão.",
                    offerFallback = true,
                )
                return@launch
            }

            val tokenLatencyMs = System.currentTimeMillis() - tokenRequestedAt
            Log.i(TAG, "token_obtained latency_ms=$tokenLatencyMs session=${sessionResp.realtimeSessionId}")

            _uiState.value = RealtimeUiState.Connecting
            wsOpenedAt = System.currentTimeMillis()

            openWebSocket(sessionResp.clientSecret.value)
        }
    }

    private fun openWebSocket(ephemeralToken: String) {
        val request = Request.Builder()
            .url(REALTIME_WS_URL)
            .header("Authorization", "Bearer $ephemeralToken")
            .header("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val wsLatencyMs = System.currentTimeMillis() - wsOpenedAt
                Log.i(TAG, "ws_opened latency_ms=$wsLatencyMs")

                // Configure session
                val sessionUpdate = JSONObject().apply {
                    put("type", "session.update")
                    put("session", JSONObject().apply {
                        put("modalities", org.json.JSONArray().apply {
                            put("text")
                            put("audio")
                        })
                        put("input_audio_format", "pcm16")
                        put("output_audio_format", "pcm16")
                        put("input_audio_transcription", JSONObject().put("model", "whisper-1"))
                        put("turn_detection", JSONObject().apply {
                            put("type", "server_vad")
                            put("threshold", 0.5)
                            put("silence_duration_ms", 800)
                            put("prefix_padding_ms", 300)
                        })
                    })
                }
                ws.send(sessionUpdate.toString())
                _uiState.value = RealtimeUiState.Listening
                startAudioCapture(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerEvent(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "ws_failure: ${t.message}")
                stopAudioCapture()
                _uiState.value = RealtimeUiState.Error(
                    "Conexão perdida: ${t.message}",
                    offerFallback = true,
                )
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "ws_closed code=$code reason=$reason")
                stopAudioCapture()
            }
        })
    }

    private fun handleServerEvent(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "session.created" -> Log.i(TAG, "session_created")

                "input_audio_buffer.speech_started" -> {
                    userTranscript = ""
                    if (firstTranscriptAt == 0L) firstTranscriptAt = System.currentTimeMillis()
                    _uiState.value = RealtimeUiState.Transcribing("")
                }

                "conversation.item.input_audio_transcription.delta" -> {
                    val delta = json.optString("delta", "")
                    userTranscript += delta
                    _uiState.value = RealtimeUiState.Transcribing(userTranscript)
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = json.optJSONObject("transcript")?.optString("text", userTranscript)
                        ?: userTranscript
                    userTranscript = transcript
                    aiPartial = ""
                    _uiState.value = RealtimeUiState.Responding(userTranscript, "")
                }

                "response.audio_transcript.delta" -> {
                    if (firstResponseAt == 0L) {
                        firstResponseAt = System.currentTimeMillis()
                        Log.i(TAG, "first_response_delta latency_ms=${firstResponseAt - (if (firstTranscriptAt > 0) firstTranscriptAt else wsOpenedAt)}")
                    }
                    val delta = json.optString("delta", "")
                    aiPartial += delta
                    _uiState.value = RealtimeUiState.Responding(userTranscript, aiPartial)
                }

                "response.text.delta" -> {
                    val delta = json.optString("delta", "")
                    aiPartial += delta
                    _uiState.value = RealtimeUiState.Responding(userTranscript, aiPartial)
                }

                "response.done" -> {
                    stopAudioCapture()
                    _uiState.value = RealtimeUiState.Done(userTranscript, aiPartial)
                }

                "error" -> {
                    val errMsg = json.optJSONObject("error")?.optString("message", "Erro desconhecido") ?: "Erro"
                    Log.e(TAG, "server_error: $errMsg")
                    stopAudioCapture()
                    _uiState.value = RealtimeUiState.Error("Erro da API: $errMsg", offerFallback = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse server event: ${e.message}")
        }
    }

    private fun startAudioCapture(ws: WebSocket) {
        audioJob = viewModelScope.launch {
            audioManager.startCapture { base64Chunk ->
                val msg = JSONObject().apply {
                    put("type", "input_audio_buffer.append")
                    put("audio", base64Chunk)
                }
                ws.send(msg.toString())
            }
        }
    }

    private fun stopAudioCapture() {
        audioManager.stopCapture()
        audioJob?.cancel()
        audioJob = null
    }

    fun interrupt() {
        webSocket?.send(JSONObject().put("type", "response.cancel").toString())
        stopAudioCapture()
        webSocket?.close(1000, "user_interrupted")
        webSocket = null
        _uiState.value = RealtimeUiState.Idle
    }

    fun reset() {
        interrupt()
        userTranscript = ""
        aiPartial = ""
        firstTranscriptAt = 0L
        firstResponseAt = 0L
        _uiState.value = RealtimeUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioCapture()
        webSocket?.close(1000, "viewmodel_cleared")
        okHttpClient.dispatcher.executorService.shutdown()
    }
}
