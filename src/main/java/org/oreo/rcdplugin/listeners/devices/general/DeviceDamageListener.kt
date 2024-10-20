package org.oreo.rcdplugin.listeners.devices.general


import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.utils.Utils

class DeviceDamageListener : Listener {

    /**
     * Checks if a player hit a device directly
     */
    @EventHandler (priority = EventPriority.LOWEST)
    fun handleMeleeHit(e: EntityDamageByEntityEvent) {

        if (e.entity !is ArmorStand || e.damager !is Player){
            return
        }

        val armorStand = e.entity
        val player = e.damager as Player

        if (!DeviceBase.hasDeviceMetadata(armorStand)){
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        for (device in RCD_plugin.activeDevices.values){

            if (device.main == armorStand){
                device.handleMeleeHit(player, e.damage)
                return
            }
        }

    }

    /**
     * Checks for any device damage except for direct player damage
     */
    @EventHandler
    fun deviceDamaged(e : EntityDamageEvent){ //TODO make devices be damaged by explosions

        val entity = e.entity

        Utils.sendToAllPlayers("Damaged")

        if (!DeviceBase.hasDeviceMetadata(entity) || e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || e.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ) {
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break


        Utils.sendToAllPlayers("Damaged device")

        for (device in RCD_plugin.activeDevices.values){
            if (device.main == entity){
                device.damageDevice(e.damage)
                return
            }
        }
    }

}