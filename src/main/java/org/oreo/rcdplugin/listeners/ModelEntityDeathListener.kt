package org.oreo.rcdplugin.listeners

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.objects.Turret


class ModelEntityDeathListener(private val plugin : RCD_plugin) : Listener{

    /**
     * The turret instead of damaging an entity it sets its health which apparently bypasses ModelEngine's
     checks to stop 'model entities' from dying thus deleting the model without deleting the activeModel , which caused
     me A LOT of headaches .
     * This function checks if an entity is an armor stand and weather it is a ModelEngine entity
     if it is it prevents it from dying
     */
    @EventHandler
    fun modelArmorStandDeath(e:EntityDeathEvent){

        val entity = e.entity

        if (entity !is ArmorStand || ModelEngineAPI.getModeledEntity(entity) == null ||
            ModelEngineAPI.getModeledEntity(entity).base.original !is ArmorStand){
            return
        }

        val turret = Turret.getTurretFromArmorStand(ModelEngineAPI.getModeledEntity(entity).base.original as ArmorStand)
            ?: return

        turret.damageTurret(20.0) //TODO after testing move this to a different listener
        e.isCancelled = true
    }
}