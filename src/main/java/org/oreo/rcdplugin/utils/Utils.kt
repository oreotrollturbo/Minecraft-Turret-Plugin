package org.oreo.rcdplugin.utils

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.oreo.rcdplugin.RCD_plugin.Companion.controllingDevice
import org.oreo.rcdplugin.objects.Controller

/**
 * This singleton is in charge of any misc functions that are used all over the plugin
 */
object Utils {

    /**
     * This function sets the metadata for the armorstand with its unique ID and other identifiers
     */
    fun setMetadata(armorStand: ArmorStand, turretID: String) {
        val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
        val key = NamespacedKey("rcd", "basic_turret")
        dataContainer.set(key, PersistentDataType.STRING, turretID)
    }

    /**
     * Gets the controller object from a player
     */
    fun getControllerFromPlayer(player : Player) : Controller?{

        for (controller in controllingDevice){
            if (controller.player == player){
                return controller
            }
        }

        return null
    }

    /**
     * Checks if a player is controlling a device
     */
    fun isControllingDevice(player: Player) : Boolean{
        return getControllerFromPlayer(player) != null
    }

    /**
     * gets the overworld if possible
     */
    fun getOverWorld() : World? {
        for (world in Bukkit.getWorlds()) {
            if (world.environment == Environment.NORMAL) {
                return world
            }
        }

        return null
    }
}