package org.oreo.rcdplugin.listeners.devices.general


import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.utils.Utils

class DeviceDamageListener : Listener {

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

        player.sendMessage("punched a device")

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        for (device in RCD_plugin.activeDevices.values){

            if (device.main == armorStand){
                device.handleMeleeHit(player, e.damage)
                return
            }
        }

    }

    @EventHandler
    fun deviceDamaged(e : EntityDamageByEntityEvent){

        val entity = e.entity

        if (!DeviceBase.hasDeviceMetadata(entity) || e.damager is Player) {
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        if (entity is Projectile && entity.shooter is Player) {
            Utils.sendToAllPlayers("Damaged by projectile")
        } else {
            Utils.sendToAllPlayers("Damaged directly")
        }


        for (device in RCD_plugin.activeDevices.values){
            if (device.main == entity){
                device.damageDevice(e.damage)
                return
            }
        }
    }

}