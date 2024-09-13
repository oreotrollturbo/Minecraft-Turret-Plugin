package org.oreo.rcdplugin.listeners

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.turrets.BasicTurret

class TurretInterationListener : Listener {

    /**
     * For now this event only check for picking the turret up
     * In the future it will also check for the turret being damaged
     */
    @EventHandler
    fun turretBreak(e: EntityDamageByEntityEvent){

        if (e.entity !is ArmorStand || e.damager !is Player){ //Make sure it's an armorstand
            return
        }

        val armorStand = e.entity as ArmorStand
        val player = e.damager as Player

        if (!BasicTurret.hasTurretMetadata(armorStand)){ // Make sure it's a turrets metadata
            return
        }

        e.isCancelled = true // Cancel the event so the armorstand doesn't break

        val turret = BasicTurret.getTurretFromArmorStand(armorStand)

        if (turret == null || !ItemManager.isHoldingTurretControl(player)) { //This only works for melee damage
            turret?.damageTurret(10.0)
            return
        }

        val turretID = player.inventory.itemInMainHand.itemMeta.lore?.get(1)

        if (turret.id != turretID){ //Compare the ID inscribed in the item with the turrets
            player.sendMessage("§c Wrong controller")
            return
        }

        //delete the remote and drop the turret
        player.inventory.itemInMainHand.amount -= 1
        turret.dropTurret()
        player.sendMessage("Turret dropped successfully")
    }

}
