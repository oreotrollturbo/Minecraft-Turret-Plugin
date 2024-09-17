package org.oreo.rcdplugin.listeners

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.getOverWorld
import org.oreo.rcdplugin.data.TurretSaveData
import org.oreo.rcdplugin.turrets.Turret

class DeviceSpawnEvent(private val plugin : RCD_plugin) : Listener{

    @EventHandler
    fun playerLoadChunk(e:ChunkLoadEvent){

        val turetsToDelete = ArrayList<TurretSaveData>()

        //e.chunk.entities.contains(instanceof Player)


        for (turretDelete in turetsToDelete){
            plugin.turretsToLoad.remove(turretDelete)
        }
    }

}