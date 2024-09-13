package org.oreo.rcdplugin.listeners

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
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
            val turretID = controller.itemMeta.lore?.get(1).toString()
            val turret = BasicTurret.getTurretFromID(turretID)

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

        if (!RCD_plugin.controllingTurret.contains(player)){ // Make sure the player is controlling a turret
            return
        }

        if (e.from.z != e.to.z || e.from.x != e.to.x || e.from.y < e.to.y){
            // If the player is moving up stop it
            e.isCancelled = true
            return

        } else if (e.from.y > e.to.y){
            //Detecting downward movement which equates to shifting
            e.isCancelled = false
            //Teleport the player to their original location
            BasicTurret.removePlayerFromControlling(player)
        }
    }

    /**
     * This event is paper specific that's why the plugin only works on paper servers and not spigot
     * I am making sure the player doesn't spectate an entity while in a turret, so instead we force the player out
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    fun onPlayerSpectate(event: PlayerStartSpectatingEntityEvent) {

        val player = event.player
        if (RCD_plugin.controllingTurret.contains(player)) { //if the player is controlling the turret
            event.isCancelled = true

            //Exit the turret if the player tries to spectate anything
            BasicTurret.removePlayerFromControlling(player)
        }
    }

    /**
     * Makes sure a player is teleported back to the correct location when they leave the server
     */
    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val player = e.player

        BasicTurret.removePlayerFromControlling(player)
    }

}