package org.oreo.rcdplugin.listeners

import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.RCD_plugin

class BulletHitListener(private val plugin: RCD_plugin) : Listener {

    @EventHandler
    fun bulletLand(e:EntityDamageByEntityEvent){

        if (e.damager !is Snowball){
            return
        }
        val projectile = e.damager as Snowball
        val damaged = e.entity as LivingEntity

        if (!RCD_plugin.currentBullets.contains(projectile)){
            return
        }
         damaged.damage(10.0)
    }

    @EventHandler
    fun bulletLaunch(e:ProjectileLaunchEvent){
        if (!RCD_plugin.currentBullets.contains(e.entity)){
            return
        }
        val bullet = e.entity as Snowball
        val world = bullet.world

        object : BukkitRunnable() {
            override fun run() {
                if (bullet.isDead || bullet.isValid){
                    cancel()
                }
                world.spawnParticle(Particle.ELECTRIC_SPARK,bullet.location,1,0.0,0.0,0.0)
            }
        }.runTaskTimer(plugin, 0L, 7L)
    }
}