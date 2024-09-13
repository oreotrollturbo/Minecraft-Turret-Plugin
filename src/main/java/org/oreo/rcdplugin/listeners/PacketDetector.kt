package org.oreo.rcdplugin.listeners

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.turrets.Turret

class PacketDetector(private val plugin: JavaPlugin) : PacketListener {
    /**
     * Some things cannot be detected with regular bukkit listeners
     * In this case I want to detect right-clicking in spectator mode
     * This is why I use the packeEvents library
     */
    override fun onPacketReceive(e: PacketReceiveEvent) {
        if (e.user.uuid == null) { //Make sure its sent by a player
            return
        }

        val player = Bukkit.getPlayer(e.user.uuid) // e.getPlayer doesn't work for some reason

        if ( player !== null && RCD_plugin.controllingTurret.contains(player)) { //The player is in a turret
            if (e.packetType == PacketType.Play.Client.INTERACT_ENTITY) { //Check for right-clicking

                //get the turret from the player
                val turret = RCD_plugin.controllingTurret[player]?.values?.first()?.let { Turret.getTurretFromID(it) }

                if (turret == null){
                    return
                }
                //The issue with this library is that its completely async from the main thread
                // That's why in the .shoot() function I have a bukkit task to resync it
                turret.shoot()
            }
        }
    }
}