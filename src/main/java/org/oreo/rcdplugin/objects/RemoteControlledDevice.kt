package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.model.ActiveModel
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.scheduler.BukkitTask

/**
 * The base for all Remotely Controlled Devices
 */
abstract class RemoteControlledDevice(location: Location) {

    val world: World = location.world

    //We spawn the stand one block above so that it isn't in the block
    abstract val spawnLocation : Location

    //The controller that is stored when the player is controlling the device
    var controller: Controller? = null

    var health: Double = 0.0

    //The main armorstand is the core of all devices
    abstract val main: ArmorStand

    //The active ModelEngine model that we can use for a lot of things
    lateinit var activeModel : ActiveModel

    //The devices update task is here
    var updateTask : BukkitTask? = null

    /**
     * We need the devices max health for global logic
     */
    abstract val maxHealth: Double


    init {
        setDeviceMetadata()
        startUpdateTask()
    }


    /**
     * Removes a controller from the device
     */
    fun removeController() { //TODO move the remotedelete bs down to PermanentDeviceBase

        if (controller == null) {
            return
        }

        controller!!.removeFromDevice()

        controller = null

        removeChildController()

        updateTask?.cancel()
    }



    /**
     * Runs type specific deletion for any device case .
     * After type-specific deletion, the method removes the device from the main structure and the active devices list.
     */
    fun deleteDevice(remoteDelete: Boolean = false){ //TODO move the remotedelete bs down to PermanentDeviceBase
        removeController()

        deleteAbstractDevice(remoteDelete)
    }

    /**
     * Damaged the device and delete if it reaches 0 health
     */
    fun damageDevice(damage : Double){

        health -= damage

        if (health <= 0){

            world.playSound(main.location, Sound.BLOCK_SMITHING_TABLE_USE,0.5f,0.7f)
            world.playSound(main.location, Sound.ENTITY_GENERIC_EXPLODE,1f,0.7f)

            destroyDevice()
            return
        }

        damageChild(damage)
    }


    /**
     * Any special logic that a child device might need
     */
    abstract fun damageChild(damage: Double)

    /**
     * When a device is forcefully destroyed
     */
    abstract fun destroyDevice()

    /**
     * Every class that directly inherits from this class needs to have it implemented
     * This is for regular deletion
     */
    protected abstract fun deleteAbstractDevice(remoteDelete: Boolean = false) //TODO move the remotedelete bs down to PermanentDeviceBase

    /**
     * Any special logic for a specific device
     */
    abstract fun removeChildController() //TODO move the remotedelete bs down to PermanentDeviceBase

    /**
     * Logic for when the child device is destroyed
     */
    abstract fun destroyChildDevice()

    /**
     * Starts the internal update task that manages movement/rotation
     */
    abstract fun startUpdateTask()

    /**
     * Deletes the actual device like armorstands, the object pointers etc.
     * All devices must have this function for obvious reasons .
     */
    abstract fun deleteChildDevice()

    /**
     * Handles right-clicking for all devices
     */
    abstract fun handleRightClick()

    /**
     * Every device handles adding a controller in a different way, but they all have to
     */
    abstract fun addController(player: Player)

    /**
     * Set the devices metadata to "rcd"
     */
    abstract fun setDeviceMetadata()


    companion object {

        /**
         * Checks if the entity has device metadata
         * Specifically if the namespaceKey is "rcd"
         * @param entity the entity to check
         */
        fun hasDeviceMetadata(entity: Entity) : Boolean{
            val dataContainer: PersistentDataContainer = entity.persistentDataContainer

            for (nameSpaceKey in dataContainer.keys){
                if (nameSpaceKey.namespace == "rcd") return true
            }
            return false
        }

    }

}