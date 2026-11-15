package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.local.ConversationEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.ChatQivoSidebarContent
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeleteConversationDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PromptStudioScreen
import com.example.ui.screens.RenameConversationDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoLine
import com.example.ui.theme.ChatQivoMintSurface
import com.example.ui.theme.ChatQivoSubtle
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoWhite
import com.example.ui.theme.ManropeFontFamily
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            com.example.ui.theme.ChatQivoTheme {
                ChatQivoApp()
            }
        }
    }
}

@Composable
fun ChatQivoApp(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val allConversations by viewModel.allConversations.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showSettings by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationEntity?>(null) }

    // Text-to-Speech state
    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.getDefault()
                instance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        speakingMessageId = null
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speakingMessageId = null
                    }
                })
            }
        }
        instance
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Speech-to-Text state
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember {
        runCatching {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        }.getOrNull()
    }
    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(context, "Didn't catch that. Please try speaking again.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    val combined = if (uiState.inputText.isBlank()) spoken else "${uiState.inputText} $spoken"
                    viewModel.onInputChanged(combined)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            runCatching { speechRecognizer?.destroy() }
        }
    }

    fun startSpeechCapture() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Voice input is not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isListening) {
            runCatching { speechRecognizer.stopListening() }
            isListening = false
        } else {
            runCatching {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer.startListening(intent)
            }.onFailure {
                isListening = false
                Toast.makeText(context, "Unable to start voice recognition.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechCapture()
        } else {
            Toast.makeText(context, context.getString(R.string.error_mic_permission), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoiceClick() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startSpeechCapture()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(context, uri)
        }
    }

    fun handleShare(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    }

    fun handleSpeakToggle(messageId: String, text: String) {
        if (speakingMessageId == messageId) {
            tts.stop()
            speakingMessageId = null
        } else {
            speakingMessageId = messageId
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
        }
    }

    val activeConversation = allConversations.find { it.id == uiState.activeConversationId }
    val topTitle = when {
        uiState.showPromptStudio -> stringResource(R.string.nav_projects)
        activeConversation != null -> activeConversation.title
        else -> stringResource(R.string.new_chat)
    }

    if (drawerState.isOpen || uiState.activeConversationId != null || uiState.showPromptStudio) {
        BackHandler {
            if (drawerState.isOpen) {
                scope.launch { drawerState.close() }
            } else {
                viewModel.startNewChat()
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usePermanentDrawer = maxWidth >= 840.dp

        val sidebarContent: @Composable () -> Unit = {
            ChatQivoSidebarContent(
                conversations = conversations,
                activeConversationId = uiState.activeConversationId,
                searchQuery = uiState.searchQuery,
                isHomeActive = uiState.activeConversationId == null && !uiState.showPromptStudio,
                isPromptStudioActive = uiState.showPromptStudio,
                onSearchChange = viewModel::onSearchQueryChanged,
                onBrandClick = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onHomeClick = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                },
                onPromptStudioClick = {
                    viewModel.openPromptStudio()
                    scope.launch { drawerState.close() }
                },
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onRenameConversation = { renameTarget = it },
                onDeleteConversation = { deleteTarget = it },
                onSettingsClick = {
                    showSettings = true
                    scope.launch { drawerState.close() }
                }
            )
        }

        val mainContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ChatQivoWhite)
            ) {
                ChatQivoTopBar(
                    title = topTitle,
                    showMenuButton = !usePermanentDrawer,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onShareClick = { handleShare(viewModel.buildShareTranscript()) },
                    onSettingsClick = { showSettings = true }
                )

                when {
                    uiState.showPromptStudio -> {
                        PromptStudioScreen(
                            templates = viewModel.promptTemplates,
                            onSelectTemplate = { prompt ->
                                viewModel.applySuggestion(prompt, sendImmediately = false)
                            }
                        )
                    }
                    uiState.activeConversationId != null -> {
                        ChatScreen(
                            messages = messages,
                            inputText = uiState.inputText,
                            pendingAttachment = uiState.pendingAttachment,
                            isStreaming = uiState.isStreaming,
                            isWaitingFirstToken = uiState.isWaitingFirstToken,
                            isListening = isListening,
                            streamingMessageId = uiState.streamingMessageId,
                            streamingText = uiState.streamingText,
                            errorMessage = uiState.errorMessage,
                            speakingMessageId = speakingMessageId,
                            onInputChange = viewModel::onInputChanged,
                            onAttachClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveAttachment = viewModel::clearAttachment,
                            onVoiceClick = ::handleVoiceClick,
                            onSendClick = { viewModel.sendMessage() },
                            onStopClick = viewModel::stopGeneration,
                            onRegenerateClick = viewModel::regenerateLastResponse,
                            onShareText = ::handleShare,
                            onSpeakToggle = ::handleSpeakToggle,
                            onDismissError = viewModel::dismissError
                        )
                    }
                    else -> {
                        HomeScreen(
                            inputText = uiState.inputText,
                            pendingAttachment = uiState.pendingAttachment,
                            isStreaming = uiState.isStreaming,
                            isListening = isListening,
                            errorMessage = uiState.errorMessage,
                            onInputChange = viewModel::onInputChanged,
                            onAttachClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveAttachment = viewModel::clearAttachment,
                            onVoiceClick = ::handleVoiceClick,
                            onSendClick = { viewModel.sendMessage() },
                            onStopClick = viewModel::stopGeneration,
                            onSuggestionClick = { prompt ->
                                viewModel.applySuggestion(prompt, sendImmediately = false)
                            },
                            onDismissError = viewModel::dismissError
                        )
                    }
                }
            }
        }

        if (usePermanentDrawer) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = ChatQivoWhite,
                        modifier = Modifier.width(276.dp)
                    ) {
                        sidebarContent()
                    }
                },
                content = mainContent
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = ChatQivoWhite,
                        modifier = Modifier.width(276.dp)
                    ) {
                        sidebarContent()
                    }
                },
                content = mainContent
            )
        }
    }

    renameTarget?.let { target ->
        RenameConversationDialog(
            conversation = target,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                viewModel.renameConversation(target.id, newTitle)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        DeleteConversationDialog(
            conversation = target,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteConversation(target.id)
                deleteTarget = null
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            conversationCount = allConversations.size,
            onClearAll = viewModel::clearAllConversations,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun ChatQivoTopBar(
    title: String,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatQivoWhite)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                            tint = ChatQivoInk
                        )
                    }
                }
                Text(
                    text = title,
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ChatQivoInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ChatQivoMintSurface
                ) {
                    Text(
                        text = stringResource(R.string.badge_ai),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatQivoTeal,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(9.dp),
                    color = ChatQivoWhite,
                    border = BorderStroke(1.dp, ChatQivoLine),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("top_share_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = ChatQivoSubtle,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.share),
                            fontSize = 12.sp,
                            color = ChatQivoSubtle
                        )
                    }
                }
                Surface(
                    onClick = onSettingsClick,
                    shape = RoundedCornerShape(9.dp),
                    color = ChatQivoWhite,
                    border = BorderStroke(1.dp, ChatQivoLine),
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("top_settings_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = ChatQivoSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = ChatQivoLine)
    }
}
