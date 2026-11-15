package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.PendingAttachment
import com.example.ui.PromptTemplate
import com.example.ui.components.PromptComposer
import com.example.ui.theme.ChatQivoAmberInk
import com.example.ui.theme.ChatQivoAmberSurface
import com.example.ui.theme.ChatQivoBlueAccent
import com.example.ui.theme.ChatQivoBlueInk
import com.example.ui.theme.ChatQivoBlueSurface
import com.example.ui.theme.ChatQivoErrorBorder
import com.example.ui.theme.ChatQivoErrorInk
import com.example.ui.theme.ChatQivoErrorSurface
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoLine
import com.example.ui.theme.ChatQivoMintSurface
import com.example.ui.theme.ChatQivoMuted
import com.example.ui.theme.ChatQivoPurpleInk
import com.example.ui.theme.ChatQivoPurpleSurface
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoWhite
import com.example.ui.theme.ManropeFontFamily

private data class HomeSuggestion(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color
)

@Composable
fun HomeScreen(
    inputText: String,
    pendingAttachment: PendingAttachment?,
    isStreaming: Boolean,
    isListening: Boolean,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        HomeSuggestion(
            title = "Write something",
            subtitle = "Ideas, drafts and better words",
            prompt = "Help me write something clear and engaging.",
            icon = Icons.Default.Edit,
            iconBg = ChatQivoMintSurface,
            iconTint = ChatQivoTeal
        ),
        HomeSuggestion(
            title = "Understand a topic",
            subtitle = "Learn with simple explanations",
            prompt = "Explain a topic to me step by step.",
            icon = Icons.Default.Lightbulb,
            iconBg = ChatQivoBlueSurface,
            iconTint = ChatQivoBlueInk
        ),
        HomeSuggestion(
            title = "Build a project",
            subtitle = "Turn ideas into real things",
            prompt = "Help me plan and build a website project.",
            icon = Icons.Default.Code,
            iconBg = ChatQivoPurpleSurface,
            iconTint = ChatQivoPurpleInk
        ),
        HomeSuggestion(
            title = "Explore an idea",
            subtitle = "Think, research and discover",
            prompt = "Help me explore a new idea.",
            icon = Icons.Default.AutoAwesome,
            iconBg = ChatQivoAmberSurface,
            iconTint = ChatQivoAmberInk
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ChatQivoWhite)
            .imePadding()
            .navigationBarsPadding()
    ) {
        val isWide = maxWidth >= 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWide) 32.dp else 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spark Pill
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFFEFF8F5),
                    border = BorderStroke(1.dp, Color(0xFFCDE7DF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1AA58F))
                        )
                        Text(
                            text = stringResource(R.string.hero_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = ChatQivoTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Headline
                val heroHeadline = buildAnnotatedString {
                    append(stringResource(R.string.hero_title_prefix))
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(ChatQivoTeal, ChatQivoBlueAccent)
                            )
                        )
                    ) {
                        append(stringResource(R.string.hero_title_em))
                    }
                    append(stringResource(R.string.hero_title_suffix))
                }

                Text(
                    text = heroHeadline,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isWide) 46.sp else 33.sp,
                        lineHeight = if (isWide) 52.sp else 39.sp,
                        letterSpacing = if (isWide) (-1.8).sp else (-1.2).sp,
                        color = ChatQivoInk,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF7D8A85),
                        fontSize = if (isWide) 15.sp else 13.5.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ChatQivoErrorSurface,
                        border = BorderStroke(1.dp, ChatQivoErrorBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clickable(onClick = onDismissError)
                    ) {
                        Text(
                            text = errorMessage,
                            color = ChatQivoErrorInk,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

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
                    compact = false
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Suggestion Cards (4 cols on wide, 2x2 on mobile)
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        suggestions.forEachIndexed { idx, item ->
                            SuggestionCard(
                                item = item,
                                onClick = { onSuggestionClick(item.prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("suggestion_card_$idx")
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SuggestionCard(
                                item = suggestions[0],
                                onClick = { onSuggestionClick(suggestions[0].prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("suggestion_card_0")
                            )
                            SuggestionCard(
                                item = suggestions[1],
                                onClick = { onSuggestionClick(suggestions[1].prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("suggestion_card_1")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SuggestionCard(
                                item = suggestions[2],
                                onClick = { onSuggestionClick(suggestions[2].prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("suggestion_card_2")
                            )
                            SuggestionCard(
                                item = suggestions[3],
                                onClick = { onSuggestionClick(suggestions[3].prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("suggestion_card_3")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.hint_disclaimer),
                    fontSize = 11.sp,
                    color = Color(0xFF96A19D),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.footer_private),
                    fontSize = 11.sp,
                    color = Color(0xFF98A39F)
                )
                Text(text = "·", fontSize = 11.sp, color = Color(0xFF98A39F))
                Text(
                    text = stringResource(R.string.footer_minds),
                    fontSize = 11.sp,
                    color = Color(0xFF98A39F)
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    item: HomeSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(106.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = ChatQivoWhite,
        border = BorderStroke(1.dp, Color(0xFFD9E5E0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatQivoInk,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = ChatQivoMuted,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun PromptStudioScreen(
    templates: List<PromptTemplate>,
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatQivoWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 760.dp)) {
            Text(
                text = "Prompt Studio",
                style = MaterialTheme.typography.headlineMedium,
                color = ChatQivoInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ready-to-use starters for writing, coding, learning, Urdu/Arabic multilingual chats, and AI prompt engineering.",
                style = MaterialTheme.typography.bodyMedium,
                color = ChatQivoMuted
            )
            Spacer(modifier = Modifier.height(20.dp))

            templates.forEach { tpl ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTemplate(tpl.prompt) },
                    shape = RoundedCornerShape(14.dp),
                    color = ChatQivoWhite,
                    border = BorderStroke(1.dp, ChatQivoLine)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ChatQivoMintSurface
                        ) {
                            Text(
                                text = tpl.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChatQivoTeal,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tpl.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ChatQivoInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tpl.subtitle,
                            fontSize = 13.sp,
                            color = ChatQivoMuted
                        )
                    }
                }
            }
        }
    }
}
