package com.cybermusicrush.powerup

import com.cybermusicrush.game.GameEngine

class PowerUpManager(private val engine: GameEngine) {
    private val activePowerUps = mutableListOf<PowerUp>()

    fun activatePowerUp(powerUp: PowerUp) {
        // Optional: Check if a powerup of the same type is already active to refresh duration
        val existing = activePowerUps.find { it.type == powerUp.type }
        if (existing != null) {
            // Logic to refresh duration instead of stacking duplicates
            return
        }
        
        powerUp.onActivate(engine)
        activePowerUps.add(powerUp)
    }

    fun update(deltaTimeMs: Long) {
        val iterator = activePowerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.onUpdate(deltaTimeMs, engine)
            
            if (powerUp.isExpired) {
                iterator.remove() // Clean up expired power-ups safely
            }
        }
    }
    
    fun clearAll() {
        activePowerUps.forEach { it.expire(engine) }
        activePowerUps.clear()
    }
}
