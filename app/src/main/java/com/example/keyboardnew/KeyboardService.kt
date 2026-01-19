package com.example.keyboardnew

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.keyboardnew.emotionAssist.LlmViewModel
import com.example.keyboardnew.emotionDetection.EmotionDetectorViewModel
import com.example.keyboardnew.model.Emotion
import com.example.keyboardnew.model.Key
import com.example.keyboardnew.model.KeyboardLanguageManager
import com.example.keyboardnew.model.ReplyOptionsResult
import com.example.keyboardnew.model.autocompleteSuggestionPrompt
import com.example.keyboardnew.model.replySuggestionPrompt
import com.example.keyboardnew.replySuggestions.KeyboardEventRepository
import com.example.keyboardnew.replySuggestions.ReplyOptionsRepository
import com.example.keyboardnew.suggestions.SuggestionsProvider
import com.example.keyboardnew.ui.KeyboardLayout
import com.example.keyboardnew.ui.theme.KeyboardNewTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class KeyboardService: InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner, ViewModelStoreOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle: Lifecycle = dispatcher.lifecycle

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry : SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var isShiftEnabled by mutableStateOf(false)
    private var emojiSuggestions by mutableStateOf(emptyList<String>())
    private var replyOptions by mutableStateOf(emptyList<String>())
    private var inputSuggestions by mutableStateOf(emptyList<String>())
    private var currentInput by mutableStateOf("")
    private var currentEmotion by mutableStateOf(Emotion.NEUTRAL)

    private val _currentInput = MutableStateFlow("")
    private val cachedReplyOptions = mutableListOf<String>()

    private lateinit var keyboardLanguageManager: KeyboardLanguageManager
    private lateinit var suggestionsProvider: SuggestionsProvider
    private lateinit var replyOptionsRepository: ReplyOptionsRepository
    private lateinit var keyboardEventRepository: KeyboardEventRepository

    private val emotionDetectorViewModel: EmotionDetectorViewModel by lazy {
        ViewModelProvider(this)[EmotionDetectorViewModel::class]
    }

    private val llmViewModel: LlmViewModel by lazy {
        ViewModelProvider(this)[LlmViewModel::class]
    }


    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store


    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        keyboardLanguageManager = KeyboardLanguageManager(this)
        suggestionsProvider = SuggestionsProvider(this)
        savedStateRegistryController.performRestore(null)

        lifecycleScope.launch {
            suggestionsProvider.loadDictionary()
        }

        updateCurrentEmotion()
        setupInputDebounce()

        //llmViewModel.initModel(applicationContext)

        replyOptionsRepository = ReplyOptionsRepository(applicationContext)
        replyOptionsRepository.startListening()

        keyboardEventRepository = KeyboardEventRepository(applicationContext)

        lifecycleScope.launch {
            replyOptionsRepository.replyOptions.collect { suggestions ->
                replyOptions = suggestions
                cachedReplyOptions.clear()
                cachedReplyOptions.addAll(replyOptions)
            }
        }

        lifecycleScope.launch {
            replyOptionsRepository.emotion.collect { emotion ->
                if (emotion != null) {
                    currentEmotion = emotion
                    updateEmojiSuggestions()
                }
            }
        }

//        lifecycleScope.launch {
//            llmViewModel.response.collect { llmResponse ->
//                Log.d("taaag", llmResponse)
//                if (llmResponse.isEmpty()) {
//                    return@collect
//                }
//                val replyOptionResult = Gson().fromJson(llmResponse, ReplyOptionsResult::class.java)
//                inputSuggestions = listOf(llmResponse)
//            }
//        }
    }

    @CallSuper
    override fun onBindInput() {
        super.onBindInput()
        dispatcher.onServicePreSuperOnBind()
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)

        currentInput = ""
        _currentInput.value = ""
        inputSuggestions = emptyList()
        isShiftEnabled = false
        replyOptions = cachedReplyOptions
    }

    override fun onFinishInput() {
        super.onFinishInput()

        currentInput = ""
        _currentInput.value = ""
        inputSuggestions = emptyList()
        replyOptions = emptyList()
        cachedReplyOptions.clear()
        isShiftEnabled = false
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                KeyboardNewTheme{
                    Column(modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars.add(WindowInsets(bottom = 16.dp)))
                    ) {
//                        CameraLayout()
                        KeyboardLayout(
                            languageManager = keyboardLanguageManager,
                            currentInput = currentInput,
                            currentEmotion = currentEmotion,
                            suggestions = inputSuggestions,
                            replyOptions = replyOptions,
                            emojiSuggestions = emojiSuggestions,
                            isShiftEnabled = isShiftEnabled,
                            onKeyPress = { key ->
                                when(key) {
                                    is Key.Character -> {
                                        keyboardEventRepository.sendKeyPress(key.value)
                                        handleLetterKeyPress(key.value)
                                    }
                                    Key.Shift -> {
                                        keyboardEventRepository.sendKeyPress("SHIFT")
                                        handleShiftPress()
                                    }
                                    Key.Delete -> {
                                        keyboardEventRepository.sendBackspacePress()
                                        handleDelete()
                                    }
                                    Key.Space -> {
                                        keyboardEventRepository.sendKeyPress("SPACE")
                                        handleSpace()
                                    }
                                    else -> { }
                                }
                            },
                            onEmojiClick = { emoji ->
                                keyboardEventRepository.sentEmojiSuggestionClick(emoji)
                                handleSuggestionClick(emoji)
                            },
                            onTextApply = { text ->
                                handleTextApplied(text)
                            },
                            onSuggestionClick = { suggestion ->
                                keyboardEventRepository.sentWordSuggestionClick(suggestion)
                                handleSuggestionClick(suggestion)
                            },
                            onReplyOptionClick = { replyOption ->
                                keyboardEventRepository.sentReplyOptionClick(replyOption)
                                handleSuggestionClick(replyOption)
                            }
                        )
                    }
                }
            }
        }

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
        }

        return composeView

    }

    @OptIn(FlowPreview::class)
    private fun setupInputDebounce() {
        lifecycleScope.launch {
            _currentInput
                .debounce(1000)
                .distinctUntilChanged()
                .collect {
                    updateEmojiSuggestions()
                }
        }
    }


    private fun handleLetterKeyPress(letter: String) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(letter, 1)

        currentInput += letter
        _currentInput.value = currentInput

        if (replyOptions.isNotEmpty()) {
            replyOptions = emptyList()
        }
    }

    private fun handleShiftPress() {
        isShiftEnabled = !isShiftEnabled
    }

    private fun handleDelete() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.deleteSurroundingTextInCodePoints(1, 0)

        Log.d("taaag", currentInput.toString())

        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            _currentInput.value = currentInput
        }

        if (currentInput.isEmpty()) {
            inputSuggestions = emptyList()
            replyOptions = cachedReplyOptions
        }
    }

    private fun handleSpace() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(" ", 1)
        currentInput += " "
        _currentInput.value = currentInput

        updateInputSuggestions()

//        if (lastMessagesWithEmotion != null) {
//            llmViewModel.generateResponse(
//                autocompleteSuggestionPrompt
//                    .replace(
//                        "{{MESSAGES}}", lastMessagesWithEmotion!!.messages.joinToString("\n")
//                    )
//                    .replace("{{NEW_MESSAGE}}", lastMessagesWithEmotion!!.messages.last())
//                    .replace("{{PARTIAL_INPUT}}", currentInput)
//            )
//        }
    }

    private fun handleSuggestionClick(suggestion: String) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText("$suggestion ", 1)
        currentInput += "$suggestion "
        _currentInput.value = currentInput

        updateInputSuggestions()
    }

    private fun handleTextApplied(text: String) {
        val inputConnection = currentInputConnection ?: return
        if (currentInput.isNotEmpty()) {
            inputConnection.deleteSurroundingText(currentInput.length, 0)
        }

        inputConnection.commitText(text, 1)
        currentInput = text
        _currentInput.value = currentInput
    }

    @OptIn(FlowPreview::class)
    private fun updateCurrentEmotion() {
        lifecycleScope.launch {
            emotionDetectorViewModel.detectedEmotion
                .collectLatest { emotion ->
                    currentEmotion = emotion
                }
        }
    }

    private fun updateEmojiSuggestions() {
        emojiSuggestions = suggestionsProvider.getEmojiForEmotion(currentEmotion)
    }

    private fun updateInputSuggestions() {
        val previousWord = currentInput.split(" ").last { it.isNotEmpty() }
        inputSuggestions = suggestionsProvider.getSuggestions(previousWord)
    }

}