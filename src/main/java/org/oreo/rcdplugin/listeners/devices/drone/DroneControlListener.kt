package org.oreo.rcdplugin.listeners.devices.drone

import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.objects.*


class DroneControlListener(private val plugin: RCD_plugin): Listener {

    /**
     * This function handles the player entering "control mode"
     */
    @EventHandler
    fun droneControl(e: PlayerInteractEvent){
        val player = e.player

        if (!ItemManager.isHoldingCustomItem(player, ItemManager.droneControl!!) ||
            player.gameMode == GameMode.SPECTATOR) {
            return
        }

        val controller = player.inventory.itemInMainHand

        //Get the corresponding turret via the unique ID that's in the items lore
        val droneID = controller.itemMeta.lore?.get(1).toString()

        val drone = DeviceBase.getDeviceFromID(droneID) as Drone?


        if (RCD_plugin.inCooldown.contains(player)) {
            //This cooldown is used to prevent any spamming that could result in exploits
            player.sendMessage("§c You are in cooldown")
            e.isCancelled = true
            return
        }

        if (drone == null) {
            player.sendMessage("§c Drone does not exist")
            controller.amount -= 1
            player.world.playSound(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            return
        }

        drone.addController(player)
    }

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

        if (controller.deviceType != DeviceEnum.DRONE) {
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
     * is properly removed from controlling any device they might have been interacting with.
     *
     * @param e the PlayerQuitEvent that is triggered when a player leaves the server
     */
    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val player = e.player

        DeviceBase.removePlayerFromControlling(player)
    }

}