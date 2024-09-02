package org.oreo.rcdplugin.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.BasicTurret

class PlaceTurretListener : Listener {

    @EventHandler
    fun turretPlaced(e:PlayerInteractEvent){
        val act = e.action

        if (act != Action.RIGHT_CLICK_BLOCK){
            return
        }

        val  player = e.player
        if (!ItemManager.isHoldingBasicTurret(player)){
            return
        }

        //Every player can have 1 turret right now
        //TODO consider migrating to turret control system
        for (turret in RCD_plugin.activeTurrets.values){
            if (turret.owner == player){
                player.sendMessage("§c You already own a turret")
                return
            }
        }

        player.inventory.itemInMainHand.amount -= 1   //Make sure the item is used

        val placeLocation = e.clickedBlock?.location

        if (placeLocation != null){
            //There has to be a way to initialise an object without creating a variable
            BasicTurret(placeLocation, player)
        }else{
            player.sendMessage("§c Invalid place location")
        }
    }
}