package org.oreo.rcdplugin.listeners.devices.general

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.objects.DeviceEnum

class PlaceDeviceListener(private val plugin: RCD_plugin) : Listener {

    /**
     * Handles player interaction events and places a device if certain conditions are met.
     * This function evaluates if the player has performed a right-click action on a block,
     * verifies the space for placement, checks the player's cooldown status, and places
     * the appropriate device (e.g., turret or drone) if the player is holding the correct item.
     *
     * @param e the player interaction event triggered by the player
     */
    @EventHandler
    fun devicePlaced(e: PlayerInteractEvent) {
        val act = e.action

        if (act != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = e.player

        val clickedBlock = e.clickedBlock

        // Position above the block instead of inside of it
        val placeLocation = clickedBlock?.location?.add(0.5, 1.0, 0.5)

        if (placeLocation == null){
            player.sendMessage("§cInvalid place location")
            return
        }

        val world = placeLocation.world

        if (RCD_plugin.placeCooldown.contains(player) || !isValidPlaceLocation(world,placeLocation)){
            return
        }

        when { //TODO change this too

            ItemManager.isHoldingCustomItem(player, ItemManager.turret) -> {
                DeviceBase.playerSpawnDevice(plugin = plugin , player = player , placeLocation = placeLocation ,
                    deviceType = DeviceEnum.TURRET)
            }
            ItemManager.isHoldingCustomItem(player, ItemManager.drone) -> {
                DeviceBase.playerSpawnDevice(plugin = plugin , player = player , placeLocation = placeLocation ,
                    deviceType = DeviceEnum.DRONE)
            }
            else -> {
                return
            }
        }


        player.inventory.itemInMainHand.amount -= 1 // Remove the item from the player's inventory

        //Adds a cooldown so that players don't accidentally place two devices in each-other
        RCD_plugin.placeCooldown.add(player)
        object : BukkitRunnable() {
            override fun run() {
                RCD_plugin.placeCooldown.remove(player)
            }
        }.runTaskLater(plugin, 5L) // 5 ticks should be enough
    }

    /**
     * Checks if the location is valid for devices to spawn in
     */
    private fun isValidPlaceLocation(world: World? , placeLocation : Location) : Boolean{
        return !(world == null ||
                !world.getBlockAt(placeLocation).isEmpty ||
                !world.getBlockAt(placeLocation.add(0.0, 1.0, 0.0)).isEmpty)
    }

}