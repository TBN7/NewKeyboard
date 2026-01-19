package com.example.keyboardnew.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keyboardnew.emotionAssist.LlmViewModel
import com.example.keyboardnew.model.BottomRowKeys
import com.example.keyboardnew.model.EmojiBottomRowKeys
import com.example.keyboardnew.model.Emotion
import com.example.keyboardnew.model.Key
import com.example.keyboardnew.model.KeyboardLanguageConfig
import com.example.keyboardnew.model.KeyboardLanguageManager
import com.example.keyboardnew.model.symbolKeys

sealed class KeyboardLayoutType {
    data object Alphabet : KeyboardLayoutType()
    data object Symbol : KeyboardLayoutType()
    data object Emoji : KeyboardLayoutType()
    data object EmotionAssist : KeyboardLayoutType()
}

@Composable
fun KeyboardLayout(
    languageManager: KeyboardLanguageManager,
    currentInput: String,
    currentEmotion: Emotion,
    llmViewModel: LlmViewModel = viewModel(),
    suggestions: List<String>,
    emojiSuggestions: List<String>,
    isShiftEnabled: Boolean,
    replyOptions: List<String>,
    onKeyPress: (Key) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onReplyOptionClick: (String) -> Unit,
    onEmojiClick: (String) -> Unit,
    onTextApply: (String) -> Unit
) {
    val currentLanguage = languageManager.currentLanguage
    var currentLayoutType by remember {
        mutableStateOf<KeyboardLayoutType>(KeyboardLayoutType.Alphabet)
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        if (currentLayoutType != KeyboardLayoutType.EmotionAssist) {
            SuggestionsBar(
                suggestions = suggestions,
                replyOptions = replyOptions,
                emojis = emojiSuggestions,
                onEmojiClick = onEmojiClick,
                onSuggestionClick = onSuggestionClick,
                onReplyOptionClick = onReplyOptionClick,
                onEmotionAssistClick = {
                    currentLayoutType = KeyboardLayoutType.EmotionAssist
                }
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
            is KeyboardLayoutType.EmotionAssist ->
                EmotionAssistLayout(
                    currentInput = currentInput,
                    currentEmotion = currentEmotion,
                    llmViewModel = llmViewModel,
                    onTextApply = onTextApply,
                    onBackToKeyboardPressed = {
                        currentLayoutType = KeyboardLayoutType.Alphabet
                    }
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
            is KeyboardLayoutType.Emoji ->
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
            else -> { /* No bottom row for Emotion Assist */}
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
fun SuggestionsBar(
    modifier: Modifier = Modifier,
    suggestions: List<String>,
    replyOptions: List<String>,
    emojis: List<String>,
    onEmojiClick: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onReplyOptionClick: (String) -> Unit,
    onEmotionAssistClick: () -> Unit
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
//        IconButton(
//            modifier = modifier
//                .size(48.dp)
//                .padding(8.dp),
//            onClick = onEmotionAssistClick
//        ) {
//            Image(painter = painterResource(R.drawable.magic_wand), "")
//        }
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(4.dp)
                            .clickable {
                                onSuggestionClick(suggestion)
                            }
                    )
                }
            }
        } else if (replyOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))


            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally) ) {
                replyOptions.forEach { replyOption ->
                    Card(
                        modifier = modifier
                            .padding(2.dp)
                            .height(36.dp)
                            .heightIn(76.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        onClick = {
                            onReplyOptionClick(replyOption)
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = replyOption,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}