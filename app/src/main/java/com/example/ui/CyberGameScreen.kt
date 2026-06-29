package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CyberTheme
import com.example.data.GameState
import com.example.data.HighScore
import com.example.data.TapParticle
import com.example.data.ParticleType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.viewmodel.CyberTilesViewModel
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp

@Composable
fun CyberGameScreen(
    viewModel: CyberTilesViewModel,
    modifier: Modifier = Modifier
) {
    val theme = viewModel.currentTheme
    val gameState = viewModel.gameState
    val score = viewModel.score
    val tilesTapped = viewModel.tilesTapped
    val speed = viewModel.currentSpeed
    val level = ((speed - viewModel.initialSpeed) / 50).toInt() + 1
    val progress = ((speed - viewModel.initialSpeed) % 50) / 50f

    val personalBest by viewModel.personalBest.collectAsState()
    val topScores by viewModel.topScores.collectAsState()

    var showLeaderboard by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var containerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val containerHeightDp = with(density) { containerHeightPx.toDp() }

    val rowHeightDp = 160.dp
    val bottomOffsetDp = 100.dp

    // Setup Game loop for continuous auto-scrolling
    LaunchedEffect(viewModel.isGameStarted, viewModel.gameState) {
        if (viewModel.isGameStarted && viewModel.gameState == GameState.PLAYING) {
            var lastTime = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { time ->
                    val deltaSeconds = (time - lastTime) / 1_000_000_000f
                    lastTime = time

                    // Increase scroll offset
                    val newOffset = viewModel.scrollOffset + (deltaSeconds * viewModel.currentSpeed)
                    viewModel.scrollOffset = newOffset

                    // Track game session duration
                    viewModel.gameDurationSeconds += deltaSeconds

                    // Update physics particles
                    viewModel.updateParticles(deltaSeconds)

                    // Update combo timer
                    viewModel.updateComboTimer(deltaSeconds)

                    // Update background grid scroll
                    viewModel.updateGridScroll(deltaSeconds)

                    // Collision / Miss detection:
                    // If the lowest untapped row has scrolled completely off-screen at the bottom
                    if (containerHeightDp > 0.dp) {
                        val limitOffset = (viewModel.nextExpectedIndex + 1) * rowHeightDp.value + bottomOffsetDp.value
                        if (newOffset > limitOffset) {
                            viewModel.triggerGameOver(viewModel.nextExpectedIndex, viewModel.getOrCreateTileRow(viewModel.nextExpectedIndex).activeColumn)
                        }
                    }
                }
            }
        }
    }

    // Secondary lighter loop for updating particles when game is not playing but particles are active
    LaunchedEffect(viewModel.isGameStarted, viewModel.gameState) {
        if (!viewModel.isGameStarted || viewModel.gameState != GameState.PLAYING) {
            var lastTime = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { time ->
                    val deltaSeconds = (time - lastTime) / 1_000_000_000f
                    lastTime = time
                    viewModel.updateParticles(deltaSeconds)
                    viewModel.updateGridScroll(deltaSeconds)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Decorative background grids & gradients
        SynthwaveBackground(
            theme = theme,
            multiplier = viewModel.comboMultiplier,
            scrollPhase = viewModel.gridScrollPhase
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section: MD3 Top App Bar styled
            HeaderSection(
                currentScore = score,
                personalBestScore = personalBest?.score ?: 0,
                theme = theme,
                isSoundEnabled = viewModel.isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onInfoClick = { showThemeSelector = true },
                gameState = gameState,
                onPauseClick = { viewModel.pauseGame() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Game Grid / Lanes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, theme.gridLine.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .onGloballyPositioned { layoutCoordinates ->
                        containerHeightPx = layoutCoordinates.size.height
                    }
                    .pointerInput(gameState) {
                        detectTapGestures {
                            viewModel.handleBackgroundTap()
                        }
                    }
            ) {
                if (containerHeightDp > 0.dp) {
                    GameGrid(
                        viewModel = viewModel,
                        containerHeight = containerHeightDp,
                        rowHeight = rowHeightDp,
                        bottomOffset = bottomOffsetDp,
                        theme = theme
                    )
                }

                // Render dynamic tap particles
                ParticlesLayer(particles = viewModel.particles)

                // Combo indicator floating overlay at the top-middle of the grid
                if (gameState == GameState.PLAYING && viewModel.isGameStarted) {
                    ComboIndicator(
                        combo = viewModel.combo,
                        multiplier = viewModel.comboMultiplier,
                        timerProgress = viewModel.comboTimerProgress,
                        theme = theme,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }

                // Start Overlay (if game hasn't started yet)
                if (gameState == GameState.PLAYING && !viewModel.isGameStarted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )

                            Text(
                                text = "TAP TILE 1 TO START",
                                color = theme.activeTile,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = theme.glowColor,
                                        blurRadius = 15f
                                    )
                                ),
                                modifier = Modifier.alpha(pulseAlpha),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tiles will slide down. Tap them in order!",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Game Over Screen Overlay
                if (gameState == GameState.GAME_OVER) {
                    GameOverOverlay(
                        score = score,
                        tilesTapped = tilesTapped,
                        level = level,
                        maxCombo = viewModel.maxCombo,
                        personalBestScore = personalBest?.score ?: 0,
                        theme = theme,
                        onRestart = { viewModel.startGame() }
                    )
                }

                // Pause Overlay
                if (gameState == GameState.PAUSED) {
                    PauseOverlay(
                        score = score,
                        tilesTapped = tilesTapped,
                        maxCombo = viewModel.maxCombo,
                        gameDurationSeconds = viewModel.gameDurationSeconds,
                        level = level,
                        theme = theme,
                        onResume = { viewModel.resumeGame() },
                        onRestart = { viewModel.startGame() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Level Progress Indicator
            LevelProgressIndicator(
                level = level,
                progress = progress,
                theme = theme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom controls / Navigation Bar style
            BottomNavBar(
                theme = theme,
                isPlaying = gameState == GameState.PLAYING,
                onPlayClick = {
                    if (gameState != GameState.PLAYING) {
                        viewModel.startGame()
                    } else {
                        viewModel.resetGame()
                        viewModel.gameState = GameState.MENU
                    }
                },
                onLeaderboardClick = { showLeaderboard = true },
                onThemeClick = { showThemeSelector = true }
            )
        }

        // Leaderboard Sheet Dialog
        if (showLeaderboard) {
            LeaderboardDialog(
                topScores = topScores,
                theme = theme,
                onDismiss = { showLeaderboard = false },
                onClear = { viewModel.clearHistory() }
            )
        }

        // Theme Selector Dialog
        if (showThemeSelector) {
            ThemeSelectorDialog(
                currentTheme = theme,
                onThemeSelected = {
                    viewModel.currentTheme = it
                    showThemeSelector = false
                },
                onDismiss = { showThemeSelector = false }
            )
        }
    }
}

@Composable
fun HeaderSection(
    currentScore: Int,
    personalBestScore: Int,
    theme: CyberTheme,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onInfoClick: () -> Unit,
    gameState: GameState,
    onPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Current Score and Highest Score side-by-side or styled elegantly
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.activeTile,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(0.7f)
                )
                Text(
                    text = String.format("%,d", currentScore),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = theme.glowColor,
                            blurRadius = 12f
                        )
                    )
                )
            }

            // Vertical divider decoration
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Column {
                Text(
                    text = "HIGHEST SCORE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(0.7f)
                )
                Text(
                    text = String.format("%,d", personalBestScore),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.85f),
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = theme.glowColor,
                            blurRadius = 6f
                        )
                    )
                )
            }
        }

        // Right Row: Interactive immersive design controls (badge, JD avatar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Immersive decorative audio toggle badge (Cyan square container style from mockup)
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(40.dp)
                    .background(theme.activeTile.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, theme.activeTile.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .testTag("sound_toggle")
            ) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.Outlined.MusicNote else Icons.Outlined.MusicOff,
                    contentDescription = "Toggle Sound",
                    tint = theme.activeTile,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (gameState == GameState.PLAYING) {
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(theme.accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause Game",
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clickable gradient avatar ("JD" custom profile element from the mockup) which opens Theme Selector
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF007F), // Neon Pink
                                theme.accent,      // Cyber theme accent
                                theme.activeTile,  // Cyber theme active color
                                Color(0xFFFF007F)
                            )
                        )
                    )
                    .clickable { onInfoClick() }
                    .padding(2.dp)
                    .testTag("info_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF0A0C10)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GameGrid(
    viewModel: CyberTilesViewModel,
    containerHeight: Dp,
    rowHeight: Dp,
    bottomOffset: Dp,
    theme: CyberTheme
) {
    val activeRows = viewModel.activeRows
    val scrollOffset = viewModel.scrollOffset
    val nextExpectedIndex = viewModel.nextExpectedIndex

    Box(modifier = Modifier.fillMaxSize()) {
        // Render 3 grid lines to divide screen into 4 columns
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(4) { colIndex ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind {
                            if (colIndex < 3) {
                                val x = size.width
                                drawLine(
                                    color = theme.gridLine,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 2f
                                )
                            }
                        }
                )
            }
        }

        // Render game tiles
        activeRows.forEach { row ->
            // Calculate bottom coordinate of the row
            val rowBottomY = containerHeight - bottomOffset + scrollOffset.dp - (row.id * rowHeight.value).dp
            val rowTopY = rowBottomY - rowHeight

            // Check if row is visible in container viewport
            if (rowBottomY > 0.dp && rowTopY < containerHeight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .offset(y = rowTopY)
                ) {
                    repeat(4) { colIndex ->
                        val isActiveColumn = row.activeColumn == colIndex
                        val isTapped = row.isTapped && row.tappedColumn == colIndex
                        val isMissed = row.tappedColumn == colIndex && !row.isTapped

                        // Tile cell container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                        ) {
                            var cellX by remember { mutableStateOf(0f) }
                            var cellY by remember { mutableStateOf(0f) }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { layoutCoordinates ->
                                        val position = layoutCoordinates.localToRoot(Offset.Zero)
                                        cellX = position.x + layoutCoordinates.size.width / 2f
                                        cellY = position.y + layoutCoordinates.size.height / 2f
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (isActiveColumn) {
                                            if (isTapped) {
                                                Modifier.background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            theme.tappedTile.copy(alpha = 0.5f),
                                                            theme.tappedTile.copy(alpha = 0.2f)
                                                        )
                                                    )
                                                ).border(2.dp, theme.tappedTile, RoundedCornerShape(16.dp))
                                            } else {
                                                Modifier
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                theme.activeTile,
                                                                theme.activeTile.copy(alpha = 0.7f)
                                                            )
                                                        )
                                                    )
                                                    .border(2.dp, theme.accent, RoundedCornerShape(16.dp))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        viewModel.handleTileTap(
                                                            row.id,
                                                            colIndex,
                                                            cellX,
                                                            cellY
                                                        )
                                                    }
                                            }
                                        } else {
                                            if (isMissed) {
                                                // Failed wrong-tap
                                                Modifier
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFFFF1744),
                                                                Color(0xFFB71C1C)
                                                            )
                                                        )
                                                    )
                                                    .border(2.dp, Color.Red, RoundedCornerShape(16.dp))
                                            } else {
                                                Modifier
                                                    .background(Color.White.copy(alpha = 0.03f))
                                                    .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.05f),
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        viewModel.handleTileTap(
                                                            row.id,
                                                            colIndex,
                                                            cellX,
                                                            cellY
                                                        )
                                                    }
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActiveColumn && !isTapped) {
                                    // Make active tiles look beautiful & cyber
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(4.dp)
                                                .background(Color.White.copy(alpha = 0.6f), CircleShape)
                                        )
                                        if (row.id == 0L) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "START",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                } else if (isTapped) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Tapped",
                                        tint = theme.tappedTile,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else if (isMissed) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Missed",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticlesLayer(particles: List<TapParticle>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val progress = (particle.age / particle.maxLifetime).coerceIn(0f, 1f)
            val alpha = 1f - progress
            val currentColor = particle.color.copy(alpha = alpha)

            when (particle.type) {
                ParticleType.SHOCKWAVE -> {
                    // Growing ring
                    val startRadius = particle.size
                    val targetRadius = startRadius * 6f
                    val currentRadius = startRadius + (targetRadius - startRadius) * progress
                    val strokeWidth = 3f * (1f - progress)
                    if (strokeWidth > 0f) {
                        drawCircle(
                            color = currentColor,
                            radius = currentRadius,
                            center = Offset(particle.x, particle.y),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
                ParticleType.CIRCLE -> {
                    // Outer neon glow
                    drawCircle(
                        color = currentColor.copy(alpha = alpha * 0.25f),
                        radius = particle.size * 2f * (1f - (progress * 0.5f)),
                        center = Offset(particle.x, particle.y)
                    )
                    // Core solid circle
                    drawCircle(
                        color = currentColor,
                        radius = particle.size * (1f - progress),
                        center = Offset(particle.x, particle.y)
                    )
                }
                ParticleType.SQUARE -> {
                    val currentSize = particle.size * (1f - progress)
                    if (currentSize > 0f) {
                        rotate(degrees = particle.initialRotation, pivot = Offset(particle.x, particle.y)) {
                            drawRect(
                                color = currentColor,
                                topLeft = Offset(particle.x - currentSize / 2, particle.y - currentSize / 2),
                                size = androidx.compose.ui.geometry.Size(currentSize, currentSize)
                            )
                        }
                    }
                }
                ParticleType.GLOW_STAR -> {
                    val length = particle.size * 1.5f * (1f - progress)
                    val width = 2f * (1f - progress)
                    if (length > 0f) {
                        rotate(degrees = particle.initialRotation, pivot = Offset(particle.x, particle.y)) {
                            // Horizontal sparkle line
                            drawLine(
                                color = currentColor,
                                start = Offset(particle.x - length, particle.y),
                                end = Offset(particle.x + length, particle.y),
                                strokeWidth = width
                            )
                            // Vertical sparkle line
                            drawLine(
                                color = currentColor,
                                start = Offset(particle.x, particle.y - length),
                                end = Offset(particle.x, particle.y + length),
                                strokeWidth = width
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelProgressIndicator(
    level: Int,
    progress: Float,
    theme: CyberTheme
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEON CITY RUSH",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Text(
                text = "LEVEL $level",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = theme.accent,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = theme.glowColor,
                        blurRadius = 8f
                    )
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                .padding(2.dp)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(300, easing = LinearOutSlowInEasing),
                label = "progress"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFA855F7), // Purple
                                theme.accent,
                                theme.tappedTile
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun BottomNavBar(
    theme: CyberTheme,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onThemeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        // MD3 style Cyber Navigation Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161B22))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collection / Theme tab
            IconButton(
                onClick = onThemeClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("themes_tab")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Themes",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Spacer for floating action button
            Spacer(modifier = Modifier.width(76.dp))

            // Ranking / High Scores tab
            IconButton(
                onClick = onLeaderboardClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("ranking_tab")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Scores",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Scores",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Beautiful neon circular center play button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            theme.accent,
                            theme.activeTile
                        )
                    )
                )
                .border(4.dp, Color(0xFF0A0C10), CircleShape)
                .clickable { onPlayClick() }
                .testTag("play_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Restart" else "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    tilesTapped: Int,
    level: Int,
    maxCombo: Int,
    personalBestScore: Int,
    theme: CyberTheme,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GAME OVER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF1744),
                letterSpacing = 2.sp,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Red,
                        blurRadius = 15f
                    )
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SYSTEM GLITCH DETECTED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BoxBorder(1.dp, theme.activeTile.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRow(label = "FINAL SCORE", value = String.format("%,d", score), valueColor = Color.White)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "TILES TAPPED", value = tilesTapped.toString(), valueColor = theme.accent)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "BEST COMBO", value = maxCombo.toString(), valueColor = theme.activeTile)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "SPEED LEVEL", value = "LV. $level", valueColor = theme.activeTile)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "PERSONAL BEST", value = String.format("%,d", personalBestScore), valueColor = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Replay Button
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(2.dp, theme.accent, RoundedCornerShape(16.dp))
                    .testTag("restart_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    theme.accent.copy(alpha = 0.2f),
                                    theme.activeTile.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INITIALIZE RUSH",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlay(
    score: Int,
    tilesTapped: Int,
    maxCombo: Int,
    gameDurationSeconds: Float,
    level: Int,
    theme: CyberTheme,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .testTag("pause_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GAME PAUSED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = theme.accent,
                letterSpacing = 2.sp,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = theme.accent,
                        blurRadius = 15f
                    )
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SYSTEM SUSPENDED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BoxBorder(1.dp, theme.accent.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRow(label = "CURRENT SCORE", value = String.format("%,d", score), valueColor = Color.White)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "TILES TAPPED", value = tilesTapped.toString(), valueColor = theme.accent)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "BEST COMBO", value = maxCombo.toString(), valueColor = theme.activeTile)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "TIME ELAPSED", value = String.format("%.1fs", gameDurationSeconds), valueColor = Color.White)
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    StatRow(label = "SPEED LEVEL", value = "LV. $level", valueColor = theme.activeTile)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Resume Button
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(2.dp, theme.activeTile, RoundedCornerShape(16.dp))
                    .testTag("resume_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    theme.activeTile.copy(alpha = 0.2f),
                                    theme.accent.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESUME ENGINE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restart Button
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .testTag("restart_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REINITIALIZE",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.LightGray.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardDialog(
    topScores: List<HighScore>,
    theme: CyberTheme,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = theme.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (topScores.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("CLEAR ALL", color = Color.Red.copy(alpha = 0.8f))
                }
            }
        },
        title = {
            Text(
                text = "CYBER RANKINGS",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (topScores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO HIGH SCORES RECORDED\nTAP 'PLAY' TO INITIATE SYSTEM",
                            textAlign = TextAlign.Center,
                            color = Color.LightGray.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(topScores.take(10)) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(theme.activeTile.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${topScores.indexOf(item) + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = theme.activeTile
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Level ${item.speedLevel}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${item.tilesTapped} tiles tapped",
                                            fontSize = 10.sp,
                                            color = Color.LightGray.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("%,d", item.score),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.accent
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF161B22),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ThemeSelectorDialog(
    currentTheme: CyberTheme,
    onThemeSelected: (CyberTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.LightGray)
            }
        },
        title = {
            Text(
                text = "SELECT CYBER DECK",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CyberTheme.values().forEach { theme ->
                    val isSelected = theme == currentTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) theme.activeTile.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected) theme.activeTile else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onThemeSelected(theme) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = theme.displayName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).background(theme.activeTile, CircleShape))
                                Box(modifier = Modifier.size(12.dp).background(theme.tappedTile, CircleShape))
                                Box(modifier = Modifier.size(12.dp).background(theme.accent, CircleShape))
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Theme",
                                tint = theme.activeTile
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF161B22),
        shape = RoundedCornerShape(24.dp)
    )
}

// Custom simple Card border because standard card border is not supported or simpler to build directly
@Composable
fun BoxBorder(width: Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun ComboIndicator(
    combo: Int,
    multiplier: Int,
    timerProgress: Float,
    theme: CyberTheme,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (combo > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "comboScale"
    )

    if (combo > 0) {
        Column(
            modifier = modifier
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    alpha = animatedScale
                )
                .background(
                    Color(0xFF0A0C10).copy(alpha = 0.85f),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    theme.activeTile.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$combo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = theme.glowColor,
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = "COMBO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Multiplier Badge
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF007F), // Pink
                                    theme.activeTile
                                )
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "x$multiplier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Small Combo Decay Timer progress bar
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(timerProgress)
                        .clip(CircleShape)
                        .background(theme.activeTile)
                )
            }
        }
    }
}

@Composable
fun SynthwaveBackground(
    theme: CyberTheme,
    multiplier: Int,
    scrollPhase: Float,
    modifier: Modifier = Modifier
) {
    // Shifting color animations based on multiplier
    val sunTopColor by animateColorAsState(
        targetValue = when (multiplier) {
            1 -> theme.accent
            2 -> theme.activeTile
            3 -> lerp(theme.activeTile, theme.tappedTile, 0.5f)
            else -> Color.White
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "sunTopColor"
    )

    val sunBottomColor by animateColorAsState(
        targetValue = when (multiplier) {
            1 -> theme.activeTile
            2 -> theme.accent
            3 -> lerp(theme.tappedTile, theme.activeTile, 0.5f)
            else -> theme.activeTile
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "sunBottomColor"
    )

    val gridColor by animateColorAsState(
        targetValue = when (multiplier) {
            1 -> theme.gridLine
            2 -> lerp(theme.gridLine, theme.accent, 0.35f)
            3 -> lerp(theme.gridLine, theme.activeTile, 0.6f)
            else -> lerp(theme.gridLine, Color.White, 0.8f)
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "gridColor"
    )

    val ambientGlowColor by animateColorAsState(
        targetValue = when (multiplier) {
            1 -> Color.Transparent
            2 -> theme.glowColor.copy(alpha = 0.08f)
            3 -> theme.glowColor.copy(alpha = 0.18f)
            else -> theme.activeTile.copy(alpha = 0.28f)
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ambientGlowColor"
    )

    // Base background with ambient glow
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val horizonY = size.height * 0.52f
            
            // 1. Draw atmospheric vertical sky gradient
            val skyBrush = Brush.verticalGradient(
                colors = listOf(
                    theme.background,
                    lerp(theme.background, theme.gridLine, 0.4f),
                    theme.background
                ),
                startY = 0f,
                endY = size.height
            )
            drawRect(brush = skyBrush)

            // 2. Draw ambient radial glow centered on the horizon
            if (ambientGlowColor != Color.Transparent) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientGlowColor, Color.Transparent),
                        center = Offset(size.width / 2f, horizonY),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width / 2f, horizonY)
                )
            }

            // 3. Draw outrun neon glowing sun
            val sunRadius = size.width * 0.28f
            val sunCenter = Offset(size.width / 2f, horizonY - 10.dp.toPx())
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(sunTopColor, sunBottomColor),
                    startY = sunCenter.y - sunRadius,
                    endY = sunCenter.y + sunRadius
                ),
                radius = sunRadius,
                center = sunCenter
            )

            // 4. Draw outrun sun slicing grid gaps (horizontal gaps cutting the sun)
            val numBars = 6
            for (b in 0 until numBars) {
                // Bars get exponentially thicker towards the bottom
                val barHeight = 4.dp.toPx() + b * 5.dp.toPx()
                // Gaps are positioned dynamically from horizon going upwards
                val barY = horizonY - 14.dp.toPx() - b * (18.dp.toPx() + b * 2f)
                if (barY > sunCenter.y - sunRadius) {
                    drawRect(
                        color = theme.background,
                        topLeft = Offset(sunCenter.x - sunRadius - 10f, barY),
                        size = androidx.compose.ui.geometry.Size(sunRadius * 2 + 20f, barHeight)
                    )
                }
            }

            // 5. Draw glowing retro synthwave mountain range on horizon
            val mountainPath1 = Path().apply {
                moveTo(0f, horizonY)
                lineTo(size.width * 0.12f, horizonY - 35.dp.toPx())
                lineTo(size.width * 0.24f, horizonY - 12.dp.toPx())
                lineTo(size.width * 0.38f, horizonY - 55.dp.toPx())
                lineTo(size.width * 0.52f, horizonY - 20.dp.toPx())
                lineTo(size.width * 0.65f, horizonY - 45.dp.toPx())
                lineTo(size.width * 0.80f, horizonY - 15.dp.toPx())
                lineTo(size.width * 0.90f, horizonY - 30.dp.toPx())
                lineTo(size.width, horizonY)
                close()
            }
            drawPath(
                path = mountainPath1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        theme.gridLine.copy(alpha = 0.9f),
                        theme.background.copy(alpha = 0.95f)
                    ),
                    startY = horizonY - 55.dp.toPx(),
                    endY = horizonY
                )
            )
            // Stroke the mountain tops with neon glow
            drawPath(
                path = mountainPath1,
                color = gridColor.copy(alpha = 0.4f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 6. Draw perspective road lines (3D grid scrolling towards camera)
            // Perspective columns radiating from center horizon
            val columns = 10
            for (i in 0..columns) {
                val fraction = i.toFloat() / columns
                val bottomX = fraction * size.width
                // Radial perspective convergence lines
                drawLine(
                    color = gridColor.copy(alpha = 0.35f),
                    start = Offset(size.width / 2f, horizonY),
                    end = Offset(bottomX, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Exponentially spaced horizontal lines for depth illusion
            for (i in 0..11) {
                // We use scrollPhase to animate moving horizontal lines forward
                val t = (i.toFloat() - scrollPhase) / 11f
                if (t >= 0f && t <= 1f) {
                    val scale = t * t // perspective mapping
                    val lineY = horizonY + scale * (size.height - horizonY)
                    drawLine(
                        color = gridColor.copy(alpha = 0.45f * scale),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = (1.dp.toPx() + 2.5.dp.toPx() * scale)
                    )
                }
            }
        }
    }
}
