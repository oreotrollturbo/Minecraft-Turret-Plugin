package org.oreo.rcdplugin.listeners.devices.general

import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.objects.DeviceBase

class PlayerControlDevice : Listener {

    /**
     * Handles players entering a device
     */
    @EventHandler
    fun playerEnterDevice(e: PlayerInteractEvent){

        val player = e.player

        if (player.gameMode == GameMode.SPECTATOR || !ItemManager.isHoldingDeviceController(player)){
            return
        }

        val controller = player.inventory.itemInMainHand

        //Get the corresponding device via the unique ID that's in the items lore
        val deviceID = if (controller.itemMeta.lore?.get(1) != null){
            controller.itemMeta.lore?.get(1).toString()
        } else {
            return
        }

        val device = DeviceBase.getDeviceFromID(deviceID)

        if (RCD_plugin.inCooldown.contains(player)) {
            //This cooldown is used to prevent any spamming that could result in exploits
            player.sendMessage("§c You are in cooldown")
            e.isCancelled = true
            return
        }

        if (device == null) {
            player.sendMessage("§c Device does not exist")
            controller.amount -= 1
            player.world.playSound(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            return
        }

        device.addController(player)
    }

}