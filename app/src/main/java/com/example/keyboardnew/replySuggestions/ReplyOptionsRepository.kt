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


private const val ACTION_SEND_REPLY_OPTIONS = "kz.project.minimessenger.SEND_REPLY_OPTIONS"
private const val ACTION_EMOTION_RECEIVED = "kz.project.minimessenger.EMOTION_RECEIVED"

class ReplyOptionsRepository(
    private val context: Context
) {
    private val _replyOptions = MutableStateFlow<List<String>>(emptyList())
    val replyOptions: StateFlow<List<String>> = _replyOptions

    private val _emotion = MutableStateFlow<Emotion?>(null)
    val emotion: StateFlow<Emotion?> = _emotion

    private var broadcastReceiver: BroadcastReceiver? = null

    fun startListening() {
        if (broadcastReceiver != null) {
            return
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_SEND_REPLY_OPTIONS) {
                    _replyOptions.value = intent.getStringArrayListExtra("replyOptions") ?: emptyList()
                }
                if (intent.action == ACTION_EMOTION_RECEIVED) {
                    _emotion.value = Emotion.valueOf(intent.getStringExtra("emotion")
                        ?: Emotion.NEUTRAL.name)
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_SEND_REPLY_OPTIONS)
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