package com.example.data

import android.graphics.Bitmap
import com.example.ai.ChatQivoAiService
import com.example.data.local.ChatDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val dao: ChatDao,
    private val aiService: ChatQivoAiService
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = dao.observeConversations()

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun getMessagesOnce(conversationId: String): List<MessageEntity> =
        dao.getMessagesOnce(conversationId)

    suspend fun createConversation(firstMessageTitle: String): ConversationEntity {
        val now = System.currentTimeMillis()
        val cleanTitle = firstMessageTitle.trim().replace("\n", " ")
            .let { if (it.length > 42) it.take(42).trimEnd() + "…" else it }
            .ifBlank { "New conversation" }
        val convo = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertConversation(convo)
        return convo
    }

    suspend fun touchConversation(conversation: ConversationEntity) {
        dao.upsertConversation(conversation.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        isError: Boolean = false
    ): MessageEntity {
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            attachmentUri = attachmentUri,
            attachmentName = attachmentName,
            timestamp = System.currentTimeMillis(),
            isError = isError
        )
        dao.upsertMessage(msg)
        return msg
    }

    suspend fun updateMessage(message: MessageEntity) {
        dao.upsertMessage(message)
    }

    suspend fun deleteMessage(id: String) = dao.deleteMessage(id)

    suspend fun renameConversation(id: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotBlank()) {
            dao.renameConversation(id, trimmed, System.currentTimeMillis())
        }
    }

    suspend fun deleteConversation(id: String) = dao.deleteConversation(id)

    suspend fun clearAll() = dao.clearAllConversations()

    fun streamAiReply(
        history: List<MessageEntity>,
        prompt: String,
        attachedImage: Bitmap?
    ): Flow<String> = aiService.streamResponse(history, prompt, attachedImage)
}
