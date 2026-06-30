package com.cybermusicrush.powerup.types

import com.cybermusicrush.game.GameEngine
import com.cybermusicrush.powerup.PowerUp
import com.cybermusicrush.powerup.PowerUpType

/**
 * SlowMotion Power-Up temporarily scales down the speed of incoming notes,
 * giving the player more time to react during intense track sections.
 */
class SlowMotionPowerUp(
    durationMs: Long = 5000, 
    private val speedFactor: Float = 0.65f // Reduces speed to 65% of normal
) : PowerUp(PowerUpType.SLOW_MOTION, durationMs) {

    override fun onActivate(engine: GameEngine) {
        // Dynamically scale the game's scrolling/movement speed multiplier
        engine.trackSystem.speedMultiplier *= speedFactor
        
        // Optional: Apply a visual shader filter or tint to signify matrix/slow-mo state
        // engine.vfxManager.applySlowMotionFilter(true)
    }

    override fun expire(engine: GameEngine) {
        super.expire(engine)
        
        // Restore normal speed by dividing back the factor
        engine.trackSystem.speedMultiplier /= speedFactor
        
        // Optional: Remove the visual filter
        // engine.vfxManager.applySlowMotionFilter(false)
    }
}
