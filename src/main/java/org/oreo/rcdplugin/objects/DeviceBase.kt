package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.model.ActiveModel
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    var controller: Controller? = null

    var health: Double = 0.0

    //The main armorstand is the core of all devices
    val main: ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    lateinit var activeModel : ActiveModel


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
    fun deleteRemote(deviceType: DeviceEnum){

        for (player in Bukkit.getOnlinePlayers()) {

            val inventory = player.inventory

            val controllerItem = getDeviceControllerItem(deviceType)

            for (item in inventory) {
                if (item != null && ItemManager.isCustomItem(item, controllerItem)) {

                    val turretID = item.itemMeta.lore?.get(1).toString()

                    if (id == turretID){ //if the objects ID matches the items inscribed turret ID
                        item.amount -= 1
                        player.sendMessage("§cYour turret has been destroyed")
                        return
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Retrieves the device corresponding to the provided ID from the active devices.
         *
         * @param id The unique identifier of the device to retrieve.
         * @return The device associated with the identifier, or null if no such device exists.
         */
        fun getDeviceFromId(id : String) : DeviceBase?{
            return RCD_plugin.activeDevices[id]
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
    }
}

/**
 * All possible devices should be here so that they can be stored in the Controller object and anything else
 */
enum class DeviceEnum {
    TURRET,
    DRONE
}