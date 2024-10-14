package org.oreo.rcdplugin.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team

/**
 * Custom spectator mode I made because the vanilla one is janky and uncontrollable
 * This is used for the drone to achieve better and smoother control
 */
object CustomSpectator { //TODO make this into a proper class
    /**
     * Enables the custom spectator for a player
     */
    fun enableCustomSpectator(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        var team = scoreboard.getTeam("noCollision")

        if (team == null) {
            team = scoreboard.registerNewTeam("noCollision")
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        }

        team.addEntry(player.name) // Add the player to the no-collision team

        player.isVisibleByDefault = false
        player.allowFlight = true
        player.isFlying = true
        player.canPickupItems = false


        val effect = PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, true, false, false)
        player.addPotionEffect(effect)
    }

    /**
     * Disables the custom spectator for a player
     */
    fun disableCustomSpectator(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam("noCollision")

        if (team != null && team.hasEntry(player.name)) {
            team.removeEntry(player.name) // Remove player from the team
        }

        player.isVisibleByDefault = true
        player.isFlying = false
        player.allowFlight = false
        player.canPickupItems = true
        player.removePotionEffect(PotionEffectType.INVISIBILITY)
    }
}