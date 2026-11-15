package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatQivoAiService
import com.example.data.ChatRepository
import com.example.data.local.ChatDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "ChatViewModel"

data class PendingAttachment(
    val uri: Uri,
    val name: String,
    val previewBitmap: Bitmap?
)

data class PromptTemplate(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val category: String
)

data class ChatUiState(
    val activeConversationId: String? = null,
    val searchQuery: String = "",
    val inputText: String = "",
    val pendingAttachment: PendingAttachment? = null,
    val isStreaming: Boolean = false,
    val isWaitingFirstToken: Boolean = false,
    val streamingMessageId: String? = null,
    val streamingText: String = "",
    val errorMessage: String? = null,
    val showPromptStudio: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(
        dao = ChatDatabase.getInstance(application).chatDao(),
        aiService = ChatQivoAiService()
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> =
        combine(
            repository.observeConversations(),
            _uiState
        ) { list, state ->
            val q = state.searchQuery.trim()
            if (q.isEmpty()) list
            else list.filter { it.title.contains(q, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<ConversationEntity>> =
        repository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMessages: StateFlow<List<MessageEntity>> =
        _uiState
            .flatMapLatest { state ->
                val id = state.activeConversationId
                if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val promptTemplates: List<PromptTemplate> = listOf(
        PromptTemplate(
            title = "Write something",
            subtitle = "Ideas, drafts and better words",
            prompt = "Help me write something clear, engaging, and well-structured.",
            category = "Writing"
        ),
        PromptTemplate(
            title = "Understand a topic",
            subtitle = "Learn with simple explanations",
            prompt = "Explain a complex topic to me step by step in simple language.",
            category = "Learning"
        ),
        PromptTemplate(
            title = "Build a project",
            subtitle = "Turn ideas into real things",
            prompt = "Help me plan and build a clean modern website using HTML, CSS, and JavaScript.",
            category = "Coding"
        ),
        PromptTemplate(
            title = "Explore an idea",
            subtitle = "Think, research and discover",
            prompt = "Help me brainstorm creative ideas and an actionable plan for a new project.",
            category = "Ideas"
        ),
        PromptTemplate(
            title = "Urdu & Multilingual",
            subtitle = "Write or translate in Urdu, Arabic, Hindi",
            prompt = "براہِ کرم اس موضوع پر سادہ اور واضح اردو میں رہنمائی کریں۔",
            category = "Multilingual"
        ),
        PromptTemplate(
            title = "AI Prompt Engineer",
            subtitle = "Craft a detailed professional AI prompt",
            prompt = "Create a structured, high-performing AI prompt with role, context, constraints, and output format for:",
            category = "Prompts"
        )
    )

    private var activeStreamJob: Job? = null

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun startNewChat() {
        stopGeneration()
        _uiState.update {
            it.copy(
                activeConversationId = null,
                inputText = "",
                pendingAttachment = null,
                errorMessage = null,
                showPromptStudio = false
            )
        }
    }

    fun openPromptStudio() {
        _uiState.update { it.copy(showPromptStudio = true, activeConversationId = null) }
    }

    fun selectConversation(conversationId: String) {
        stopGeneration()
        _uiState.update {
            it.copy(
                activeConversationId = conversationId,
                errorMessage = null,
                showPromptStudio = false
            )
        }
    }

    fun applySuggestion(text: String, sendImmediately: Boolean = false) {
        _uiState.update { it.copy(inputText = text, showPromptStudio = false, errorMessage = null) }
        if (sendImmediately) {
            sendMessage()
        }
    }

    fun attachImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeScaledBitmap(context, uri)
            }
            if (bitmap != null) {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "image.jpg"
                _uiState.update {
                    it.copy(
                        pendingAttachment = PendingAttachment(uri, name, bitmap),
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Could not load the selected image. Please choose another file.")
                }
            }
        }
    }

    fun clearAttachment() {
        _uiState.update { it.copy(pendingAttachment = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage(overrideText: String? = null) {
        val currentState = _uiState.value
        if (currentState.isStreaming) return

        val prompt = (overrideText ?: currentState.inputText).trim()
        val attachment = currentState.pendingAttachment
        if (prompt.isBlank() && attachment == null) {
            _uiState.update { it.copy(errorMessage = "Please enter a message before sending.") }
            return
        }

        _uiState.update {
            it.copy(
                inputText = if (overrideText == null) "" else it.inputText,
                pendingAttachment = null,
                errorMessage = null,
                showPromptStudio = false
            )
        }

        viewModelScope.launch {
            val existingId = currentState.activeConversationId
            val convo = if (existingId == null) {
                val titleSeed = prompt.ifBlank { attachment?.name ?: "Image analysis" }
                repository.createConversation(titleSeed).also { created ->
                    _uiState.update { it.copy(activeConversationId = created.id) }
                }
            } else {
                allConversations.value.find { it.id == existingId }?.also {
                    repository.touchConversation(it)
                } ?: repository.createConversation(prompt.ifBlank { "Conversation" }).also { created ->
                    _uiState.update { it.copy(activeConversationId = created.id) }
                }
            }

            val historyBeforeSend = repository.getMessagesOnce(convo.id)

            repository.addMessage(
                conversationId = convo.id,
                role = "user",
                content = prompt,
                attachmentUri = attachment?.uri?.toString(),
                attachmentName = attachment?.name
            )

            streamIntoConversation(
                conversationId = convo.id,
                history = historyBeforeSend,
                prompt = prompt,
                bitmap = attachment?.previewBitmap
            )
        }
    }

    fun regenerateLastResponse() {
        val currentState = _uiState.value
        val convoId = currentState.activeConversationId ?: return
        if (currentState.isStreaming) return

        viewModelScope.launch {
            val messages = repository.getMessagesOnce(convoId)
            val lastUserIndex = messages.indexOfLast { it.role == "user" }
            if (lastUserIndex == -1) return@launch

            val lastUserMessage = messages[lastUserIndex]
            // Remove any trailing model/error messages after the last user message
            messages.drop(lastUserIndex + 1).forEach { trailing ->
                repository.deleteMessage(trailing.id)
            }

            val historyBeforeUser = messages.take(lastUserIndex)
            val bitmap = lastUserMessage.attachmentUri?.let { uriStr ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        decodeScaledBitmap(getApplication(), Uri.parse(uriStr))
                    }.getOrNull()
                }
            }

            streamIntoConversation(
                conversationId = convoId,
                history = historyBeforeUser,
                prompt = lastUserMessage.content,
                bitmap = bitmap
            )
        }
    }

    private fun streamIntoConversation(
        conversationId: String,
        history: List<MessageEntity>,
        prompt: String,
        bitmap: Bitmap?
    ) {
        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            val placeholderAiMessage = repository.addMessage(
                conversationId = conversationId,
                role = "model",
                content = ""
            )

            _uiState.update {
                it.copy(
                    isStreaming = true,
                    isWaitingFirstToken = true,
                    streamingMessageId = placeholderAiMessage.id,
                    streamingText = "",
                    errorMessage = null
                )
            }

            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    repository.streamAiReply(history, prompt, bitmap).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            _uiState.update {
                                it.copy(
                                    isWaitingFirstToken = false,
                                    streamingText = accumulated.toString()
                                )
                            }
                        }
                    }
                }

                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    repository.updateMessage(placeholderAiMessage.copy(content = finalText))
                } else {
                    repository.updateMessage(
                        placeholderAiMessage.copy(
                            content = "The AI returned an empty response. Please try again.",
                            isError = true
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI streaming timed out", e)
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    repository.updateMessage(
                        placeholderAiMessage.copy(content = "$partial\n\n*(Response timed out after 60s)*")
                    )
                } else {
                    repository.deleteMessage(placeholderAiMessage.id)
                }
                _uiState.update {
                    it.copy(errorMessage = "Response timed out after 60 seconds. Please try again.")
                }
            } catch (e: CancellationException) {
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    repository.updateMessage(placeholderAiMessage.copy(content = partial))
                } else {
                    repository.deleteMessage(placeholderAiMessage.id)
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI streaming error", e)
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    repository.updateMessage(placeholderAiMessage.copy(content = partial))
                } else {
                    repository.deleteMessage(placeholderAiMessage.id)
                }
                val friendly = mapFriendlyError(e)
                _uiState.update { it.copy(errorMessage = friendly) }
            } finally {
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        isWaitingFirstToken = false,
                        streamingMessageId = null,
                        streamingText = ""
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            if (_uiState.value.activeConversationId == id) {
                stopGeneration()
                _uiState.update { it.copy(activeConversationId = null) }
            }
            repository.deleteConversation(id)
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            stopGeneration()
            repository.clearAll()
            _uiState.update { it.copy(activeConversationId = null) }
        }
    }

    fun buildShareTranscript(): String {
        val convo = allConversations.value.find { it.id == _uiState.value.activeConversationId }
        val msgs = activeMessages.value
        if (msgs.isEmpty()) return "ChatQivo — Think clearly. Create freely."
        val header = "ChatQivo — ${convo?.title ?: "Conversation"}\n\n"
        val body = msgs.joinToString("\n\n") { m ->
            val speaker = if (m.role == "user") "You" else "ChatQivo"
            "$speaker:\n${m.content}"
        }
        return header + body
    }

    private fun mapFriendlyError(e: Exception): String {
        val raw = e.localizedMessage.orEmpty()
        return when {
            raw.contains("403") || raw.contains("AppCheck", ignoreCase = true) ->
                "AI service verification failed (App Check). Please check your Firebase configuration."
            raw.contains("429") || raw.contains("quota", ignoreCase = true) ||
                raw.contains("rate", ignoreCase = true) ->
                "Rate limit reached. Please wait a moment and try again."
            raw.contains("network", ignoreCase = true) || raw.contains("Unable to resolve host", ignoreCase = true) ->
                "Network error. Please check your internet connection and retry."
            raw.isNotBlank() -> raw
            else -> "Something went wrong while generating a response. Please try again."
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = 1280
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap", e)
            null
        }
    }
}
