package org.oreo.rcdplugin.utils

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
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
     * This function sets the metadata for any armorstand with its unique ID and other identifiers
     * The namespace "rcd" is used to understand the metadata is from this plugin
     * @param entity The armorstand we are setting the metadata of
     * @param id The unique identifier of whatever object the armorstand corresponds to
     * @param metadataKey The unique identifier that tells us what type of object it corresponds to
     */
    fun setMetadata(entity: Entity, id: String, metadataKey: String) {
        val dataContainer: PersistentDataContainer = entity.persistentDataContainer
        val key = NamespacedKey("rcd", metadataKey)
        dataContainer.set(key, PersistentDataType.STRING, id)
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