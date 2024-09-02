package org.oreo.rcdplugin.listeners

import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.turrets.BasicTurret

class TurretInterationListener : Listener {

    @EventHandler
    fun turretBreak(e: EntityDamageByEntityEvent){ //TODO add detection to actually break the turret

        if (e.entity !is ArmorStand){
            return
        }

        val armorStand = e.entity as ArmorStand

        if (BasicTurret.hasTurretMetadata(armorStand)){
            // The sendMessage function works on all entities apparently so there is no need to check if it's a player
            // No stacktrace will be sent
            e.damager.sendMessage("§c You cannot damage turrets")
            e.isCancelled = true
        }
    }


}
