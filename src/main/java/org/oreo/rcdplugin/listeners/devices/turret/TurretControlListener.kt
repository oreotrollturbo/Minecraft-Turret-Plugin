package org.oreo.rcdplugin.listeners.devices.turret

import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.objects.Turret


class TurretControlListener(private val plugin: RCD_plugin): Listener {

    /**
     * This function handles the player entering "control mode"
     */
    @EventHandler
    fun turretControl(e: PlayerInteractEvent){
        val player = e.player

        if (ItemManager.isHoldingCustomItem(player, ItemManager.turret!!)){

            val controller = player.inventory.itemInMainHand

            //Get the corresponding turret via the unique ID that's in the items lore
            val turretID = controller.itemMeta.lore?.get(1).toString()
            val turret = Turret.getTurretFromID(turretID)

            if (RCD_plugin.inCooldown.contains(player)){
                //This cooldown is used to prevent any spamming that could result in exploits
                player.sendMessage("§c You are in cooldown")
                e.isCancelled = true
                return
            }

            if (turret == null){
                player.sendMessage("§c Turret does not exist")
                controller.amount -= 1
                player.world.playSound(player,Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                return
            }

            turret.addController(player)

        }
    }

    /**
     * This makes sure the player cant move when in spectator mode for controlling a turret
     * Also handles the player exiting a turret via shifting
     */
    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){
        val player = e.player

        if (!Controller.isControllingDevice(player)){ // Make sure the player is controlling a turret
            return
        }

        if (e.from.z != e.to.z || e.from.x != e.to.x || e.from.y < e.to.y){
            // If the player is moving up stop it
            e.isCancelled = true
            return

        } else if (e.from.y > e.to.y){
            //Detecting downward movement which equates to shifting
            e.isCancelled = false
            //Remove the player from controlling the turret
            DeviceBase.removePlayerFromControlling(player)
        }
    }

    /**
     * Makes sure a player is teleported back to the correct location when they leave the server
     */
    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val player = e.player

        DeviceBase.removePlayerFromControlling(player)
    }

}