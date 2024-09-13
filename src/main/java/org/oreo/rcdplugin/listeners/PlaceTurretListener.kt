package org.oreo.rcdplugin.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.BasicTurret

class PlaceTurretListener(private val plugin: RCD_plugin) : Listener {

    /**
     * Handles turret placing
     */
    @EventHandler
    fun turretPlaced(e: PlayerInteractEvent) {
        val act = e.action

        if (act != Action.RIGHT_CLICK_BLOCK) { // Make sure the player is right-clicking a block
            return
        }

        val player = e.player
        if (!ItemManager.isHoldingBasicTurret(player)) { // Make sure the player is holding the turret item
            return
        }

        val clickedBlock = e.clickedBlock
        val placeLocation = clickedBlock?.location?.add(0.5, 1.0, 0.5) // Position above the block center

        if (placeLocation != null) {
            val world = placeLocation.world

            // Check for blocks around the placement location
            if (world != null &&
                world.getBlockAt(placeLocation).isEmpty &&
                world.getBlockAt(placeLocation.add(0.0, 1.0, 0.0)).isEmpty
            ) {
                BasicTurret(placeLocation.add(0.0,-2.0,0.0), player, plugin) // Place the turret
                player.inventory.itemInMainHand.amount -= 1 // Remove the item from the player's inventory
            } else {
                player.sendMessage("§cInvalid place location: space is not clear")
            }
        } else {
            player.sendMessage("§cInvalid place location")
        }
    }
}