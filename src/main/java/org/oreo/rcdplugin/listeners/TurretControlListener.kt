package org.oreo.rcdplugin.listeners

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.BasicTurret


class TurretControlListener(private val plugin: RCD_plugin): Listener {

    /**
     * Placeholder for the remote controls functionality
     */
    @EventHandler
    fun turretControl(e: PlayerInteractEvent){
        val player = e.player

        if (ItemManager.isHoldingTurretControl(player)){

            val controller = player.inventory.itemInMainHand

            val turretID = controller.itemMeta.lore?.get(1)

            val turret = BasicTurret.getTurretFromID(turretID.toString())

            if (RCD_plugin.inCooldown.contains(player)){
                player.sendMessage("§c You are in cooldown")
                e.isCancelled = true
                return
            }

            if (turret != null) {
                // This has to be BEFORE teleporting so the players location is saved properly
                turret.addController(player)

            } else{
                player.sendMessage("§c Turret does not exist")
            }
        }
    }

    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){
        val player = e.player

        if (!RCD_plugin.controllingTurret.contains(player)){
            return
        }

        if (e.from.z != e.to.z || e.from.x != e.to.x || e.from.y < e.to.y){

            e.isCancelled = true
            return

        } else if (e.from.y > e.to.y){

            e.isCancelled = false
            RCD_plugin.controllingTurret.get(player)?.keys?.let { player.teleport(it.first()) }

            player.sendMessage("Exited a turret")
            RCD_plugin.controllingTurret.remove(player)
            player.gameMode = GameMode.SURVIVAL
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {
        if (event.isCancelled){
            return
        }
        val player = event.player
        if (RCD_plugin.controllingTurret.contains(player)) {
            event.isCancelled = true

            RCD_plugin.controllingTurret.get(player)?.keys?.let { player.teleport(it.first()) }

            player.sendMessage("Exited a turret")
            RCD_plugin.controllingTurret.remove(player)
            player.gameMode = GameMode.SURVIVAL
        }
    }
}