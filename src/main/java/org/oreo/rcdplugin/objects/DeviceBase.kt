package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.model.ActiveModel
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activeDevices
import org.oreo.rcdplugin.items.ItemManager
import java.util.*

abstract class DeviceBase(location: Location , val plugin: RCD_plugin , val deviceType: DeviceEnum) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat which I make use of
     */
    var id: String = UUID.randomUUID().toString();

    val world: World = location.world

    //We spawn the stand one block above so that it isn't in the block
    val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    //The controller that is stored when the player is controlling the device
    var controller: Controller? = null

    var health: Double = 0.0

    //The main armorstand is the core of all devices
    val main: ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    lateinit var activeModel : ActiveModel

    //The devices update task is here
    var updateTask : BukkitTask? = null

    /**
     * Removes a controller from the turret
     */
    fun removeController() {

        if (controller == null) {
            return
        }

        controller!!.removeFromDevice()

        controller = null

        updateTask?.cancel()
    }

    /**
     * Checks the item that the device was placed with
     * if it has a health value inscribed it sets the objects health to it
     * the objects health is set to the max health before this even runs so any returns
     * result in the turret having max health
     */
    fun checkDeviceHealthFromController(item: ItemStack?){

        if (item == null){
            return
        }

        val meta = item.itemMeta

        val healthLore : String

        try {
            healthLore = meta.lore?.get(2) ?: return
        } catch (error: IndexOutOfBoundsException){
            return
        }

        val regex = """Health\s*:\s*(\d+(\.\d+)?)""".toRegex()
        val matchResult = regex.find(healthLore)

        val itemHealth: Double? = matchResult?.groupValues?.get(1)?.toDoubleOrNull()

        if (itemHealth == null){
            plugin.logger.info("§cHealth for device not found")
            return
        }

        health = itemHealth
    }

    /**
     * Runs type specific deletion for any device case .
     * After type-specific deletion, the method removes the device from the main structure and the active devices list
     along with its model .
     */
    fun deleteDevice(remoteDelete: Boolean = true){

        deleteChildDevice()

        removeController()

        if (!activeModel.isRemoved) {
            activeModel.isRemoved = true
        }

        main.remove()

        if (activeDevices.containsKey(id)){
            activeDevices.remove(id)
        }

        if (remoteDelete){
            deleteRemote(deviceType)
        }

        updateTask?.cancel()
    }

    /**
     * Drops the turret as an item
     * If the turret has been damaged it adds a new line defining the new health
     * Whenever a new turret is placed this is checked
     */
    fun dropDevice(){
        val deviceItem = when(deviceType){
            DeviceEnum.TURRET ->{ItemManager.turret?.clone()}
            DeviceEnum.DRONE ->{ItemManager.drone?.clone()}
        }

        val meta = deviceItem?.itemMeta

        val lore = if (meta!!.hasLore()) meta.lore else ArrayList()

        if (lore != null) {
            lore[2] = ("Health : $health")
        }

        meta.lore = lore

        deviceItem.setItemMeta(meta)


        deviceItem.let {
            world.dropItem(main.location, it)
        }

        main.remove()
        activeModel.isRemoved = true

        deleteChildDevice()
    }

    /**
     * This method creates the device control item and sets its lore as the objects unique UUID
     * this way it will be very easy to get the turret it's connected to
     */
    fun givePlayerDeviceControl(spawnPlayer: Player? , deviceType: DeviceEnum){

        if (spawnPlayer == null){
            return
        }

        val deviceControl = getDeviceControllerItem(deviceType)


        if (deviceControl == null) {// Just in case
            spawnPlayer.sendMessage("§cSomething went wrong, cannot give you the device control item")
            return
        }

        // Get the current items metadata
        val meta = deviceControl.itemMeta

        if (meta != null) { // Add the metadata just in case if there isn't any

            val lore = meta.lore ?: mutableListOf()

            if (lore.size > 1) {
                // Override the second lore line
                lore[1] = id
            } else {
                // If there's less than two lines, add a new line
                lore.add(id)
            }

            // Set the updated lore back to the meta
            meta.lore = lore

            // Apply the updated meta to the item
            deviceControl.itemMeta = meta

            spawnPlayer.inventory.addItem(deviceControl)
        }
    }

    /**
     * Loops through all the players inventories to find the remote of the turret
     * if its found it deletes it and informs the player the turret has been destroyed
     */
    private fun deleteRemote(deviceType: DeviceEnum){

        for (player in Bukkit.getOnlinePlayers()) {

            val inventory = player.inventory

            val controllerItem = getDeviceControllerItem(deviceType)

            for (item in inventory) {
                if (item != null && ItemManager.isCustomItem(item, controllerItem)) {

                    val turretID = item.itemMeta.lore?.get(1).toString()

                    if (id == turretID){ //if the objects ID matches the items inscribed turret ID
                        item.amount -= 1

                        val deviceName : String = when(deviceType){
                            DeviceEnum.TURRET -> "turret"
                            DeviceEnum.DRONE -> "drone"
                        }

                        player.sendMessage("§cYour $deviceName has been destroyed")
                        return
                    }
                }
            }
        }
    }


    fun startUpdateTask(){

        when(deviceType){
            DeviceEnum.TURRET ->{ //TODO FIX THIS IALSLIDHKASUHDDKHAGWDJAHGSDIQUKGWDIUASGHD
                val turret = this as Turret
                //TODO add this too
            }

            DeviceEnum.DRONE ->{
                val drone = this as Drone
                drone.droneUpdateCycle()
            }
        }

    }

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
     * Every device handles adding a controller in a different way but they all have to
     */
    abstract fun addController(player:Player)

    companion object {

        /**
         * Retrieves the device corresponding to the provided ID from the active devices.
         *
         * @param id The unique identifier of the device to retrieve.
         * @return The device associated with the identifier, or null if no such device exists.
         */
        fun getDeviceFromId(id : String) : DeviceBase?{
            return activeDevices[id]
        }

        /**
         * Retrieves the item associated with a specific device type.
         */
        fun getDeviceItem(deviceType: DeviceEnum) : ItemStack?{
            return when (deviceType) {
                DeviceEnum.TURRET -> ItemManager.turret
                DeviceEnum.DRONE -> ItemManager.drone
            }
        }

        /**
         * Retrieves the specific control item associated with the given device type.
         */
        fun getDeviceControllerItem(deviceType: DeviceEnum) : ItemStack?{

            return when (deviceType){
                DeviceEnum.TURRET -> ItemManager.turretControl
                DeviceEnum.DRONE -> ItemManager.droneControl
            }
        }

        /**
         * Finds what device a player is in and removes him from it
         * This is to avoid writing logic to find the turret instance within the listeners
         */
        fun removePlayerFromControlling(player: Player){

            for (turret in activeDevices.values){
                if (turret.controller?.player  == player){
                    turret.removeController()
                }
            }
        }


        /**
         * The first two functions bellow are used to create device objects
         * This is because there is a bunch of optional parameters in order for the player and the server to be able to
        spawn a device. To avoid getting confused with all the optional parameters the device creation has been abstracted
        into two different functions .
         */

        /**
         * Spawns a turret by a player
         */
        fun playerSpawnDevice(plugin: RCD_plugin , player: Player,placeLocation : Location , deviceType: DeviceEnum){

            when (deviceType){
                DeviceEnum.TURRET ->{
                    Turret(placeLocation.add(0.0, -2.0, 0.0), plugin = plugin, spawnPlayer = player
                        , turretItem =  player.inventory.itemInMainHand)
                }
                DeviceEnum.DRONE ->{
                    Drone(placeLocation.add(0.0, -2.0, 0.0), plugin = plugin, spawnPlayer = player
                        , droneItem =  player.inventory.itemInMainHand)
                }
            }
        }

        /**
         * Spawns a turret by the server
         */
        fun serverSpawnDevice(spawnLocation: Location,plugin: RCD_plugin, spawnHealth: Double, id: String , deviceType: DeviceEnum ){

            when (deviceType){
                DeviceEnum.TURRET ->{
                    Turret(spawnLocation, plugin = plugin, spawnHealth = spawnHealth, spawnID = id)
                }
                DeviceEnum.DRONE ->{
                    Drone(spawnLocation, plugin = plugin, spawnHealth = spawnHealth, spawnID = id)
                }
            }
        }

        /**
         * Gets the device object from its ID
         */
        fun getDeviceFromID(id:String): DeviceBase? {
            if (!activeDevices.containsKey(id)){
                return null
            }
            return activeDevices[id]
        }
    }
}

/**
 * All possible devices should be here so that they can be stored in the Controller object and anything else
 */
enum class DeviceEnum {
    TURRET,
    DRONE
}