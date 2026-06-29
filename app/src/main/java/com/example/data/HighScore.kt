package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val tilesTapped: Int,
    val speedLevel: Int,
    val themeName: String,
    val timestamp: Long = System.currentTimeMillis()
)
