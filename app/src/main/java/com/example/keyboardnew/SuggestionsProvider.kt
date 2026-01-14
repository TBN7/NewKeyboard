package com.example.keyboardnew

import com.example.keyboardnew.model.Emotion

object SuggestionsProvider {

    private val emojiSuggestions = mapOf(
        Emotion.HAPPY to listOf("🙂", "😊", "😄", "😆", "🤩"),
        Emotion.SAD to listOf("🙁", "😟", "😢", "😫", "😭"),
        Emotion.SURPRISED to listOf("😯", "😮", "😲", "🤯", "😱"),
        Emotion.ANGRY to listOf("😠", "😡", "🤬", "😤", "👿"),
        Emotion.NEUTRAL to listOf("😐", "😑", "😶", "😴", "🤔")
    )

    fun getEmojiForEmotion(emotion: Emotion): List<String> =
        emojiSuggestions[emotion] ?: emptyList()
}