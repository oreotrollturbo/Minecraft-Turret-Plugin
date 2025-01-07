package org.oreo.rcdplugin.listeners.devices.general


import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.oreo.rcdplugin.objects.PermanentDeviceBase

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

        val device = PermanentDeviceBase.getDeviceFromEntityOrNull(armorStand) ?: return

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        device.handleMeleeHit(player, e.damage)
        return
    }

    /**
     * Checks for any device damage except for direct player damage
     */
    @EventHandler
    fun deviceDamaged(e : EntityDamageEvent){ //TODO make devices be damaged by explosions

        val entity = e.entity

        if (e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || e.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ) {
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        val device = PermanentDeviceBase.getDeviceFromEntityOrNull(entity) ?: return


        device.damageDevice(e.damage)
        return

    }

}