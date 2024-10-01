package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activeDevices
import org.oreo.rcdplugin.data.DroneConfigs
import org.oreo.rcdplugin.items.ItemManager
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

    private val config = DroneConfigs.fromConfig(plugin)

    private val droneEnum = DeviceEnum.DRONE

    private val teleportOffset = 0.6

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
    override fun deleteChildDevice(){
        // Add stuff if needed
    }

    /**
     * Moves the drone to a specified location.
     *
     * @param location The target location to which the drone should be moved.
     */
    private fun moveDrone(location: Location){
        main.teleport(location)
    }


    /**
     * Handles everything to do with entering "control mode"
     */
    fun addController(player:Player){

        val teleportLocation = main.location.clone().add(0.0, -1 * teleportOffset, 0.0)

        startUpdateTask()

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controller = Controller(player = player , location = teleportLocation ,deviceId = id, deviceType = droneEnum, plugin = plugin)

        // Move the drone up a bit to avoid getting stuck in the ground
        moveDrone(main.location.clone().add(0.0, 0.5, 0.0))
    }

    /**
     * Handles any melee hit by a player , this is for dropping and damaging the drone
     */
    fun handleMeleeHit(player : Player){

        if (!ItemManager.isCustomItem(player.inventory.itemInMainHand, ItemManager.droneControl)){
            damageDrone(10.0)
            return
        }

        val droneID = player.inventory.itemInMainHand.itemMeta.lore?.get(1)

        if (id != droneID){ //Compare the ID inscribed in the item with the objects
            player.sendMessage("§cWrong controller")
            return
        }

        //delete the remote and drop the turret
        player.inventory.itemInMainHand.amount -= 1
        dropDevice()
        player.sendMessage("Drone dropped successfully")
    }

    /**
     * Damages the turret and destroys it if the health goes to zero or bellow
     */
    private fun damageDrone(damage : Double){

        health -= damage
        if (health <= 0){

            world.playSound(main.location,Sound.BLOCK_SMITHING_TABLE_USE,0.5f,0.7f)
            world.playSound(main.location,Sound.ENTITY_GENERIC_EXPLODE,0.6f,1.0f)

            deleteDevice()
            return
        }

        //This section plays a sound whose pitch depends on the objects health
        val healthRatio = health / config.maxHealth

        val pitch : Double = (0.5f + (0.5f * healthRatio)).coerceIn(0.4, 1.0)

        world.playSound(main.location,Sound.ENTITY_ITEM_BREAK,0.7f,pitch.toFloat())
    }

    /**
     * Starts the update cycle and adds it to its designated variable
     for easy access and cancellation in the future
     */
    fun droneUpdateCycle(){

        updateTask =

            object : BukkitRunnable() {

                override fun run() {

                    moveDrone(controller?.player?.location?.clone()?.add(0.0, teleportOffset, 0.0)!!)

                }
            }.runTaskTimer(plugin, 0L,1L)

    }


    companion object {

        const val droneKey: String = "drone"

        //the objects id key that is used for most functions here
        private val droneIdKey = NamespacedKey("rcd", droneKey)

        /**
         * Check if the armorstand has turret metadata
         */
        fun hasDroneMetadata(armorStand: ArmorStand): Boolean {
            val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
            return dataContainer.has(droneIdKey, PersistentDataType.STRING)
        }


        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getDroneFromArmorstand(stand: ArmorStand) : Drone?{

            for (drone in activeDevices.values){
                if (drone !is Drone) {
                    continue
                }

                if (drone.main == stand){
                    return drone
                }
            }

            return null
        }

    }

}