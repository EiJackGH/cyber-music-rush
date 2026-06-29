package com.example.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SynthSoundPlayer
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CyberTilesViewModel(private val repository: ScoreRepository) : ViewModel() {
    
    // Theme
    var currentTheme by mutableStateOf(CyberTheme.SYNTHWAVE)
    
    // Sound setting
    var isSoundEnabled by mutableStateOf(true)
        private set

    fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        SynthSoundPlayer.isSoundEnabled = isSoundEnabled
    }

    // High score flow
    val topScores: StateFlow<List<HighScore>> = repository.topScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val personalBest: StateFlow<HighScore?> = repository.personalBest
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Game variables
    var gameState by mutableStateOf(GameState.MENU)
    var score by mutableStateOf(0)
    var tilesTapped by mutableStateOf(0)
    var isGameStarted by mutableStateOf(false)
    var gameDurationSeconds by mutableStateOf(0f)
    
    // Combo & Multiplier System
    var combo by mutableStateOf(0)
    var maxCombo by mutableStateOf(0)
    var comboMultiplier by mutableStateOf(1)
    var comboTimerProgress by mutableStateOf(1f) // 1f down to 0f
    
    private val comboMaxDurationSeconds = 1.8f
    private var comboTimeRemaining = 0f

    fun incrementCombo() {
        combo++
        if (combo > maxCombo) {
            maxCombo = combo
        }
        // Multiplier scaling: Combo >= 20 -> x4, >= 10 -> x3, >= 5 -> x2, else x1
        comboMultiplier = when {
            combo >= 20 -> 4
            combo >= 10 -> 3
            combo >= 5 -> 2
            else -> 1
        }
        
        // Trigger combo milestone sound effect
        if (combo == 5 || combo == 10 || combo == 20 || (combo > 20 && combo % 10 == 0)) {
            SynthSoundPlayer.playComboMilestone(combo)
        }

        comboTimeRemaining = comboMaxDurationSeconds
        comboTimerProgress = 1f
        recalculateSpeed()
    }

    fun resetCombo() {
        combo = 0
        comboMultiplier = 1
        comboTimeRemaining = 0f
        comboTimerProgress = 0f
        recalculateSpeed()
    }

    fun updateComboTimer(deltaTime: Float) {
        if (combo > 0 && gameState == GameState.PLAYING && isGameStarted) {
            comboTimeRemaining -= deltaTime
            if (comboTimeRemaining <= 0f) {
                resetCombo()
            } else {
                comboTimerProgress = (comboTimeRemaining / comboMaxDurationSeconds).coerceIn(0f, 1f)
            }
        } else {
            comboTimerProgress = 0f
        }
    }
    
    // Speed settings
    var currentSpeed by mutableStateOf(350f) // dp per second
    val initialSpeed = 350f
    private val speedIncrement = 6f // Increase speed by 6dp/s per tap!

    /**
     * Dynamically scales the falling speed of the tiles based on the player's current score and combo count.
     * Incorporates both progressive score growth and a high-risk/high-reward temporary combo speed boost.
     */
    fun recalculateSpeed() {
        val scoreBonus = score * 0.15f
        val comboBonus = combo * 4.5f
        currentSpeed = (initialSpeed + scoreBonus + comboBonus).coerceAtMost(1200f)
    }

    // Rows and indices
    var nextExpectedIndex by mutableStateOf(0L)
    val activeRows = mutableStateListOf<TileRow>()
    private val rowMap = mutableMapOf<Long, TileRow>()

    // Continuous scroll
    var scrollOffset by mutableStateOf(0f)

    // Animated background grid scroll phase
    var gridScrollPhase by mutableStateOf(0f)

    fun updateGridScroll(deltaTime: Float) {
        val speedFactor = if (isGameStarted && gameState == GameState.PLAYING) {
            currentSpeed / 350f
        } else {
            0.15f
        }
        gridScrollPhase = (gridScrollPhase + deltaTime * 0.8f * speedFactor) % 1f
    }

    // Particle system
    val particles = mutableStateListOf<TapParticle>()
    private var particleIdCounter = 0L

    init {
        resetGame()
    }

    fun resetGame() {
        score = 0
        tilesTapped = 0
        isGameStarted = false
        gameDurationSeconds = 0f
        currentSpeed = initialSpeed
        nextExpectedIndex = 0L
        scrollOffset = 0f
        activeRows.clear()
        rowMap.clear()
        particles.clear()
        resetCombo()
        maxCombo = 0
        
        // Pre-fill initial rows so they are ready to show on screen
        for (i in 0L..15L) {
            val row = getOrCreateTileRow(i)
            activeRows.add(row)
        }
    }

    fun startGame() {
        resetGame()
        gameState = GameState.PLAYING
    }

    fun pauseGame() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
        }
    }

    fun getOrCreateTileRow(id: Long): TileRow {
        return rowMap.getOrPut(id) {
            val prevCol = rowMap[id - 1]?.activeColumn ?: -1
            var col = (0..3).random()
            // Avoid repeating same column in consecutive rows with high probability
            if (col == prevCol && Math.random() < 0.75) {
                col = (col + 1) % 4
            }
            TileRow(id = id, activeColumn = col)
        }
    }

    // Call this in the game loop to manage sliding window of rows
    fun updateActiveRows() {
        val start = maxOf(0L, nextExpectedIndex - 3)
        val end = nextExpectedIndex + 15
        
        // Remove rows outside the window
        activeRows.removeAll { it.id < start || it.id > end }
        
        // Add missing rows in the window
        for (i in start..end) {
            if (activeRows.none { it.id == i }) {
                val insertIndex = activeRows.indexOfFirst { it.id > i }
                val row = getOrCreateTileRow(i)
                if (insertIndex == -1) {
                    activeRows.add(row)
                } else {
                    activeRows.add(insertIndex, row)
                }
            }
        }
    }

    fun handleTileTap(rowId: Long, colIndex: Int, tapX: Float, tapY: Float) {
        if (gameState != GameState.PLAYING) return

        if (!isGameStarted) {
            // First tap starts scrolling!
            val row0 = getOrCreateTileRow(0)
            if (rowId == 0L && colIndex == row0.activeColumn) {
                row0.isTapped = true
                row0.tappedColumn = colIndex
                isGameStarted = true
                incrementCombo()
                score += 10 * comboMultiplier
                recalculateSpeed() // Recalculate dynamic difficulty speed with updated score
                tilesTapped++
                nextExpectedIndex = 1L
                SynthSoundPlayer.playTapSuccess(0)
                spawnParticles(tapX, tapY)
                updateActiveRows()
                return
            } else {
                triggerGameOver(failedRowId = rowId, failedColIndex = colIndex)
                return
            }
        }

        if (rowId == nextExpectedIndex) {
            val row = getOrCreateTileRow(rowId)
            if (colIndex == row.activeColumn) {
                // Success tap!
                row.isTapped = true
                row.tappedColumn = colIndex
                
                incrementCombo()
                score += (10 + (currentSpeed / 100).toInt()) * comboMultiplier // Score scales with speed and combo multiplier!
                tilesTapped++
                
                // Recalculate dynamic difficulty speed with updated score & combo
                recalculateSpeed()
                
                // Next expected
                nextExpectedIndex++
                
                // Play audio tone
                SynthSoundPlayer.playTapSuccess(tilesTapped)
                
                // Spawn beautiful cyber particles!
                spawnParticles(tapX, tapY)
                
                // Update active rows window
                updateActiveRows()
            } else {
                // Wrong column in the correct row
                triggerGameOver(failedRowId = rowId, failedColIndex = colIndex)
            }
        } else if (rowId > nextExpectedIndex) {
            // Tapped ahead! This counts as a mistake in rhythm games
            triggerGameOver(failedRowId = rowId, failedColIndex = colIndex)
        }
    }

    fun handleBackgroundTap() {
        if (gameState == GameState.PLAYING && isGameStarted) {
            triggerGameOver()
        }
    }

    private fun spawnParticles(x: Float, y: Float) {
        val numParticles = 24
        val colors = listOf(
            currentTheme.activeTile,
            currentTheme.tappedTile,
            currentTheme.accent,
            currentTheme.glowColor
        )
        
        // 1. Spawn a glowing shockwave expanding ring at the tap point
        particles.add(
            TapParticle(
                id = particleIdCounter++,
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = currentTheme.activeTile,
                size = 12f, // starting size (will represent initial radius/scale)
                maxLifetime = 0.4f,
                type = ParticleType.SHOCKWAVE
            )
        )

        // 2. Spawn a burst of flying sparks, diamonds, and circular glowing bits
        for (i in 0 until numParticles) {
            val angle = (Math.random() * 2 * Math.PI).toFloat()
            // Randomize velocity with speed bursts
            val speed = (120f + Math.random() * 320f).toFloat() // speed in dp/s
            val vx = Math.cos(angle.toDouble()).toFloat() * speed
            val vy = Math.sin(angle.toDouble()).toFloat() * speed
            
            // Choose type of particle for retro variety
            val type = when (Math.random()) {
                in 0.0..0.4 -> ParticleType.CIRCLE
                in 0.4..0.7 -> ParticleType.SQUARE
                else -> ParticleType.GLOW_STAR
            }

            val size = when (type) {
                ParticleType.GLOW_STAR -> (12f + Math.random() * 10f).toFloat()
                ParticleType.SQUARE -> (6f + Math.random() * 8f).toFloat()
                else -> (5f + Math.random() * 9f).toFloat()
            }

            val lifetime = (0.3f + Math.random() * 0.45f).toFloat()
            val color = colors.random()
            val initialRotation = (Math.random() * 360f).toFloat()
            val rotationSpeed = (-270f + Math.random() * 540f).toFloat() // Rotational speed in degrees/s

            particles.add(
                TapParticle(
                    id = particleIdCounter++,
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = size,
                    maxLifetime = lifetime,
                    type = type,
                    initialRotation = initialRotation,
                    rotationSpeed = rotationSpeed
                )
            )
        }
    }

    fun updateParticles(deltaTime: Float) {
        if (particles.isEmpty()) return
        val updatedList = particles.map { p ->
            p.copy(
                x = p.x + p.vx * deltaTime,
                y = p.y + p.vy * deltaTime,
                vx = p.vx * 0.92f, // Deceleration friction
                vy = p.vy * 0.92f,
                initialRotation = p.initialRotation + p.rotationSpeed * deltaTime,
                age = p.age + deltaTime
            )
        }.filter { it.age < it.maxLifetime }
        
        particles.clear()
        particles.addAll(updatedList)
    }

    fun triggerGameOver(failedRowId: Long? = null, failedColIndex: Int? = null) {
        gameState = GameState.GAME_OVER
        isGameStarted = false
        
        // Mark failed tile
        if (failedRowId != null && failedColIndex != null) {
            val row = getOrCreateTileRow(failedRowId)
            row.tappedColumn = failedColIndex
        }

        SynthSoundPlayer.playGameOver()

        // Persist high score in DB
        viewModelScope.launch {
            val pb = personalBest.value
            val isNewRecord = pb == null || score > pb.score
            
            repository.insert(
                HighScore(
                    score = score,
                    tilesTapped = tilesTapped,
                    speedLevel = ((currentSpeed - initialSpeed) / 50).toInt() + 1,
                    themeName = currentTheme.name
                )
            )

            if (isNewRecord && score > 0) {
                SynthSoundPlayer.playNewHighScore()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearScores()
        }
    }
}

class CyberTilesViewModelFactory(private val repository: ScoreRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CyberTilesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CyberTilesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
