package com.example.keyboardnew.emotionAssist

import android.content.Context
import android.util.Log
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "LlmViewModel"

class LlmViewModel : ViewModel() {

    private var llmInference: LlmInference? = null
    private var inferenceJob: Job? = null

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _benchmarks = MutableStateFlow<BenchmarkData?>(null)
    val benchmarks: StateFlow<BenchmarkData?> = _benchmarks

    private val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setTopK(4)
        .setTemperature(0f)
        .build()

    fun initModel(
        context: Context
    ) {
        if (llmInference != null) return

        val startTime = SystemClock.elapsedRealtime()
        val memoryBefore = getMemoryMb()


        viewModelScope.launch {
            val path = prepareModelFromAssets(
                context, "gemma-3-1b.task"
            )

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(512)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            val initTime = SystemClock.elapsedRealtime() - startTime
            val memoryAfter = getMemoryMb()

            _benchmarks.value = _benchmarks.value?.copy(initTimeMs = initTime)
                ?: BenchmarkData(
                    initTimeMs = initTime,
                    memoryBeforeMb = memoryBefore,
                    memoryAfterMb = memoryAfter
                )

            Log.d(TAG, "Model is initialized")
        }
    }

    private suspend fun prepareModelFromAssets(
        context: Context,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val outputFile = File(context.filesDir, fileName)

        if (!outputFile.exists()) {
            context.assets.open(fileName).use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        outputFile.absolutePath
    }

    fun generateResponse(
        prompt: String
    ) {
        Log.d(TAG, prompt)
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var session: LlmInferenceSession?
            val startTime = SystemClock.elapsedRealtime()

            try {
                session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
                session.addQueryChunk(prompt)
                val response = session.generateResponse()
                val totalTimeMs = SystemClock.elapsedRealtime() - startTime

                Log.d(TAG, "Full response in $totalTimeMs ms")

                if (!response.isNullOrEmpty()) {
                    Log.d(TAG, response)
                    _response.value = response
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.d(TAG, e.toString())
            }
        }
    }
    fun generateResponseAsync(
        prompt: String
    ) {
        Log.d(TAG, prompt)
        _isLoading.value = true

        val memoryBefore = getMemoryMb()
        val startTime = SystemClock.elapsedRealtime()
        val promptLength = prompt.length

        viewModelScope.launch(Dispatchers.IO) {
            var session: LlmInferenceSession? = null

            var memoryPeak = memoryBefore
            var firstTokenTimeMs = 0L
            var totalTokens = 0
            val responseBuilder = StringBuilder()
            var isCompleted = false

            try {
                val inputTokens = llmInference?.sizeInTokens(prompt) ?: 0

                session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
                session.addQueryChunk(prompt)
                session.generateResponseAsync { partialResult, done ->
                    val token = partialResult ?: ""
                    totalTokens += token.length
                    responseBuilder.append(token)

                    if (firstTokenTimeMs == 0L && token.isNotEmpty()) {
                        firstTokenTimeMs = SystemClock.elapsedRealtime() - startTime
                    }

                    memoryPeak = maxOf(memoryPeak, getMemoryMb())

                    if (done) {
                        isCompleted = true
                        val totalTimeMs = SystemClock.elapsedRealtime() - startTime
                        val tokensPerSecond = if (totalTimeMs > 0) totalTokens.toDouble() / (totalTimeMs / 1000.0) else 0.0

                        val benchmark = BenchmarkData(
                            promptLength = promptLength,
                            responseLength = responseBuilder.length,
                            totalTimeMs = totalTimeMs,
                            tokensPerSecond = tokensPerSecond,
                            memoryBeforeMb = memoryBefore,
                            memoryAfterMb = getMemoryMb(),
                            memoryPeakMb = memoryPeak,
                            initTimeMs = _benchmarks.value?.initTimeMs ?: 0,
                            ttftMs = firstTokenTimeMs,
                            totalInputTokens = inputTokens
                        )

                        _benchmarks.value = benchmark

                        Log.d("taaag", responseBuilder.toString())
                    }
                }
            }finally {
                if (isActive) {
                    _isLoading.value = false
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        llmInference?.close()
    }

    private fun getMemoryMb(): Double {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / (1024 * 1024)
        return used
    }

}

fun String.cleanLlmJsonResponse(): String {
    var cleaned = trim()

    if (cleaned.startsWith("```")) {
        cleaned = cleaned.removePrefix("```json")
    }
    if (cleaned.startsWith("```")) {
        cleaned = cleaned.removePrefix("```")
    }
    if (cleaned.endsWith("```")) {
        cleaned = cleaned.removeSuffix("```")
    }

    return cleaned
}

data class BenchmarkData(
    val initTimeMs: Long = 0,
    val promptLength: Int = 0,
    val ttftMs: Long = 0,
    val responseLength: Int = 0,
    val totalTimeMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
    val memoryBeforeMb: Double = 0.0,
    val memoryAfterMb: Double = 0.0,
    val memoryPeakMb: Double = 0.0,
    val totalInputTokens: Int = 0
)