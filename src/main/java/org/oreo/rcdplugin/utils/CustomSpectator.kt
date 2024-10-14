package org.oreo.rcdplugin.utils

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Custom spectator mode I made because the vanilla one is janky and uncontrollable
 * This is used for the drone to achieve better and smoother control
 */
object CustomSpectator { //TODO make this into a proper class to store active spectators if needed

    /**
     * Enables the custom spectator for a player
     */
    fun enableCustomSpectator(player: Player) {

        player.isVisibleByDefault = false
        player.allowFlight = true
        player.isFlying = true
        player.canPickupItems = false

        player.isCollidable = false


        val effect = PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, true, false, false)
        player.addPotionEffect(effect)
    }

    /**
     * Disables the custom spectator for a player
     */
    fun disableCustomSpectator(player: Player) {

        player.isVisibleByDefault = true
        player.isFlying = false
        player.allowFlight = false
        player.canPickupItems = true

        player.isCollidable = true

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
    }
}