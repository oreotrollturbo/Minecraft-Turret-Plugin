package org.oreo.rcdplugin.listeners.controller

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.Turret


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

        e.isCancelled = true

        controller.killPlayer()
    }

    /**
     * Makes sure controller villagers don't move
     * I couldn't use NoAI because that disables falling which is something I want for fall damage
     * This listener is paper specific
     */
    @EventHandler
    fun onEntityTarget(event: EntityMoveEvent) {
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
    @EventHandler (priority = EventPriority.HIGHEST)
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {

        val player = event.player

        if (Controller.getControllerFromPlayer(player) == null){
            return
        }

        event.isCancelled = true
        Turret.removePlayerFromControlling(player)

    }
}
