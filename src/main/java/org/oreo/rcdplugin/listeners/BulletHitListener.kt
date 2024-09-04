package org.oreo.rcdplugin.listeners

import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.RCD_plugin

class BulletHitListener(private val plugin: RCD_plugin) : Listener {

    // To track the particle tasks associated with bullets
    private val bulletTasks = mutableMapOf<Snowball, BukkitRunnable>()

    @EventHandler
    fun bulletLand(e: EntityDamageByEntityEvent) {
        if (e.damager !is Snowball) {
            return
        }
        val projectile = e.damager as Snowball
        val damaged = e.entity as? LivingEntity ?: return

        if (!RCD_plugin.currentBullets.contains(projectile)) {
            return
        }

        damaged.damage(10.0)
        bulletTasks[projectile]?.cancel()
        bulletTasks.remove(projectile)
    }

    @EventHandler
    fun bulletHit(e: ProjectileHitEvent) {
        val projectile = e.entity
        if (projectile !is Snowball) {
            return
        }

        if (!RCD_plugin.currentBullets.contains(projectile)) {
            return
        }

        bulletTasks[projectile]?.cancel()
        bulletTasks.remove(projectile)
    }

    @EventHandler
    fun bulletLaunch(e: ProjectileLaunchEvent) {
        if (!RCD_plugin.currentBullets.contains(e.entity)) {
            return
        }
        val bullet = e.entity as Snowball
        val world = bullet.world

        val task = BulletTask(bullet, world)
        task.runTaskTimer(plugin, 0L, 7L)

        bulletTasks[bullet] = task
    }

    // Define the task as an inner class
    private inner class BulletTask(
        private val bullet: Snowball,
        private val world: World
    ) : BukkitRunnable() {
        override fun run() {
            if (bullet.isDead || !bullet.isValid) {
                this.cancel()
                bulletTasks.remove(bullet)
                return
            }
            world.spawnParticle(Particle.ELECTRIC_SPARK, bullet.location, 1, 0.0, 0.0, 0.0)
        }
    }
}