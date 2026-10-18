package com.medstudy.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object AiEngine {
    private val client = OkHttpClient()

    suspend fun explainMedicalText(
        text: String,
        mode: String,
        provider: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (provider == "Demo Mode" || apiKey.isBlank()) {
            return@withContext getDemoExplanation(text, mode)
        }

        try {
            when (provider) {
                "Gemini API" -> callGemini(text, mode, apiKey)
                "OpenAI API" -> callOpenAI(text, mode, apiKey)
                "OpenRouter" -> callOpenRouter(text, mode, apiKey)
                else -> getDemoExplanation(text, mode)
            }
        } catch (e: Exception) {
            "Error connecting to $provider: ${e.localizedMessage}\n\nFalling back to offline explanation:\n${getDemoExplanation(text, mode)}"
        }
    }

    private fun getDemoExplanation(text: String, mode: String): String {
        return """
            [DEMO MODE ACTIVE — SAMPLE CLINICAL DATA]
            Mode: $mode
            Target Text: "$text"
            
            • Simple Definition: A clinical finding or entity requiring structured medical evaluation in internal medicine.
            • Medical Definition & Pathophysiology: Represents an underlying disturbance in cellular homeostasis, inflammatory cascade, or organ system autoregulation.
            • Clinical Features: May present with fatigue, localized discomfort, or systematic manifestations depending on severity.
            • Investigations: Recommended baseline labs include CBC, metabolic panel, and targeted imaging.
            • Management Overview: Supportive care, lifestyle adjustment, and etiology-specific pharmacotherapy.
            • MRCP Examination Trap: Examiners frequently test secondary causes and atypical presentations. Always verify against UpToDate or authoritative guidelines.
        """.trimIndent()
    }

    private fun callGemini(text: String, mode: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val prompt = "Act as an expert medical tutor preparing a student for the MRCP. Provide a structured medical breakdown for mode '$mode' based on this text: '$text'"
        
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
            val respStr = response.body?.string() ?: return "Empty response"
            val json = JSONObject(respStr)
            return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun callOpenAI(text: String, mode: String, apiKey: String): String {
        val url = "https://api.openai.com/v1/chat/completions"
        val prompt = "Act as an expert medical tutor. Provide a structured breakdown for mode '$mode' on: '$text'"
        
        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
            val respStr = response.body?.string() ?: return "Empty response"
            return JSONObject(respStr).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun callOpenRouter(text: String, mode: String, apiKey: String): String {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val prompt = "Medical study assistant mode '$mode' for: '$text'"
        
        val jsonBody = JSONObject().apply {
            put("model", "google/gemini-flash-1.5")
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://medstudy.ai")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
            val respStr = response.body?.string() ?: return "Empty response"
            return JSONObject(respStr).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }
}
