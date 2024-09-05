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
    fun turretPlaced(e:PlayerInteractEvent){
        val act = e.action

        if (act != Action.RIGHT_CLICK_BLOCK){ //Make sure the player is right-clicking a block
            return
        }

        val  player = e.player
        if (!ItemManager.isHoldingBasicTurret(player)){ //Make sure the player is holding the turret item
            return
        }

        //TODO remove this now that I have migrated to a controller system
        for (turret in RCD_plugin.activeTurrets.values){
            if (turret.controler == player){
                player.sendMessage("§c You already own a turret")
                return
            }
        }

        player.inventory.itemInMainHand.amount -= 1   //Make sure the item is used

        val placeLocation = e.clickedBlock?.location

        if (placeLocation != null){
            BasicTurret(placeLocation, player, plugin)
        }else{
            player.sendMessage("§c Invalid place location")
        }
    }
}