package org.oreo.rcdplugin.data

import org.oreo.rcdplugin.RCD_plugin

/**
 * Stores the turrets configurations
 */
data class TurretConfig(
    val maxHealth: Double,
    val turretSelfDestructEnabled: Boolean,
    val selfDestructPower: Float,
    val controllerHeightOffset : Double,
    val minTurretPitch : Double,
    val maxTurretPitch : Double,
    val shootCooldown : Int
) {
    companion object {
        /**
         * Function to get all the turrets configurations neatly packed in a data Class
         */
        fun fromConfig(plugin: RCD_plugin): TurretConfig {
            return TurretConfig(
                maxHealth = plugin.config.getDouble("turret-health"),
                turretSelfDestructEnabled = plugin.config.getBoolean("turret-explode"),
                selfDestructPower = plugin.config.getInt("turret-explode-strength").toFloat(),
                controllerHeightOffset = plugin.config.getDouble("controller-height-offset"),
                minTurretPitch = plugin.config.getDouble("min-turret-pitch"),
                maxTurretPitch = plugin.config.getDouble("max-turret-pitch"),
                shootCooldown = plugin.config.getInt("turret-cooldown")
            )
        }
    }
}