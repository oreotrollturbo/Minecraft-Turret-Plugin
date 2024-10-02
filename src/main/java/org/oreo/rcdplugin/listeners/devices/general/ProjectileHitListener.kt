package org.oreo.rcdplugin.listeners.devices.general

import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.oreo.rcdplugin.RCD_plugin

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
    fun bombHit(e: ProjectileHitEvent) { //TODO check why entity damage is terrible
        val projectile = e.entity

        if (!RCD_plugin.currentBombs.contains(projectile)) return

        val location = e.hitBlock?.location ?: return

        val world = location.world ?: return

        world.createExplosion(location,6f, false,true)
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE,1f,1f)
    }
}
