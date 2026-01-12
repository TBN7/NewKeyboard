package com.example.keyboardnew.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import com.example.keyboardnew.model.BottomRowKeys
import com.example.keyboardnew.model.EmojiBottomRowKeys
import com.example.keyboardnew.model.Key
import com.example.keyboardnew.model.KeyboardLanguageConfig
import com.example.keyboardnew.model.KeyboardLanguageManager
import com.example.keyboardnew.model.symbolKeys

sealed class KeyboardLayoutType {
    data object Alphabet : KeyboardLayoutType()
    data object Symbol : KeyboardLayoutType()
    data object Emoji : KeyboardLayoutType()
}

@Composable
fun KeyboardLayout(
    languageManager: KeyboardLanguageManager,
    emojiSuggestions: List<String>,
    isShiftEnabled: Boolean,
    onKeyPress: (Key) -> Unit,
    onEmojiClick: (String) -> Unit
) {
    val currentLanguage = languageManager.currentLanguage
    var currentLayoutType by remember {
        mutableStateOf<KeyboardLayoutType>(KeyboardLayoutType.Alphabet)
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        if (emojiSuggestions.isNotEmpty()) {
            EmojiBar(
                emojis = emojiSuggestions,
                onEmojiClick = onEmojiClick
            )
        }
        when(currentLayoutType) {
            is KeyboardLayoutType.Alphabet ->
                AlphabetRows(
                    language = currentLanguage,
                    isShiftEnabled = isShiftEnabled,
                    onKeyPress = onKeyPress
                )
            is KeyboardLayoutType.Emoji ->
                EmojiRows(
                    onKeyPress = onKeyPress
                )
            is KeyboardLayoutType.Symbol ->
                SymbolRows(
                    onKeyPress = onKeyPress
                )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (currentLayoutType) {
            is KeyboardLayoutType.Symbol, KeyboardLayoutType.Alphabet ->
                BottomRow(
                    languageManager = languageManager,
                    currentKeyboardLayoutType = currentLayoutType,
                    onKeyPress = { key ->
                        when (key) {
                            is Key.NumberToggle -> currentLayoutType = if (currentLayoutType is KeyboardLayoutType.Alphabet)
                                KeyboardLayoutType.Symbol
                            else
                                KeyboardLayoutType.Alphabet

                            is Key.EmojiToggle -> currentLayoutType = if (currentLayoutType is KeyboardLayoutType.Alphabet)
                                KeyboardLayoutType.Emoji
                            else
                                KeyboardLayoutType.Alphabet

                            else -> onKeyPress(key)
                        }
                    }
                )
            else ->
                EmojiBottomRow(
                    currentKeyboardLayoutType = currentLayoutType,
                    onKeyPress = { key ->
                        when (key) {
                            is Key.NumberToggle -> currentLayoutType = if (currentLayoutType is KeyboardLayoutType.Alphabet)
                                KeyboardLayoutType.Symbol
                            else
                                KeyboardLayoutType.Alphabet

                            else -> onKeyPress(key)
                        }
                    }
                )
        }
    }
}

@Composable
fun AlphabetRows(
    language: KeyboardLanguageConfig,
    isShiftEnabled: Boolean,
    onKeyPress: (Key) -> Unit
) {
    val rowsToUse = if (isShiftEnabled) language.shiftedRows else language.rows

    rowsToUse.forEach { row ->
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { key ->
                KeyButton(
                    modifier = Modifier.weight(1f),
                    key = key,
                    isShiftEnabled = isShiftEnabled,
                    onKeyPress = onKeyPress
                )
            }
        }
    }
}

@Composable
fun SymbolRows(
    onKeyPress: (Key) -> Unit
) {
    symbolKeys.forEach { row ->
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { key ->
                KeyButton(
                    modifier = Modifier.weight(1f),
                    key = key,
                    onKeyPress = onKeyPress
                )
            }
        }
    }
}

@Composable
fun BottomRow(
    languageManager: KeyboardLanguageManager,
    currentKeyboardLayoutType: KeyboardLayoutType,
    onKeyPress: (Key) -> Unit
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomRowKeys.forEach { key ->
            KeyButton(
                modifier = Modifier.weight(
                    if (key is Key.Space) 4f else 1f
                ),
                key = key,
                currentKeyboardLayoutType = currentKeyboardLayoutType,
                onKeyPress = {
                    if (it is Key.LanguageToggle) {
                        languageManager.switchToNextLanguage()
                    } else {
                        onKeyPress(it)
                    }
                }
            )
        }
    }
}

@Composable
fun EmojiBottomRow(
    currentKeyboardLayoutType: KeyboardLayoutType,
    onKeyPress: (Key) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EmojiBottomRowKeys.forEach { key ->
            KeyButton(
                modifier = Modifier.weight(
                    if (key is Key.Space) 4f else 1f
                ),
                key = key,
                currentKeyboardLayoutType = currentKeyboardLayoutType,
                onKeyPress = onKeyPress
            )
        }
    }
}

@Composable
fun EmojiRows(
    modifier: Modifier = Modifier,
    onKeyPress: (Key) -> Unit
) {
    Column (modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth()
                .height(300.dp),
            factory = { context ->
                EmojiPickerView(context).apply {
                    setOnEmojiPickedListener { emoji ->
                        onKeyPress(Key.Character(emoji.emoji))
                    }
                }
            }
        )
    }
}

@Composable
fun EmojiBar(
    modifier: Modifier = Modifier,
    emojis: List<String>,
    onEmojiClick: (String) -> Unit
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        emojis.forEach { emoji ->
            IconButton(
                modifier = modifier
                    .size(48.dp)
                    .padding(8.dp),
                onClick = { onEmojiClick(emoji) }
            ) {
                Text(
                    text = emoji,
                    fontSize = 28.sp
                )
            }
        }
    }
}