package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.MessageEntity
import com.example.ui.PendingAttachment
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.components.PromptComposer
import com.example.ui.components.containsRtlScript
import com.example.ui.theme.ChatQivoErrorBorder
import com.example.ui.theme.ChatQivoErrorInk
import com.example.ui.theme.ChatQivoErrorSurface
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoLine
import com.example.ui.theme.ChatQivoMuted
import com.example.ui.theme.ChatQivoSoft
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoUserBubble
import com.example.ui.theme.ChatQivoUserBubbleBorder
import com.example.ui.theme.ChatQivoUserBubbleInk
import com.example.ui.theme.ChatQivoWhite
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    messages: List<MessageEntity>,
    inputText: String,
    pendingAttachment: PendingAttachment?,
    isStreaming: Boolean,
    isWaitingFirstToken: Boolean,
    isListening: Boolean,
    streamingMessageId: String?,
    streamingText: String,
    errorMessage: String?,
    speakingMessageId: String?,
    onInputChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onShareText: (String) -> Unit,
    onSpeakToggle: (String, String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatQivoWhite)
            .imePadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Today · ChatQivo conversation",
                    fontSize = 11.sp,
                    color = Color(0xFFA3ADA9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )
            }

            itemsIndexed(messages, key = { _, item -> item.id }) { idx, msg ->
                val isCurrentlyStreamingThis = isStreaming && msg.id == streamingMessageId
                val displayedContent = if (isCurrentlyStreamingThis) streamingText else msg.content
                val isLastAi = msg.role == "model" && idx == messages.lastIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 790.dp)
                        .padding(bottom = 22.dp)
                ) {
                    if (msg.role == "user") {
                        UserMessageRow(message = msg)
                    } else {
                        AiMessageRow(
                            messageId = msg.id,
                            content = displayedContent,
                            isWaitingFirstToken = isCurrentlyStreamingThis && isWaitingFirstToken && displayedContent.isBlank(),
                            isStreaming = isCurrentlyStreamingThis,
                            isSpeaking = speakingMessageId == msg.id,
                            showRegenerate = isLastAi && !isStreaming,
                            onRegenerate = onRegenerateClick,
                            onShare = { onShareText(displayedContent) },
                            onSpeakToggle = { onSpeakToggle(msg.id, displayedContent) }
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 790.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ChatQivoErrorSurface,
                            border = BorderStroke(1.dp, ChatQivoErrorBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = ChatQivoErrorInk,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = onRegenerateClick) {
                                        Text("Retry", color = ChatQivoTeal, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = onDismissError) {
                                        Text("Dismiss", color = ChatQivoMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Compose Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChatQivoWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isStreaming) {
                OutlinedButton(
                    onClick = onStopClick,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ChatQivoLine),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ChatQivoWhite,
                        contentColor = ChatQivoInk
                    ),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("stop_generating_pill")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = ChatQivoTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stop_generating),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(modifier = Modifier.widthIn(max = 790.dp)) {
                PromptComposer(
                    text = inputText,
                    pendingAttachment = pendingAttachment,
                    isStreaming = isStreaming,
                    isListening = isListening,
                    onTextChange = onInputChange,
                    onAttachClick = onAttachClick,
                    onRemoveAttachment = onRemoveAttachment,
                    onVoiceClick = onVoiceClick,
                    onSendClick = onSendClick,
                    onStopClick = onStopClick,
                    compact = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hint_disclaimer),
                    fontSize = 10.5.sp,
                    color = Color(0xFFA0AAA6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UserMessageRow(message: MessageEntity) {
    val isRtl = containsRtlScript(message.content)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 5.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = ChatQivoUserBubble,
            border = BorderStroke(1.dp, ChatQivoUserBubbleBorder),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                if (!message.attachmentName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = if (message.content.isNotBlank()) 6.dp else 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = ChatQivoTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = message.attachmentName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ChatQivoTeal
                        )
                    }
                }
                if (message.content.isNotBlank()) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Text(
                            text = message.content,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = ChatQivoUserBubbleInk
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F2F1)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF71807A)
            )
        }
    }
}

@Composable
private fun AiMessageRow(
    messageId: String,
    content: String,
    isWaitingFirstToken: Boolean,
    isStreaming: Boolean,
    isSpeaking: Boolean,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit,
    onShare: () -> Unit,
    onSpeakToggle: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFDFF4ED), Color(0xFFE9F0FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CQ",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ChatQivoTeal
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ChatQivo",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF82908A),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (isWaitingFirstToken) {
                TypingIndicator()
            } else if (content.isNotBlank()) {
                MarkdownMessageContent(markdown = content)
            }

            if (!isStreaming && content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageActionChip(
                        icon = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        label = if (copied) stringResource(R.string.copied) else stringResource(R.string.copy),
                        onClick = {
                            clipboard.setText(AnnotatedString(content))
                            copied = true
                        }
                    )
                    MessageActionChip(
                        icon = Icons.Default.Share,
                        label = stringResource(R.string.share),
                        onClick = onShare
                    )
                    MessageActionChip(
                        icon = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        label = if (isSpeaking) stringResource(R.string.stop_listening) else stringResource(R.string.listen),
                        onClick = onSpeakToggle
                    )
                    if (showRegenerate) {
                        MessageActionChip(
                            icon = Icons.Default.Refresh,
                            label = stringResource(R.string.regenerate),
                            onClick = onRegenerate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF96A19C),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF8C9893)
        )
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "d1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 180), RepeatMode.Reverse),
        label = "d2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 360), RepeatMode.Reverse),
        label = "d3"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ChatQivoSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(alpha1, alpha2, alpha3).forEach { a ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(ChatQivoTeal)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Thinking…",
            fontSize = 12.sp,
            color = ChatQivoMuted
        )
    }
}
