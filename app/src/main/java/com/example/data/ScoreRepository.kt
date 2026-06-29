package com.example.data

import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val highScoreDao: HighScoreDao) {
    val topScores: Flow<List<HighScore>> = highScoreDao.getTopScores(10)
    val personalBest: Flow<HighScore?> = highScoreDao.getHighScore()

    suspend fun insert(highScore: HighScore) {
        highScoreDao.insertHighScore(highScore)
    }

    suspend fun clearScores() {
        highScoreDao.clearAllScores()
    }
}
