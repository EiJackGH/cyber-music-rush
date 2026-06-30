package com.cybermusicrush.leaderboard

import android.content.Context

class LeaderboardManager(context: Context) {
    private val repository = LocalScoreRepository(context)
    private val maxStoredScoresPerSong = 10

    /**
     * Attempts to submit a new entry. Returns true if it made it into the top list.
     */
    fun submitScore(playerName: String, score: Long, maxCombo: Int, songId: String): Boolean {
        val newEntry = LeaderboardEntry(playerName = playerName, score = score, maxCombo = maxCombo, songId = songId)
        val allScores = repository.getAllScores().toMutableList()
        
        allScores.add(newEntry)
        
        // Filter by current song, sort descending by score, and limit to max limit
        val filteredAndSorted = allScores
            .filter { it.songId == songId }
            .sortedByDescending { it.score }
            .take(maxStoredScoresPerSong)

        // If the new entry exists in our trimmed top list, it's a new high score slot!
        val didMakeLeaderboard = filteredAndSorted.any { it.id == newEntry.id }
        
        // Merge back other songs' scores so we don't erase them
        val otherSongsScores = allScores.filter { it.songId != songId }
        val finalScoresList = otherSongsScores + filteredAndSorted
        
        repository.saveScores(finalScoresList)
        return didMakeLeaderboard
    }

    /**
     * Gets the ranked leaderboard for a specific track.
     */
    fun getLeaderboardForSong(songId: String): List<LeaderboardEntry> {
        return repository.getAllScores()
            .filter { it.songId == songId }
            .sortedByDescending { it.score }
    }
}
