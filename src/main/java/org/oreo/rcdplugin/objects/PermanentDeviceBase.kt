package org.oreo.rcdplugin.objects

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activePermanentDevices
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.utils.Utils
import java.util.*

/**
 * Only devices that permanently exist within the world inherit from this ie : Drones , Turrets etc.
 * These devices are also saved and reloaded on server restart whereas temporary devices are destroyed
 * Things like Rockets that are meant to be controlled only once DO NOT inherit from this
 */
abstract class PermanentDeviceBase(location: Location, val plugin: RCD_plugin, val deviceType: DeviceEnum)
    : RemoteControlledDevice(location = location){

    /**
     * Java has a built-in library to give things random UUID's that don't repeat which I make use of
     */
    var id: String = UUID.randomUUID().toString();

    final override val spawnLocation: Location = location.clone().add(0.0, 1.0, 0.0)

    override val main: ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)


    init {
        activePermanentDevices[id] = this
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

        //Extract the devices health from the Lore using regex patterns
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
     * After type-specific deletion, the method removes the device from the main structure and the active devices list.
     */
    override fun deleteAbstractDevice(remoteDelete: Boolean){


        if (!activeModel.isRemoved) {
            activeModel.isRemoved = true
        }

        main.remove()

        if (activePermanentDevices.containsKey(id)){
            activePermanentDevices.remove(id)
        }

        updateTask?.cancel()

        if (remoteDelete){
            deleteRemote(deviceType)
        }
    }

    /**
     * Drops the device as an item
     * If the turret has been damaged it adds a new line defining the new health
     * Whenever a new turret is placed this is checked
     */
    private fun dropDevice(){
        val deviceItem = getDeviceItem(deviceType)

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

            if (Utils.isInventoryFull(spawnPlayer)){

                world.dropItem(spawnPlayer.location, deviceControl)
                return
            }

            spawnPlayer.inventory.addItem(deviceControl)
        }
    }

    /**
     * Loops through all the players inventories to find the remote of the turret
     if its found it deletes it and informs the player the turret has been destroyed
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

    /**
     * Special logic for when the device is deleted
     */
    override fun destroyDevice(){

        destroyChildDevice()
        deleteDevice()
    }

    /**
     * Special listener for melee hits
     * Handles dropping the device or dealing melee damage
     */
    fun handleMeleeHit(player: Player , damage: Double){

        //Melee hits do a flat fourth of the damage
        if (!isHoldingController(player)){
            if (damage > maxHealth/4){
                damageDevice(damage)
            } else {
                damageDevice(maxHealth/4)
            }

            return
        }

        val deviceID = player.inventory.itemInMainHand.itemMeta.lore?.get(1)

        if (id != deviceID){ //Compare the ID inscribed in the item with the objects
            player.sendMessage("§cWrong controller")
            return
        }

        //delete the remote and drop the turret
        player.inventory.itemInMainHand.amount -= 1
        dropDevice()
        player.sendMessage("Device dropped successfully")
    }

    /**
     * Checks if the player is holding the devices controller type
     */
    abstract fun isHoldingController(player: Player): Boolean


    companion object {

        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getPermanentDeviceFromEntityOrNull(stand: ArmorStand) : PermanentDeviceBase?{

            for (device in activePermanentDevices.values){

                if (device.main == stand){
                    return device
                }
            }

            return null
        }

        /**
         * Retrieves the device corresponding to the provided ID from the active devices.
         *
         * @param id The unique identifier of the device to retrieve.
         * @return The device associated with the identifier, or null if no such device exists.
         */
        fun getDeviceFromId(id : String) : PermanentDeviceBase?{
            return activePermanentDevices[id]
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

            for (device in activePermanentDevices.values){
                if (device.controller?.player  == player){
                    device.removeController()
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
         * This is used when the server re initialises devices from the save file ONLY TO BE USED FOR THAT
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
         * Checks if the entity has device metadata
         * Specifically if the namespaceKey is "rcd"
         * @param entity the entity to check
         */
        fun getDeviceFromEntityOrNull(entity: Entity) : PermanentDeviceBase?{
            for (device in activePermanentDevices.values){

                if (device.main == entity){
                    return device
                }

            }

            return null
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