package org.oreo.rcdplugin.listeners.turret

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.objects.Turret

class PlaceTurretListener(private val plugin: RCD_plugin) : Listener {

    /**
     * Handles turret placing
     */
    @EventHandler
    fun turretPlaced(e: PlayerInteractEvent) {
        val act = e.action

        if (act != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = e.player
        if (!ItemManager.isHoldingBasicTurret(player)) {
            return
        }

        val clickedBlock = e.clickedBlock

        // Position above the block instead of inside
        val placeLocation = clickedBlock?.location?.add(0.5, 1.0, 0.5)

        if (placeLocation == null){
            player.sendMessage("§cInvalid place location")
            return
        }

        val world = placeLocation.world

        // Check for blocks around the placement location
        if (world == null ||
            !world.getBlockAt(placeLocation).isEmpty ||
            !world.getBlockAt(placeLocation.add(0.0, 1.0, 0.0)).isEmpty) {
            player.sendMessage("§cInvalid place location: space is not clear")
            return
        }

        // Place the turret if the location is valid
        Turret.playerSpawnTurret(plugin = plugin , player = player , placeLocation = placeLocation)
        player.inventory.itemInMainHand.amount -= 1 // Remove the item from the player's inventory

    }

}