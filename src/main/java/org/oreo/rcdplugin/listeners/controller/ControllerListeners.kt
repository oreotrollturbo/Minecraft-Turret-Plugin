package org.oreo.rcdplugin.listeners.controller

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import io.papermc.paper.event.player.PlayerArmSwingEvent
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.objects.Turret
import org.oreo.rcdplugin.utils.Utils


class ControllerListeners(private val plugin: RCD_plugin) : Listener {

    /**
     * Handles the event where an entity damages a villager. If the villager has controller
     metadata, this method cancels the damage event and applies the damage to the controller's associated player.
     */
    @EventHandler
    fun onControllerHit(e: EntityDamageEvent) {

        val villager = e.entity

        if (villager !is Villager || !Controller.hasControllerMetadata(villager)) {
            return
        }

        val controller = Controller.getControllerForVillager(villager) ?: return

        e.isCancelled = true

        controller.damagePlayer(e.damage)
    }

    /**
     * Make sure the controller armorstand doesn't randomly die and kill the controller if the villager dies
     */
    @EventHandler
    fun onControllerVillagerDeath(e: EntityDeathEvent) {
        val villager = e.entity

        if (villager !is Villager || !Controller.hasControllerMetadata(villager)) {
            return
        }

        val controller = Controller.getControllerForVillager(villager) ?: return

        controller.killPlayer()
    }

    /**
     * Makes sure controller villagers don't move
     * I couldn't use NoAI because that disables falling which is something I want for fall damage
     * This listener is paper specific
     */
    @EventHandler
    fun onControllerVillagerMove(event: EntityMoveEvent) {
        val entity = event.entity

        if (event.entity !is Villager || !Controller.hasControllerMetadata(entity as Villager)) {
            return
        }

        val fromY = event.from.y
        val toY = event.to.y

        if (fromY > toY){
            return
        }

        event.isCancelled = true
    }


    /**
     * This event is paper specific that's why the plugin only works on paper servers and not spigot
     * I am making sure the player doesn't spectate an entity while in a turret, so instead we force the player out
     */
    @EventHandler (priority = EventPriority.LOWEST)
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {

        val player = event.player

        if (Controller.getControllerFromPlayer(player) == null || player.gameMode != GameMode.SPECTATOR){
            return
        }

        event.isCancelled = true
        DeviceBase.removePlayerFromControlling(player)
    }

    /**
     * Making sure the player controlling the drone cannot take damage
     */
    @EventHandler
    fun controllerPlayerTakeDamage(e:EntityDamageEvent) {
        val player = e.entity
        if (player !is Player || Controller.getControllerFromPlayer(player) == null) return

        e.isCancelled = true
    }

    /**
     * Make sure the player cant attack anything
     */
    @EventHandler
    fun controllerPlayerAttack(e:PlayerArmSwingEvent){
        val player = e.player
        if (Controller.getControllerFromPlayer(player) == null) return

        e.isCancelled = true
    }

    /**
     * I cannot stop a player from changing dimensions, so I opted for just deleting the device
     */
    @EventHandler
    fun playerChangeDimension(e:PlayerChangedWorldEvent){
        val player = e.player
        val device = Controller.getControllerFromPlayer(player)?.deviceId?.let { DeviceBase.getDeviceFromID(it) } ?: return

        Utils.sendActionBarMessage(player,"Connection lost", ChatColor.RED)
        device.deleteDevice()
    }
}
