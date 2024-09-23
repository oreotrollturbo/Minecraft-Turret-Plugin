package org.oreo.rcdplugin.objects

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.oreo.rcdplugin.RCD_plugin.Companion.controllingDevice
import org.oreo.rcdplugin.utils.Utils
import java.util.*


/**
 * Reusable "controller" object
 * This is to be used for all vehicles instead of adding all relevant data manually
 * @param player The player instance is the core of this object and the most important , the object draws
 other information from it on initialisation like previous location, previous game mode etc.
 */
class Controller(val player: Player,location: Location , val deviceId: String, val deviceType : DeviceEnum) {

    private var prevGameMode : GameMode = player.gameMode
    private val prevLocation : Location = player.location

    private var healthOnReturn : Double = player.health
    private val world = player.world

    //This armorstand represents the player , if it gets hit the player gets hit
    private val villager : Villager = world.spawn(prevLocation, Villager::class.java)

    // Unique ID for each Controller instance
    val id: String = UUID.randomUUID().toString()

    /**
     * Sets up the villager configs and then adds the player to its device
     */
    init {
        Utils.setMetadata(entity = villager, id = id , metadataKey =  controllerKey )
        villager.customName = player.name + " (controlling device)"
        villager.isCustomNameVisible = true
        villager.velocity = player.velocity
        villager.fallDistance = player.fallDistance

        addToDevice(location)
    }


    /**
     * Applies damage to the player controlled by this controller.
     * Reduces the player's health by the specified damage amount, ensuring the health does not go below 0.
     * If the player's health reaches 0, this method will invoke `removeFromDevice` to handle the player's removal
     * from the controlling device and reset the player's health appropriately.
     *
     * @param damage The amount of damage to inflict on the player.
     */
    fun damagePlayer(damage : Double){

        healthOnReturn = maxOf(0.0, healthOnReturn - damage)
        world.playSound(player,Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f)

        if (healthOnReturn <= 0){
            killPlayer()
        }
    }

    /**
     * Kicks the player out of control mode and kills them
     */
    fun killPlayer(){
        //Teleport the player instantly to make sure they don't die in the turret
        player.teleport(villager.location)
        DeviceBase.getDeviceFromId(deviceId)?.removeController()
        player.health = 0.0
    }

    /**
     * Removes the player from the controlling device, resetting the player's game mode and location,
     * and removing the controller from the controlling device's list.
     *
     * This method restores the player's previous game mode and location before they were
     * added to the controlling device. It ensures that the player returns to their previous state
     * and removes the current controller instance from the list of devices it was controlling.
     */
    fun removeFromDevice(){
        player.gameMode = prevGameMode
        player.health = healthOnReturn
        player.teleport(villager.location)
        player.fallDistance = villager.fallDistance
        villager.remove()

        controllingDevice.remove(this)
    }

    /**
     * Adds the player to the controlling device, setting the player's game mode to spectator and
     * teleporting them to the specified location.
     *
     * @param location The location to which the player will be teleported.
     */
    private fun addToDevice(location: Location){
        player.gameMode = GameMode.SPECTATOR
        player.teleport(location)
        controllingDevice.add(this)
    }

    companion object{
        //The metadata key used to identify it's a controller armorstand
        val controllerKey = "controller"

        //the objects id key that is used for most functions here
        private val controllerIDKey = NamespacedKey("rcd", controllerKey)

        /**
         * Check if the armorstand has controller metadata
         */
        fun hasControllerMetadata(villager: Villager): Boolean {
            val dataContainer: PersistentDataContainer = villager.persistentDataContainer
            return dataContainer.has(controllerIDKey, PersistentDataType.STRING)
        }

        /**
         * Retrieves the controller associated with a given villager.
         *
         * @param villager The villager for which to retrieve the controller.
         * @return The controller object associated with the villager, or null if no controller metadata exists.
         */
        fun getControllerForVillager(villager: Villager): Controller? {
            if (!hasControllerMetadata(villager)) return null

            val dataContainer: PersistentDataContainer = villager.persistentDataContainer
            val id = dataContainer.get(controllerIDKey, PersistentDataType.STRING)!!
            return getControllerFromId(id)
        }

        /**
         * Gets the controller object from a player if he has one
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
         * Gets the controller object from its ID
         */
        private fun getControllerFromId(id:String): Controller? {

            for (controller in controllingDevice){
                if (controller.id == id){
                    return controller
                }
            }

            return null
        }
    }

}