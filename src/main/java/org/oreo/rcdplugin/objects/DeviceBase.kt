package org.oreo.rcdplugin.objects

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.oreo.rcdplugin.RCD_plugin
import java.util.*

abstract class DeviceBase(location: Location , val plugin: RCD_plugin) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat which I make use of
     */
    var id: String = UUID.randomUUID().toString();

    val world: World = location.world

    //We spawn the stand one block above so that it isn't in the block
    val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    var controller: Controller? = null

    var health: Double = 0.0;

    //The main armorstand is the core of all devices
    val main: ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)


    /**
     * Removes a controller from the turret
     */
    fun removeController() {

        if (controller == null) {
            plugin.logger.info("ERROR Controller not found to remove !!")
            return
        }

        controller!!.removeFromDevice()

        controller = null
    }

    companion object {

        fun getDeviceFromId(id : String) : DeviceBase?{
            return RCD_plugin.activeDevices[id]
        }
    }
}

/**
 * All possible devices should be here so that they can be stored in the Controller object and anything else
 */
enum class DeviceEnum {
    TURRET,
    DRONE //This one is a placeholder there is no drone yet
}