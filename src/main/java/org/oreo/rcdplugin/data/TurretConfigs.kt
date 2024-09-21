package org.oreo.rcdplugin.data

import org.bukkit.configuration.file.FileConfiguration

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
        fun fromConfig(config: FileConfiguration): TurretConfig {
            return TurretConfig(
                maxHealth = config.getDouble("turret-health"),
                turretSelfDestructEnabled = config.getBoolean("turret-explode", false),
                selfDestructPower = config.getInt("turret-explode-strength").toFloat(),
                controllerHeightOffset = config.getDouble("turret-controller-height-offset"),
                minTurretPitch = config.getDouble("turret-min-turret-pitch"),
                maxTurretPitch = config.getDouble("turret-max-turret-pitch"),
                shootCooldown = config.getInt("turret-cooldown")
            )
        }
    }
}