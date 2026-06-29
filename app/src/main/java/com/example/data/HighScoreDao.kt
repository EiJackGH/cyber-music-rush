package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC, timestamp DESC LIMIT :limit")
    fun getTopScores(limit: Int): Flow<List<HighScore>>

    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 1")
    fun getHighScore(): Flow<HighScore?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScore)

    @Query("DELETE FROM high_scores")
    suspend fun clearAllScores()
}
