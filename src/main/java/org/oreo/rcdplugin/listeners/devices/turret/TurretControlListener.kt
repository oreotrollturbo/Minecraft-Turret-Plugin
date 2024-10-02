package org.oreo.rcdplugin.listeners.devices.turret

import org.bukkit.GameMode
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
import org.oreo.rcdplugin.objects.DeviceEnum
import org.oreo.rcdplugin.objects.Turret


class TurretControlListener(private val plugin: RCD_plugin): Listener {

    /**
     * This makes sure the player cant move when in spectator mode for controlling a turret
     * Also handles the player exiting a turret via shifting
     */
    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){
        val player = e.player

        if (!Controller.isControllingDevice(player) ||
            (Controller.getControllerFromPlayer(player)?.deviceType) != DeviceEnum.TURRET
        ){ // Make sure the player is controlling a turret
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
     * Makes sure the controller is removed if the player leaves the server while controlling
     */
    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val player = e.player

        DeviceBase.removePlayerFromControlling(player)
    }

}