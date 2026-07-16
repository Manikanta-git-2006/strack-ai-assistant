package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {

    private val apiService = RetrofitClient.service

    /**
     * Sends a message to the Gemini API with a system instruction configuring Strack AI as the assistant.
     * Returns the text response.
     */
    suspend fun getAiResponse(userPrompt: String, history: List<Content> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = RetrofitClient.getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("AiRepository", "API Key is missing or default placeholder!")
            return@withContext "Error: API Key is not configured. Please add your GEMINI_API_KEY to the secrets panel."
        }

        // Configure system instruction to set the assistant's identity as "Strack AI"
        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = "You are 'Strack AI', a brilliant, friendly, and highly scalable AI coding companion designed for Android developers. " +
                           "You specialize in Kotlin, Jetpack Compose, Material Design 3, asynchronous programming, coroutines, and UI/UX best practices. " +
                           "Always speak with confidence, give concise and elegant solutions, and keep explanations practical. Under no circumstances should you break character. " +
                           "Keep responses well-formatted using clear paragraphs or neat lists for high readability."
                )
            )
        )

        // Compile chat history and new message
        val updatedContents = mutableListOf<Content>()
        updatedContents.addAll(history)
        updatedContents.add(Content(parts = listOf(Part(text = userPrompt))))

        val request = GenerateContentRequest(
            contents = updatedContents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = 0.7f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!candidateText.isNullOrBlank()) {
                candidateText
            } else {
                "Strack AI could not formulate a reply. Please try rephrasing your message."
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Exception during Gemini API generation", e)
            "Strack AI encountered an error: ${e.localizedMessage ?: "Unknown connection error. Check internet connection."}"
        }
    }
}
