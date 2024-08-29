package org.oreo.rcdplugin.listeners

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.BasicTurret

class TurretControlListener: Listener {

    /**
     * Placeholder for the remote controls functionality
     */
    @EventHandler
    fun turretControl(e: PlayerInteractEvent){
        val act = e.action

        // If the player doesn't right-click return
        if (!(act == Action.RIGHT_CLICK_AIR || act == Action.RIGHT_CLICK_BLOCK)){
            return
        }

        val player = e.player

        if (ItemManager.isHoldingTurretControl(player)){

            val controller = player.inventory.itemInMainHand

            val turretID = controller.itemMeta.lore?.get(1)

            val turret = BasicTurret.getTurretFromID(turretID.toString())

            if (turret != null) {
                // This has to be BEFORE teleporting so the players location is saved properly
                RCD_plugin.controllingTurret.put(player,player.location)
                player.teleport(turret.main.location.clone().add(0.0,0.4,0.0))
                player.gameMode = GameMode.SPECTATOR
            } else{
                player.sendMessage("§c Turret does not exist")
            }
        }
    }

    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){ //TODO make the turret move
        val player = e.player

        if (!RCD_plugin.controllingTurret.contains(player)){
            return
        }

        if (e.from.z != e.to.z || e.from.x != e.to.x || e.from.y < e.to.y){
            e.isCancelled = true
            return
        } else if (e.from.y > e.to.y){
            RCD_plugin.controllingTurret.get(player)?.let { player.teleport(it) }
            player.gameMode = GameMode.SURVIVAL
            RCD_plugin.controllingTurret.remove(player)
        }
    }

    @EventHandler
    fun turretPlayerSpecateEntity(e:PlayerStartSpectatingEntityEvent){ //TODO fix this 
        val player = e.player
        if (player.gameMode == GameMode.SPECTATOR && RCD_plugin.controllingTurret.contains(player)){
            player.sendMessage("stopped")
            //e.isCancelled = true
        }
    }


}