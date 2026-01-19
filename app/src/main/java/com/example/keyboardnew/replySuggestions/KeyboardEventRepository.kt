package com.example.keyboardnew.replySuggestions

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.keyboardnew.logging.LoggingConstants
import com.example.keyboardnew.logging.LoggingConstants.ACTION_KEYBOARD_EVENT
import org.json.JSONObject

class KeyboardEventRepository(
    private val context: Context
) {
    fun sentWordSuggestionClick(suggestion: String) {
        sendEvent(
            eventType = LoggingConstants.WORD_SUGGESTION_CLICK,
            data = JSONObject().apply {
                put("suggestion_text", suggestion)
            }
        )
    }

    fun sentReplyOptionClick(replyOption: String) {
        sendEvent(
            eventType = LoggingConstants.REPLY_OPTION_CLICK,
            data = JSONObject().apply {
                put("reply_option", replyOption)
            }
        )
    }

    fun sentEmojiSuggestionClick(emoji: String) {
        sendEvent(
            eventType = LoggingConstants.EMOJI_SUGGESTION_CLICK,
            data = JSONObject().apply {
                put("emoji", emoji)
            }
        )
    }

    fun sendKeyPress(character: String) {
        sendEvent(
            eventType = LoggingConstants.KEY_PRESS,
            data = JSONObject().apply {
                put("character", character)
            }
        )
    }

    fun sendBackspacePress() {
        sendEvent(
            eventType = LoggingConstants.BACKSPACE_PRESS
        )
    }

    fun sendEvent(
        eventType: String,
        data: JSONObject? = null
    ) {
        try {
            val intent = Intent(ACTION_KEYBOARD_EVENT).apply {
                putExtra(LoggingConstants.EVENT_TYPE, eventType)
                putExtra(LoggingConstants.EVENT_TIMESTAMP, System.currentTimeMillis())
                data?.let {
                    putExtra(LoggingConstants.EVENT_DATA, it.toString())
                }
                setPackage("kz.project.minimessenger")
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.d("taaag", e.toString())
        }
    }
}