package org.oreo.rcdplugin.listeners.devices

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.objects.DeviceEnum
import org.oreo.rcdplugin.objects.Turret

class PacketDetector(private val plugin: JavaPlugin) : PacketListener {
    /**
     * Some things cannot be detected with regular bukkit listeners
     * In this case I want to detect right-clicking in spectator mode
     * This is why I use the packeEvents library
     * The only reason the client even sends a right-click packet is because the turret has an invisible armorstand
     * shoved inside the player spectating in a way that the spectator cant see it
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

        when(e.packetType){
            PacketType.Play.Client.INTERACT_ENTITY -> handleRightClick(controller) //Check for right-clicking
        }
    }

    fun handleRightClick(controller: Controller){
        when (controller.deviceType){
            DeviceEnum.TURRET -> turretRightClick(controller)
            DeviceEnum.DRONE -> droneRightClick(controller)
        }
    }



    private fun turretRightClick(controller : Controller){
        //get the turret from the player
        val turret = DeviceBase.getDeviceFromID(controller.deviceId) as Turret

        //The issue with this library is that its completely async from the main thread
        // That's why in the .shoot() function I have a bukkit task to re-sync it
        turret.shoot()
    }

    fun droneRightClick(controller: Controller){
        //TODO not done
    }

}