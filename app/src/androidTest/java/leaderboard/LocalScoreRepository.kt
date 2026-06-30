package com.cybermusicrush.leaderboard

import android.content.Context

class LocalScoreRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("cyber_music_rush_scores", Context.MODE_PRIVATE)
    
    // Simple placeholder simulation of JSON string conversion
    // If using Gson: gson.toJson(entries) / gson.fromJson(...)
    private fun serializeList(entries: List<LeaderboardEntry>): String {
        return entries.joinToString(";") { 
            "${it.id},${it.playerName},${it.score},${it.maxCombo},${it.songId},${it.timestamp}" 
        }
    }

    private fun deserializeList(serialized: String?): List<LeaderboardEntry> {
        if (serialized.isNullOrEmpty()) return emptyList()
        return try {
            serialized.split(";").map {
                val tokens = it.split(",")
                LeaderboardEntry(tokens[0], tokens[1], tokens[2].toLong(), tokens[3].toInt(), tokens[4], tokens[5].toLong())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllScores(): List<LeaderboardEntry> {
        val serializedData = sharedPreferences.getString("high_scores", "")
        return deserializeList(serializedData)
    }

    fun saveScores(entries: List<LeaderboardEntry>) {
        sharedPreferences.edit()
            .putString("high_scores", serializeList(entries))
            .apply()
    }
}
