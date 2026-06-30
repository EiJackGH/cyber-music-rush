package com.cybermusicrush.leaderboard

import java.util.UUID

data class LeaderboardEntry(
    val id: String = UUID.randomUUID().toString(),
    val playerName: String,
    val score: Long,
    val maxCombo: Int,
    val songId: String,
    val timestamp: Long = System.currentTimeMillis()
)
