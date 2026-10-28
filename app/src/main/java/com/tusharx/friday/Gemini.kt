package com.tusharx.friday

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    data class Turn(val role: String, val text: String) // role: "user" or "model"

    suspend fun generate(prefs: Prefs, system: String, turns: List<Turn>, json: Boolean = true): String =
        withContext(Dispatchers.IO) {
            if (prefs.apiKey.isBlank()) throw RuntimeException("Gemini API key missing. Settings mein key daalo.")
            val contents = JSONArray()
            for (t in turns) {
                contents.put(
                    JSONObject().put("role", t.role)
                        .put("parts", JSONArray().put(JSONObject().put("text", t.text)))
                )
            }
            val gen = JSONObject().put("temperature", 0.9)
            if (json) gen.put("responseMimeType", "application/json")
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                .put("contents", contents)
                .put("generationConfig", gen)

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${prefs.model}:generateContent")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", prefs.apiKey)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = try { JSONObject(text).getJSONObject("error").getString("message") } catch (e: Exception) { text.take(200) }
                throw RuntimeException("Gemini error $code: $msg")
            }
            val parts = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
            sb.toString()
        }
}
