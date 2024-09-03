package org.oreo.rcdplugin.listeners

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.turrets.BasicTurret

class PacketDetector(private val plugin: JavaPlugin) : PacketListener {

    override public fun onPacketReceive(e: PacketReceiveEvent) {
        if (e.user.uuid == null) {
            return
        }

        val player = Bukkit.getPlayer(e.user.uuid)

        if ( player !== null && RCD_plugin.controllingTurret.contains(player)) {
            if (e.packetType == PacketType.Play.Client.INTERACT_ENTITY) {

                val turret = RCD_plugin.controllingTurret[player]?.values?.first()?.let { BasicTurret.getTurretFromID(it) }

                if (turret == null){
                    return
                }

                player.sendMessage("[RCD plugin] shoot")

                turret.shoot()
            }
        }
    }
}