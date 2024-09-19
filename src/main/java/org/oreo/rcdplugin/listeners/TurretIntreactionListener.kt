package org.oreo.rcdplugin.listeners

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.Turret

class TurretInterationListener : Listener {

    /**
     * Checks for players hitting a turret
     * this handles picking up the turret and damaging it
     */
    @EventHandler
    fun turretHit(e: EntityDamageByEntityEvent){

        if (e.entity !is ArmorStand || e.damager !is Player){
            return
        }

        val armorStand = e.entity as ArmorStand
        val player = e.damager as Player

        if (!Turret.hasTurretMetadata(armorStand)){
            return
        }

        e.isCancelled = true // Cancel the event so the armor stand doesn't break

        val turret = Turret.getTurretFromArmorStand(armorStand)
            ?: //This only works for melee damage
            return

        turret.handleMeleeHit(player = player)
    }

}
