package com.example.keyboardnew.suggestions

import android.content.Context
import android.util.Log
import com.example.keyboardnew.model.Emotion
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SuggestionsProvider(
    private val context: Context
) {
    private val emojiSuggestions = mapOf(
        Emotion.HAPPY to listOf("🙂", "😊", "😄", "😆", "🤩"),
        Emotion.SAD to listOf("🙁", "😟", "😢", "😫", "😭"),
        Emotion.SURPRISED to listOf("😯", "😮", "😲", "🤯", "😱"),
        Emotion.ANGRY to listOf("😠", "😡", "🤬", "😤", "👿"),
        Emotion.NEUTRAL to listOf("😐", "😑", "😶", "😴", "🤔")
    )

    private val bigrams = mutableMapOf<String, Map<String, Int>>()
    private val gson = Gson()

    suspend fun loadDictionary() {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context
                    .assets
                    .open("dictionary.json")
                    .bufferedReader()
                    .use { it.readText() }

                bigrams.putAll(gson.fromJson(jsonString, BigramData::class.java).bigrams)

            } catch (e: Exception) {
                Log.d("SuggestionsProvider", e.toString())
            }
        }
    }

    fun getSuggestions(previousWord: String, limit: Int = 3): List<String> =
        bigrams[previousWord.lowercase()]
            ?.entries
            ?.sortedByDescending { it.value }
            ?.take(limit)
            ?.map { it.key }
            ?: emptyList()

    fun getEmojiForEmotion(emotion: Emotion): List<String> =
        emojiSuggestions[emotion] ?: emptyList()
}