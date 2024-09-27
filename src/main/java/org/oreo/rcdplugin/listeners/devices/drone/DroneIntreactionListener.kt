package org.oreo.rcdplugin.listeners.devices.drone

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.objects.Drone

class DroneIntreactionListener : Listener {

    /**
     * Checks for players hitting a drone
     * this handles picking up the drone and damaging it
     */
    @EventHandler
    fun droneHit(e: EntityDamageByEntityEvent){

        if (e.entity !is ArmorStand || e.damager !is Player){
            return
        }

        val armorStand = e.entity as ArmorStand
        val player = e.damager as Player

        if (!Drone.hasDroneMetadata(armorStand)){
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        val drone = Drone.getDroneFromArmorstand(armorStand)
            ?: //This only works for melee damage
            return

        drone.handleMeleeHit(player = player)
    }

}
