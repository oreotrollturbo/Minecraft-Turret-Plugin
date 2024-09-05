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
     * This function handles the player controlling a turret
     */
    @EventHandler
    fun turretControl(e: PlayerInteractEvent){
        val player = e.player

        if (ItemManager.isHoldingTurretControl(player)){ //Make sure the player is holding a controller

            val controller = player.inventory.itemInMainHand

            //Get the corresponding turret via the unique ID that's in the items lore
            val turretID = controller.itemMeta.lore?.get(1)
            val turret = BasicTurret.getTurretFromID(turretID.toString())

            if (RCD_plugin.inCooldown.contains(player)){
                //This cooldown is used to prevent any spamming that could result in exploits
                player.sendMessage("§c You are in cooldown")
                e.isCancelled = true
                return
            }

            if (turret != null) {
                turret.addController(player)
            } else{
                player.sendMessage("§c Turret does not exist")
            }
        }
    }

    /**
     * This makes sure the player cant move when in spectator mode for controlling a turret
     * Also handles the player exiting a turret via shifting
     */
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
            //Detecting downard movement which equates to shifting
            e.isCancelled = false
            //Teleport the player to their original location
            RCD_plugin.controllingTurret.get(player)?.keys?.let { player.teleport(it.first()) }

            player.sendMessage("Exited a turret") //This was originally a debug message but I might keep it
            RCD_plugin.controllingTurret.remove(player)
            player.gameMode = GameMode.SURVIVAL //TODO make it so that it returns the player to the gamemode they where in
        }
    }

    /**
     * This event is paper specific that's why the plugin only works on paper servers and not spigot
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {

        val player = event.player
        if (RCD_plugin.controllingTurret.contains(player)) { //if the player is controlling the turret
            event.isCancelled = true

            //Exit the turret if the player tries to spectate anything
            RCD_plugin.controllingTurret.get(player)?.keys?.let { player.teleport(it.first()) }

            player.sendMessage("Exited a turret") //Debug message might remove
            RCD_plugin.controllingTurret.remove(player)
            player.gameMode = GameMode.SURVIVAL
        }
    }
}