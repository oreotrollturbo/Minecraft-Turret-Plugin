package org.oreo.rcdplugin.listeners

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
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
                val map : MutableMap<Location,String> = mutableMapOf()

                if (turretID != null) {
                    map.put(player.location,turretID)
                }

                RCD_plugin.controllingTurret.put(player,map)
                player.gameMode = GameMode.SPECTATOR
                player.teleport(turret.main.location.clone().add(0.0,0.4,0.0))

                RCD_plugin.inCooldown.add(player)
                object : BukkitRunnable() {
                    override fun run() {
                        RCD_plugin.inCooldown.remove(player)
                    }
                }.runTaskLater(plugin, 20 * 1) // 200 ticks = 10 seconds

            } else{
                player.sendMessage("§c Turret does not exist")
            }
        }
    }

    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){ //TODO make the turret turn
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

            player.sendMessage(RCD_plugin.controllingTurret.get(player).toString())
            player.sendMessage("Exited a turret")
            RCD_plugin.controllingTurret.remove(player)
            player.gameMode = GameMode.SURVIVAL
        } else if (e.from.pitch != e.to.pitch || e.from.yaw != e.to.yaw){
            val turret = RCD_plugin.controllingTurret.get(player)?.values?.first()
                ?.let { BasicTurret.getTurretFromID(it) }

            if (turret != null) {
                turret.rotateTurret(e.to.pitch,e.to.yaw)
            }else{
                player.sendMessage("§c Turret not found ??!?!?!?!?")
            }
        }
    }


    @EventHandler
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {
        val player = event.player
        if (RCD_plugin.controllingTurret.contains(player)) {
            event.isCancelled = true
            player.sendMessage("You cant spectate when you're controlling a turret")
        }
    }
}