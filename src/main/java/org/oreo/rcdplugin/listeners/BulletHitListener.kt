package org.oreo.rcdplugin.listeners

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.RCD_plugin

class BulletHitListener : Listener {

    @EventHandler
    fun bulletLand(e:EntityDamageByEntityEvent){
        val projectile = e.damager as Snowball
        val damaged = e.entity as LivingEntity

        if (!RCD_plugin.currentBullets.contains(projectile)){
            return
        }
         damaged.damage(10.0)
    }
}