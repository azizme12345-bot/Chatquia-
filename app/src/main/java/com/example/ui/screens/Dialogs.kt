package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ConversationEntity
import com.example.ui.theme.ChatQivoErrorInk
import com.example.ui.theme.ChatQivoInk
import com.example.ui.theme.ChatQivoMuted
import com.example.ui.theme.ChatQivoTeal
import com.example.ui.theme.ChatQivoWhite

@Composable
fun RenameConversationDialog(
    conversation: ConversationEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember(conversation.id) { mutableStateOf(conversation.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChatQivoWhite,
        title = {
            Text(
                text = "Rename conversation",
                style = MaterialTheme.typography.titleMedium,
                color = ChatQivoInk
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("Conversation title") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ChatQivoTeal)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = ChatQivoMuted)
            }
        }
    )
}

@Composable
fun DeleteConversationDialog(
    conversation: ConversationEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChatQivoWhite,
        title = {
            Text(
                text = "Delete conversation?",
                style = MaterialTheme.typography.titleMedium,
                color = ChatQivoInk
            )
        },
        text = {
            Text(
                text = "\"${conversation.title}\" will be permanently removed from your device.",
                fontSize = 13.sp,
                color = ChatQivoMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ChatQivoErrorInk)
            ) {
                Text(stringResource(R.string.delete_chat))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = ChatQivoMuted)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    conversationCount: Int,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ChatQivoWhite,
        title = {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = ChatQivoInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ChatQivo Workspace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ChatQivoInk
                )
                Text(
                    text = "Powered by Gemini AI (gemini-3.8-flash) via Firebase AI Logic with automatic multilingual & RTL Urdu/Arabic support.",
                    fontSize = 12.5.sp,
                    color = ChatQivoMuted
                )
                Text(
                    text = "Saved conversations on this device: $conversationCount",
                    fontSize = 12.5.sp,
                    color = ChatQivoInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (!confirmClear) {
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        enabled = conversationCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.clear_all_chats),
                            color = if (conversationCount > 0) ChatQivoErrorInk else ChatQivoMuted
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onClearAll()
                                confirmClear = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatQivoErrorInk),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm clear")
                        }
                        TextButton(
                            onClick = { confirmClear = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Keep")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = ChatQivoTeal, fontWeight = FontWeight.Bold)
            }
        }
    )
}
