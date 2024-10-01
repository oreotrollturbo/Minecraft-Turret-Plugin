package org.oreo.rcdplugin.data

import org.oreo.rcdplugin.RCD_plugin

/**
 * Stores the turrets configurations
 */
data class DroneConfigs(
    val maxHealth: Double,
    val teleportOffset : Double,
    val hitboxOffset : Double,
) {
    companion object {
        /**
         * Function to get all the Drones configurations neatly packed in a data Class
         */
        fun fromConfig(plugin: RCD_plugin): DroneConfigs {
            return DroneConfigs(
                maxHealth = plugin.config.getDouble("drone-health"),
                teleportOffset = plugin.config.getDouble("drone-offset"),
                hitboxOffset = plugin.config.getDouble("drone-hitbox-offset"),
            )
        }
    }
}