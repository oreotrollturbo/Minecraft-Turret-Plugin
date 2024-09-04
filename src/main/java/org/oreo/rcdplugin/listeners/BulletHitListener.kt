package org.oreo.rcdplugin.listeners

import org.bukkit.Color
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

    private val bulletTasks = mutableMapOf<Snowball, BukkitRunnable>()

    val dustOptions = Particle.DustOptions(Color.WHITE, 0.7f)

    @EventHandler
    fun bulletLand(e: EntityDamageByEntityEvent) {
        if (e.damager !is Snowball) return
        val projectile = e.damager as Snowball
        val damaged = e.entity as? LivingEntity ?: return

        if (!RCD_plugin.currentBullets.contains(projectile)) return

        damaged.health -= 10
        bulletTasks[projectile]?.cancel()
        bulletTasks.remove(projectile)
    }

    @EventHandler
    fun bulletHit(e: ProjectileHitEvent) {
        val projectile = e.entity
        if (projectile !is Snowball) return

        if (!RCD_plugin.currentBullets.contains(projectile)) return

        bulletTasks[projectile]?.cancel()
        bulletTasks.remove(projectile)
    }

    @EventHandler
    fun bulletLaunch(e: ProjectileLaunchEvent) {
        //if (!RCD_plugin.currentBullets.contains(e.entity)) return
        val bullet = e.entity as Snowball
        val world = bullet.world

        val task = createBulletTask(bullet, world)
        task.runTaskTimer(plugin, 0L, 1L)

        bulletTasks[bullet] = task
    }

    private fun createBulletTask(bullet: Snowball, world: World): BukkitRunnable {
        return object : BukkitRunnable() {
            override fun run() {
                if (bullet.isDead || !bullet.isValid) {
                    this.cancel()
                    bulletTasks.remove(bullet)
                    return
                }
                world.spawnParticle(Particle.REDSTONE, bullet.location, 1, 0.0, 0.0, 0.0,0.0,dustOptions)
            }
        }
    }
}
