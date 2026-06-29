package com.cybermusicrush.powerup.types

import com.cybermusicrush.game.GameEngine
import com.cybermusicrush.powerup.PowerUp
import com.cybermusicrush.powerup.PowerUpType

class MultiplierPowerUp(durationMs: Long = 5000) : PowerUp(PowerUpType.MULTIPLIER, durationMs) {
    private val originalMultiplier = 1

    override fun onActivate(engine: GameEngine) {
        // Temporarily boost the engine's score multiplier
        engine.scoreSystem.currentMultiplier *= 2 
    }

    override fun expire(engine: GameEngine) {
        super.expire(engine)
        // Revert back to normal
        engine.scoreSystem.currentMultiplier /= 2
    }
}
