package com.cybermusicrush.powerup.types

import com.cybermusicrush.game.GameEngine
import com.cybermusicrush.powerup.PowerUp
import com.cybermusicrush.powerup.PowerUpType

/**
 * Shield Power-Up protects the player's combo chain and health from a missed note.
 * It can expire either via its time duration or upon absorbing a single mistake.
 */
class ShieldPowerUp(durationMs: Long = 8000) : PowerUp(PowerUpType.SHIELD, durationMs) {

    override fun onActivate(engine: GameEngine) {
        // Enable the shield flag in your game engine's health or combo system
        engine.comboSystem.isShieldActive = true
        
        // Optional: Trigger a visual effect update via the engine
        // engine.uiController.showShieldVisual(true)
    }

    override fun onUpdate(deltaTimeMs: Long, engine: GameEngine) {
        // First, check if the engine tells us the shield was broken by a missed note
        if (engine.comboSystem.hasShieldBeenBroken()) {
            expire(engine)
            return
        }

        // Otherwise, let the normal time duration countdown happen
        super.onUpdate(deltaTimeMs, engine)
    }

    override fun expire(engine: GameEngine) {
        super.expire(engine)
        
        // Disable the shield flag so the player takes damage/loses combos normally again
        engine.comboSystem.isShieldActive = false
        engine.comboSystem.resetShieldBreakFlag() // Clean up state
        
        // Optional: Turn off visual effects
        // engine.uiController.showShieldVisual(false)
    }
}
