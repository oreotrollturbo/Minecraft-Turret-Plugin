package org.oreo.rcdplugin.listeners.devices.drone

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.*


class DroneControlListener(private val plugin: RCD_plugin): Listener {

    /**
     * Handles the player movement when they are controlling a drone. If the player attempts
     * to move into a solid block, the event is cancelled to prevent the movement.
     *
     * @param e the PlayerMoveEvent that is triggered when a player moves
     */
    @EventHandler
    fun playerMoveWhileControlling(e: PlayerMoveEvent){
        val player = e.player

        val controller = Controller.getControllerFromPlayer(player) ?: return

        if (controller.deviceType != DeviceEnum.DRONE || e.from.block == e.to.block) {
            return
        }

        val newLoc = e.to

        if (newLoc.block.isSolid){
            e.isCancelled = true
        }
    }


    /**
     * Handles the event when a player leaves the server.
     *
     * This method is triggered when a player quits the server and ensures that the player
     * is properly removed from controlling any device they might have been Controlling.
     *
     * @param e the PlayerQuitEvent that is triggered when a player leaves the server
     */
    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val player = e.player

        DeviceBase.removePlayerFromControlling(player)
    }

    /**
     * When a player controlling a drone stops flying it removes them
     */
    @EventHandler
    fun dronePlayerStopFlying(e:PlayerToggleFlightEvent){
        val player = e.player
        val controller = Controller.getControllerFromPlayer(player) ?: return

        if (controller.deviceType != DeviceEnum.DRONE) return

        val device = DeviceBase.getDeviceFromID(controller.deviceId) ?: return

        device.removeController()
    }

}