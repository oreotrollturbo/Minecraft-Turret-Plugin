package org.oreo.rcdplugin.listeners.projectiles

import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.oreo.rcdplugin.RCD_plugin

/**
 * To anyone confused as to what these listeners do they will contain everything to do with custom projectiles
 * For example : turret shots and drone bombs
 */
class ProjectileHitListener(private val plugin: RCD_plugin) : Listener {


    private val turretDamage = plugin.config.getInt("turret-damage")

    /**
     * Handles bullet deletion from the list and damaging the entity
     */
    @EventHandler
    fun bulletHit(e: ProjectileHitEvent) {
        val projectile = e.entity
        if (projectile !is Snowball || !RCD_plugin.currentBullets.contains(projectile)) return

        RCD_plugin.currentBullets.remove(projectile)

        val damaged : Entity? = e.hitEntity

        if (damaged !is LivingEntity) {
            return
        }

        damaged.health = maxOf(0.0, damaged.health - turretDamage)
    }

    /**
     * Handles all the "bombs" that land
     */
    @EventHandler
    fun bombHit(e: ProjectileHitEvent) {
        val projectile = e.entity

        if (!RCD_plugin.currentBombs.contains(projectile)) return

        val world = projectile.world

        world.createExplosion(projectile,3f, false,true)
        world.playSound(projectile.location, Sound.ENTITY_GENERIC_EXPLODE,1f,1f)
    }
}
