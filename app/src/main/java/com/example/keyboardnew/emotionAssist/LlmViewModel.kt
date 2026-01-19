package com.example.keyboardnew.emotionAssist

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class LlmViewModel : ViewModel() {

    private var llmInference: LlmInference? = null
    private var inferenceJob: Job? = null

    private var activeInference = AtomicReference<Job?>(null)

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun initModel(
        context: Context
    ) {
        if (llmInference != null) return

        viewModelScope.launch {
            val path = prepareModelFromAssets(
                context, "gemma.task"
            )

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(512)
                .setMaxTopK(32)
//                .setPreferredBackend(LlmInference.Backend.GPU)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            Log.d("taaag", "Model is initialized")
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
        Log.d("taaag", prompt)
        _isLoading.value = true
        val previousJob = activeInference.getAndSet(null)
//        previousJob?.cancel()

        val newJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("taaag", "Inside new job ${previousJob?.isCancelled}")
                val result = llmInference?.generateResponse(prompt)
                Log.d("taaag", result.toString())
                if (result != null && isActive) {
                    _response.value = result.cleanLlmJsonResponse()
                }
            } finally {
                if (isActive) {
                    _isLoading.value = false
                }
            }
        }

        activeInference.set(newJob)
    }


    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        llmInference?.close()

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