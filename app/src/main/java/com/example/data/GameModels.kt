package com.example.data

import androidx.compose.ui.graphics.Color

enum class TileType {
    NORMAL,
    SCORE_MULTIPLIER,
    INVINCIBILITY
}

data class TileRow(
    val id: Long,
    val activeColumn: Int,
    var isTapped: Boolean = false,
    var tappedColumn: Int = -1, // -1 means untapped, or 0-3 if tapped
    var isMissed: Boolean = false,
    val tileType: TileType = TileType.NORMAL
)

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class ParticleType {
    CIRCLE,
    SQUARE,
    SHOCKWAVE,
    GLOW_STAR
}

data class TapParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val maxLifetime: Float,
    var age: Float = 0f,
    val type: ParticleType = ParticleType.CIRCLE,
    val initialRotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

enum class CyberTheme(
    val displayName: String,
    val background: Color,
    val activeTile: Color,
    val tappedTile: Color,
    val border: Color,
    val accent: Color,
    val gridLine: Color,
    val textPrimary: Color,
    val glowColor: Color
) {
    SYNTHWAVE(
        displayName = "Synthwave Purple",
        background = Color(0xFF0F051D),
        activeTile = Color(0xFFFF007F), // Hot pink
        tappedTile = Color(0xFF00FFFF), // Electric Cyan
        border = Color(0xFFFF007F),
        accent = Color(0xFF00FFFF),
        gridLine = Color(0xFF26123D),
        textPrimary = Color(0xFFFFFFFF),
        glowColor = Color(0xAAFF007F)
    ),
    MATRIX(
        displayName = "Matrix Green",
        background = Color(0xFF000000),
        activeTile = Color(0xFF39FF14), // Matrix Green
        tappedTile = Color(0xFF105C10), // Darker Green
        border = Color(0xFF39FF14),
        accent = Color(0xFF80FF80),
        gridLine = Color(0xFF0A2B0A),
        textPrimary = Color(0xFFD0FFD0),
        glowColor = Color(0xAA39FF14)
    ),
    OUTRUN(
        displayName = "Sunset Outrun",
        background = Color(0xFF150A1A),
        activeTile = Color(0xFFFF5E00), // Sunset Orange
        tappedTile = Color(0xFFFFD700), // Gold
        border = Color(0xFFFF5E00),
        accent = Color(0xFFFFD700),
        gridLine = Color(0xFF2F1A3B),
        textPrimary = Color(0xFFFFFFFF),
        glowColor = Color(0xAAFF5E00)
    ),
    CYBERPUNK(
        displayName = "Cyber Neon",
        background = Color(0xFF06141D),
        activeTile = Color(0xFF00F0FF), // Neon Cyan
        tappedTile = Color(0xFFC084FC), // Neon Light Purple
        border = Color(0xFF00F0FF),
        accent = Color(0xFF8B5CF6),
        gridLine = Color(0xFF112F41),
        textPrimary = Color(0xFFE2F0F9),
        glowColor = Color(0xAA00F0FF)
    )
}
