package org.oreo.rcdplugin.utils

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Custom spectator mode I made because the vanilla one is janky and uncontrollable
 * This is used for the drone to achieve better and smoother control
 */
object CustomSpectator { //This could be made into a proper class if needed

    /**
     * Enables the custom spectator for a player
     */
    fun enableCustomSpectator(player: Player) {

        player.isCustomSpectator = false

        val effect = PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, true, false, false)
        player.addPotionEffect(effect)
    }

    /**
     * Disables the custom spectator for a player
     */
    fun disableCustomSpectator(player: Player) {

        player.isCustomSpectator = false


        player.removePotionEffect(PotionEffectType.INVISIBILITY)
    }

    /**
     * Getter and setter for all the custom spectator configurations
     */
    var Player.isCustomSpectator: Boolean
        get() = !isVisibleByDefault &&
                allowFlight &&
                isFlying &&
                !canPickupItems &&
                !isCollidable
        set(enable) {
            isVisibleByDefault = !enable
            allowFlight = enable
            isFlying = enable
            canPickupItems = !enable
            isCollidable = !enable
        }
}