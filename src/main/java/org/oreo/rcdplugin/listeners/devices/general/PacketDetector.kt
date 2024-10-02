package org.oreo.rcdplugin.listeners.devices.general

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.objects.*

class PacketDetector(private val plugin: JavaPlugin) : PacketListener {
    /**
     * Some things cannot be detected with regular bukkit listeners
     * In this case I want to detect right-clicking in spectator mode
     * This is why I use the packetEvents library which is way more reliable than regular listeners
     * The only reason the client even sends a right-click packet is because the turret has an invisible armorstand
     shoved inside the player spectating in a way that the spectator cant see it
     * Alternatively the ModeledEntities hitbox can be extended upwards to cover all the of players reach
     */
    override fun onPacketReceive(e: PacketReceiveEvent) {
        if (e.user.uuid == null) { //Make sure its sent by a player
            return
        }
        // e.getPlayer doesn't work for some reason
        val player = Bukkit.getPlayer(e.user.uuid) ?: return

        val controller = Controller.getControllerFromPlayer(player)
            ?:
            return

        if (e.packetType != PacketType.Play.Client.INTERACT_ENTITY) return

        val device = DeviceBase.getDeviceFromId(controller.deviceId) ?: return

        device.handleRightClick()
    }

}