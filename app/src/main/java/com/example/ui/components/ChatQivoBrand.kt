package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ConversationEntity
import com.example.ui.PendingAttachment
import com.example.ui.theme.ChatQivoBlueSurface
import com.example.ui.theme.ChatQivoFocusBorder
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoInputBorder
import com.example.ui.theme.ChatQivoLine
import com.example.ui.theme.ChatQivoMintSurface
import com.example.ui.theme.ChatQivoMintTop
import com.example.ui.theme.ChatQivoMuted
import com.example.ui.theme.ChatQivoSoft
import com.example.ui.theme.ChatQivoSubtle
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoTealBright
import com.example.ui.theme.ChatQivoTealDark
import com.example.ui.theme.ChatQivoTealDeep
import com.example.ui.theme.ChatQivoWhite
import com.example.ui.theme.ManropeFontFamily

@Composable
fun ChatQivoCubeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val sx = this.size.width / 40f
        val sy = this.size.height / 40f
        fun pt(x: Float, y: Float) = Offset(x * sx, y * sy)

        val topFace = Path().apply {
            moveTo(20f * sx, 2f * sy)
            lineTo(37f * sx, 11.5f * sy)
            lineTo(20f * sx, 21f * sy)
            lineTo(3f * sx, 11.5f * sy)
            close()
        }
        val leftFace = Path().apply {
            moveTo(3f * sx, 11.5f * sy)
            lineTo(20f * sx, 21f * sy)
            lineTo(20f * sx, 38f * sy)
            lineTo(3f * sx, 28.5f * sy)
            close()
        }
        val rightFace = Path().apply {
            moveTo(37f * sx, 11.5f * sy)
            lineTo(20f * sx, 21f * sy)
            lineTo(20f * sx, 38f * sy)
            lineTo(37f * sx, 28.5f * sy)
            close()
        }

        drawPath(topFace, color = ChatQivoMintTop)
        drawPath(leftFace, color = Color(0xFF0B625B))
        drawPath(rightFace, color = ChatQivoTealDeep)

        val edgeStroke = Stroke(width = 1.2f * sx)
        val edgeColor = Color(0xFF174B45)
        drawPath(topFace, color = edgeColor, style = edgeStroke)
        drawPath(leftFace, color = edgeColor, style = edgeStroke)
        drawPath(rightFace, color = edgeColor, style = edgeStroke)
        drawLine(edgeColor, pt(20f, 2f), pt(20f, 21f), strokeWidth = 1.2f * sx)
    }
}

@Composable
fun ChatQivoWordmark(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChatQivoCubeLogo(size = 32.dp)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ChatQivoInk)) {
                    append(stringResource(R.string.brand_chat))
                }
                withStyle(SpanStyle(color = ChatQivoTeal)) {
                    append(stringResource(R.string.brand_qivo))
                }
            },
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
            letterSpacing = (-0.8).sp
        )
    }
}

@Composable
fun ChatQivoSidebarContent(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    searchQuery: String,
    isHomeActive: Boolean,
    isPromptStudioActive: Boolean,
    onSearchChange: (String) -> Unit,
    onBrandClick: () -> Unit,
    onNewChat: () -> Unit,
    onHomeClick: () -> Unit,
    onPromptStudioClick: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (ConversationEntity) -> Unit,
    onDeleteConversation: (ConversationEntity) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(276.dp)
            .background(ChatQivoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        ChatQivoWordmark(
            onClick = onBrandClick,
            modifier = Modifier.testTag("sidebar_brand")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // New Chat Gradient Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ChatQivoTeal, ChatQivoTealBright)
                    )
                )
                .clickable(onClick = onNewChat)
                .testTag("new_chat_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = ChatQivoWhite,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.new_chat),
                    color = ChatQivoWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Chats Input
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFBFCFC),
            border = BorderStroke(1.dp, ChatQivoLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ChatQivoMuted,
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ChatQivoInk,
                        fontSize = 13.sp,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                    ),
                    cursorBrush = SolidColor(ChatQivoTeal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_chats_input"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_chats_hint),
                                color = ChatQivoMuted,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = ChatQivoMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.workspace_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA0AAA6),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        SidebarNavItem(
            label = stringResource(R.string.nav_home),
            icon = Icons.Default.Home,
            selected = isHomeActive,
            onClick = onHomeClick,
            testTag = "nav_home"
        )

        SidebarNavItem(
            label = stringResource(R.string.nav_projects),
            icon = Icons.Default.GridView,
            selected = isPromptStudioActive,
            onClick = onPromptStudioClick,
            testTag = "nav_prompt_studio"
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.recent_chats_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA0AAA6),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (conversations.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.no_recent_chats)
                        } else {
                            stringResource(R.string.no_search_results)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatQivoMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            } else {
                items(conversations, key = { it.id }) { convo ->
                    SidebarHistoryItem(
                        conversation = convo,
                        selected = convo.id == activeConversationId,
                        onClick = { onSelectConversation(convo.id) },
                        onRename = { onRenameConversation(convo) },
                        onDelete = { onDeleteConversation(convo) }
                    )
                }
            }
        }

        HorizontalDivider(color = ChatQivoLine, modifier = Modifier.padding(vertical = 8.dp))

        SidebarNavItem(
            label = stringResource(R.string.settings),
            icon = Icons.Default.Settings,
            selected = false,
            onClick = onSettingsClick,
            testTag = "sidebar_settings"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFD9F4EB), Color(0xFFDCE8FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CQ",
                    color = ChatQivoTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.guest_user),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatQivoInk
                )
                Text(
                    text = stringResource(R.string.personal_workspace),
                    fontSize = 10.sp,
                    color = ChatQivoMuted
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) ChatQivoMintSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) ChatQivoTeal else ChatQivoSubtle,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) ChatQivoTeal else ChatQivoSubtle
        )
    }
}

@Composable
private fun SidebarHistoryItem(
    conversation: ConversationEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ChatQivoMintSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = conversation.title,
            fontSize = 12.sp,
            color = if (selected) ChatQivoTealDark else Color(0xFF5D6A65),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Conversation actions",
                    tint = ChatQivoMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = ChatQivoWhite
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename_chat), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.delete_chat),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun PromptComposer(
    text: String,
    pendingAttachment: PendingAttachment?,
    isStreaming: Boolean,
    isListening: Boolean,
    onTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (text.isNotBlank() || pendingAttachment != null) ChatQivoFocusBorder else ChatQivoInputBorder,
                shape = RoundedCornerShape(if (compact) 16.dp else 19.dp)
            ),
        shape = RoundedCornerShape(if (compact) 16.dp else 19.dp),
        color = ChatQivoWhite,
        shadowElevation = if (compact) 4.dp else 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 12.dp,
                    top = if (compact) 10.dp else 14.dp,
                    bottom = if (compact) 9.dp else 11.dp
                )
        ) {
            if (pendingAttachment != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ChatQivoSoft,
                    border = BorderStroke(1.dp, ChatQivoLine),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pendingAttachment.previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = pendingAttachment.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                        Text(
                            text = pendingAttachment.name,
                            fontSize = 12.sp,
                            color = ChatQivoInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(140.dp)
                        )
                        IconButton(
                            onClick = onRemoveAttachment,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = ChatQivoMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = ChatQivoInk,
                    fontSize = if (compact) 14.sp else 15.sp
                ),
                cursorBrush = SolidColor(ChatQivoTeal),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions(
                    onSend = { if (!isStreaming) onSendClick() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (compact) 42.dp else 54.dp)
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .testTag("prompt_input"),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening… speak now" else stringResource(R.string.input_placeholder),
                            color = if (isListening) ChatQivoTeal else Color(0xFFA3AEAA),
                            fontSize = if (compact) 14.sp else 15.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_attach_image),
                            tint = Color(0xFF87938E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ChatQivoMintSurface else Color.Transparent)
                            .testTag("voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.cd_voice_input),
                            tint = if (isListening) ChatQivoTeal else Color(0xFF87938E),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChatQivoSoft
                    ) {
                        Text(
                            text = "ChatQivo AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF71807A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChatQivoInk)
                            .clickable(onClick = onStopClick)
                            .testTag("stop_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cd_stop_generation),
                            tint = ChatQivoWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val canSend = text.isNotBlank() || pendingAttachment != null
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (canSend) {
                                        listOf(ChatQivoTeal, ChatQivoTealBright)
                                    } else {
                                        listOf(Color(0xFF9EC7C1), Color(0xFF9EC7C1))
                                    }
                                )
                            )
                            .clickable(enabled = canSend, onClick = onSendClick)
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.cd_send_message),
                            tint = ChatQivoWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
