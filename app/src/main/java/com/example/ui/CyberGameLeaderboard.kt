package com.example.ui

import android.content.Context
import java.util.UUID

/**
 * A self-contained, high-performance Leaderboard System for Cyber Music Rush.
 * Handles local score tracking, sorting, and structural limits per song.
 */
object CyberGameLeaderboard {

    private const val PREFS_NAME = "cyber_game_leaderboard_prefs"
    private const val SCORES_KEY = "saved_leaderboard_scores"
    private const val MAX_ENTRIES_PER_SONG = 10

    /**
     * Data class representing a distinct leaderboard high score submission.
     */
    data class Entry(
        val id: String = UUID.randomUUID().toString(),
        val playerName: String,
        val score: Long,
        val maxCombo: Int,
        val accuracy: Float, // e.g., 98.5f for 98.5%
        val songId: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Submits a fresh score to the system. 
     * Automatically filters, sorts, limits to top 10, and updates persistence.
     * 
     * @return Boolean [true] if the submitted score successfully placed in the Top 10.
     */
    fun submitScore(
        context: Context,
        playerName: String,
        score: Long,
        maxCombo: Int,
        accuracy: Float,
        songId: String
    ): Boolean {
        // Sanitize name to prevent breaking the raw string formatting delimiter
        val sanitizedName = playerName.replace(",", " ").replace("|", " ").trim()
        val nameToStore = if (sanitizedName.isEmpty()) "CyberRunner" else sanitizedName

        val newEntry = Entry(
            playerName = nameToStore,
            score = score,
            maxCombo = maxCombo,
            accuracy = accuracy,
            songId = songId
        )

        val allScores = loadAllScores(context).toMutableList()
        allScores.add(newEntry)

        // Isolate current track scores, sort descending by score, and grab top 10
        val targetTrackRankings = allScores
            .filter { it.songId == songId }
            .sortedByDescending { it.score }
            .take(MAX_ENTRIES_PER_SONG)

        // Check if our new entry survived the filter/sorting cut
        val didPlaceInTopTen = targetTrackRankings.any { it.id == newEntry.id }

        // Merge back unaltered tracks data so we don't accidentally drop other song scores
        val untouchedTracksRankings = allScores.filter { it.songId != songId }
        val consolidatedScores = untouchedTracksRankings + targetTrackRankings

        saveScores(context, consolidatedScores)
        return didPlaceInTopTen
    }

    /**
     * Retrieves the ranked, descending list of top scores for a specific track.
     */
    fun getTopScoresForSong(context: Context, songId: String): List<Entry> {
        return loadAllScores(context)
            .filter { it.songId == songId }
            .sortedByDescending { it.score }
    }

    /**
     * Completely wipes the leaderboard data (useful for system settings/reset options).
     */
    fun clearLeaderboard(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(SCORES_KEY)
            .apply()
    }

    // --- Private Data Persistence Serialization Layer ---

    private fun loadAllScores(context: Context): List<Entry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawData = prefs.getString(SCORES_KEY, "") ?: ""
        if (rawData.isEmpty()) return emptyList()

        return try {
            rawData.split("|").map { entryRow ->
                val tokens = entryRow.split(",")
                Entry(
                    id = tokens[0],
                    playerName = tokens[1],
                    score = tokens[2].toLong(),
                    maxCombo = tokens[3].toInt(),
                    accuracy = tokens[4].toFloat(),
                    songId = tokens[5],
                    timestamp = tokens[6].toLong()
                )
            }
        } catch (e: Exception) {
            // Fault tolerance: If formatting structure is corrupted/mutated, fall back cleanly
            emptyList()
        }
    }

    private fun saveScores(context: Context, scores: List<Entry>) {
        val serializedData = scores.joinToString("|") { 
            "${it.id},${it.playerName},${it.score},${it.maxCombo},${it.accuracy},${it.songId},${it.timestamp}" 
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SCORES_KEY, serializedData)
            .apply()
    }
}