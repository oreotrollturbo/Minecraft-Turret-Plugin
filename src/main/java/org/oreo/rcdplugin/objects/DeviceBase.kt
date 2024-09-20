package org.oreo.rcdplugin.objects

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.oreo.rcdplugin.RCD_plugin

abstract class DeviceBase(location: Location , val plugin: RCD_plugin) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat
     * for now I haven't seen any duplicate ID's, but it might happen after the server restarts ?
     * if it does , I will have to migrate to locally storing UUIDs in a JSON file and then check when creating a UUID
     */
    lateinit var id: String;

    val world : World = location.world

    //We spawn the stand one block above so that it isn't in the block
    val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    var controller : Controller? = null

    var health : Double = 0.0;

    //The main armorstand is the core of all devices
    val main : ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)


    /**
     * Removes a controller from the turret
     */
    fun removeController(){

        if (controller == null){
            plugin.logger.info("ERROR Controller not found to remove !!")
            return
        }

        controller!!.removeFromDevice()

        controller = null
    }


    companion object{



    }

}