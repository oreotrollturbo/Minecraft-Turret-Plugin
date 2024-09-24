package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activeDevices
import org.oreo.rcdplugin.data.DroneConfigs
import org.oreo.rcdplugin.utils.Utils

/**
 * The main turret object that handles all internal logic
 * The first two parameters are required whereas the other two are optional and default to null
 * @param spawnHealth Parameter for when a turret is initialised by the server after a shutdown/restart
 * @param spawnPlayer Is used when a player spawns a turret to know who to give a controller to
 * @param droneItem Is used when a player spawns a turret it is used to check if the turret item it was spawned with
 has health inscribed in it
 */
class Drone(location: Location, plugin: RCD_plugin, spawnHealth : Double? = null, spawnID : String? = null,
            spawnPlayer:Player? = null, droneItem : ItemStack? = null) : DeviceBase(location = location, plugin = plugin,
                 deviceType = DeviceEnum.DRONE) {

    val config = DroneConfigs.fromConfig(plugin)

    private val droneEnum = DeviceEnum.DRONE

    init {
        main.location.chunk.isForceLoaded = true

        if (spawnHealth != null){
            health = spawnHealth
        } else {
            checkDeviceHealthFromController(droneItem)
        }

        //THIS IS THE ONLY INSTANCE WHEN CHANGING THE ID SHOULD BE DONE
        if (spawnID != null){
            id = spawnID
        }

        main.setBasePlate(false)
        main.isVisible = false
        main.customName = "Drone"
        Utils.setMetadata(main, id, droneKey)


        //Initialising the objects models using ModelEngine's API

        val modeLedeMain = ModelEngineAPI.createModeledEntity(main)
        activeModel = ModelEngineAPI.createActiveModel(plugin.config.getString("drone-model-name"))

        modeLedeMain.isBaseEntityVisible = false
        activeModel.setCanHurt(false)
        modeLedeMain.addModel(activeModel,true)


        givePlayerDeviceControl(spawnPlayer, droneEnum)

        activeDevices[id] = this

        main.location.chunk.isForceLoaded = false
    }

    /**
     * Add any drone specific deletion operations here
     */
    fun deleteDrone(){
        //Any drone specific logic will be here
    }


    /**
     * Handles everything to do with entering "control mode"
     */
    fun addController(player:Player){

        val teleportLocation = main.location.clone()

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controller = Controller(player = player , location = teleportLocation ,deviceId = id, deviceType = droneEnum)

        //Adds a cooldown so that players cant spam enter and leave the turret
        RCD_plugin.inCooldown.add(player)
        object : BukkitRunnable() {
            override fun run() {
                RCD_plugin.inCooldown.remove(player)
            }
        }.runTaskLater(plugin, 20 * 3) // 60 ticks = 3 seconds
    }


    companion object {

        val droneKey: String = "drone"

    }

}