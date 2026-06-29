package com.cybermusicrush.powerup

import com.cybermusicrush.game.GameEngine // Assume this exists

abstract class PowerUp(
    val type: PowerUpType,
    val durationMs: Long
) {
    var isExpired: Boolean = false
        private set
        
    protected var timeRemainingMs: Long = durationMs

    // Called when the player hits the power-up note
    abstract fun onActivate(engine: GameEngine)

    // Called every frame/tick while active
    open fun onUpdate(deltaTimeMs: Long, engine: GameEngine) {
        if (isExpired) return
        timeRemainingMs -= deltaTimeMs
        if (timeRemainingMs <= 0) {
            expire(engine)
        }
    }

    // Called when the timer runs out
    open fun expire(engine: GameEngine) {
        isExpired = true
    }
}
