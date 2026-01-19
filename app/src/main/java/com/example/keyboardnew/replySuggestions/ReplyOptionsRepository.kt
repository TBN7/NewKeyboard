package com.example.keyboardnew.replySuggestions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.keyboardnew.model.Emotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val ACTION_MESSAGE_RECEIVED = "kz.project.minimessenger.MESSAGE_RECEIVED"

private const val ACTION_EMOTION_RECEIVED = "kz.project.minimessenger.EMOTION_RECEIVED"

/**
 * messages: [
 *  "INCOMING: Hello"
 *  "OUTGOING: Hi!"
 * ]
 */
data class MessagesWithEmotion(
    val messages: List<String> = emptyList(),
    val emotion: Emotion = Emotion.NEUTRAL
)

class ReplyOptionsRepository(
    private val context: Context
) {
    private val _messagesWithEmotion = MutableStateFlow<MessagesWithEmotion?>(null)
    val messagesWithEmotion: StateFlow<MessagesWithEmotion?> = _messagesWithEmotion

    private val _emotion = MutableStateFlow<Emotion?>(null)
    val emotion: StateFlow<Emotion?> = _emotion

    private var broadcastReceiver: BroadcastReceiver? = null

    fun startListening() {
        if (broadcastReceiver != null) {
            return
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_MESSAGE_RECEIVED) {
                    _messagesWithEmotion.value = MessagesWithEmotion(
                        messages = intent.getStringArrayListExtra("messages") ?: emptyList(),
                        emotion = Emotion.valueOf(intent.getStringExtra("emotion") ?: Emotion.NEUTRAL.name)
                    )
                }
                if (intent.action == ACTION_EMOTION_RECEIVED) {
                    _emotion.value = Emotion.valueOf(intent.getStringExtra("emotion")
                        ?: Emotion.NEUTRAL.name)
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_MESSAGE_RECEIVED)
            addAction(ACTION_EMOTION_RECEIVED)
        }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }
}