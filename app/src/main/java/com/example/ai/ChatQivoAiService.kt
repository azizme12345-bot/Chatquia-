package com.example.ai

import android.graphics.Bitmap
import com.example.data.local.MessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class ChatQivoAiService {

    private val systemPrompt = """
        You are ChatQivo, an intelligent, versatile All-in-One AI assistant.
        Users can discuss almost any topic in one conversation: general Q&A, writing and rewriting,
        learning and simple explanations, programming and coding, website development (HTML, CSS,
        JavaScript, React, Next.js, TypeScript, etc.), creating professional AI prompts, research,
        analysis, problem-solving, planning, creativity, and multilingual translation.

        Response Guidelines:
        - Understand the user's actual request and respond directly without unnecessary filler or repeating the question.
        - Be clear, accurate, helpful, and concise. Use headings and bullet points when helpful.
        - If asked for code or website code, provide complete, clean, working code in fenced Markdown blocks with language tags.
        - If asked for a prompt, provide a ready-to-use professional prompt.
        - If asked to rewrite text, provide the rewritten version directly.
        - Explain technical topics in simple, approachable language.
        - Never invent facts or pretend to have completed actions you cannot perform. If uncertain, clearly say so.

        Language Support:
        - Detect the user's language automatically and respond in the same language unless the user explicitly asks for another language.
        - Fully support English, Urdu (اردو), Arabic (العربية), Hindi (हिन्दी), Punjabi, Roman Urdu, mixed Urdu/English, and other common languages.
        - For Urdu and Arabic, write naturally in proper Urdu/Arabic script so RTL layout renders cleanly.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) },
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
            }
        )
    }

    fun streamResponse(
        history: List<MessageEntity>,
        userPrompt: String,
        attachedImage: Bitmap? = null
    ): Flow<String> {
        val normalizedHistory = buildAlternatingHistory(history)

        val chat = model.startChat(history = normalizedHistory)
        val outgoingContent = content("user") {
            if (attachedImage != null) {
                image(attachedImage)
            }
            if (userPrompt.isNotBlank()) {
                text(userPrompt)
            } else if (attachedImage != null) {
                text("Please analyze this image and help me with it.")
            }
        }

        return chat.sendMessageStream(outgoingContent).mapNotNull { chunk ->
            chunk.text
        }
    }

    private fun buildAlternatingHistory(history: List<MessageEntity>): List<Content> {
        val valid = history.filter { !it.isError && it.content.isNotBlank() }
        if (valid.isEmpty()) return emptyList()

        // Merge consecutive same-role messages so roles strictly alternate user/model
        val merged = mutableListOf<Pair<String, String>>()
        for (msg in valid) {
            val role = if (msg.role == "user") "user" else "model"
            if (merged.isEmpty()) {
                if (role == "user") {
                    merged.add(role to msg.content)
                }
            } else if (merged.last().first == role) {
                val prev = merged.removeAt(merged.lastIndex)
                merged.add(role to "${prev.second}\n\n${msg.content}")
            } else {
                merged.add(role to msg.content)
            }
        }

        // startChat history must end with a model turn before sending a new user turn
        if (merged.isNotEmpty() && merged.last().first == "user") {
            merged.removeAt(merged.lastIndex)
        }

        return merged.map { (role, text) ->
            content(role = role) {
                text(text)
            }
        }
    }
}
