package org.oreo.rcdplugin.items

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.objects.Drone
import org.oreo.rcdplugin.objects.Turret
import java.util.*

object ItemManager {
    private var plugin: JavaPlugin? = null

    var turret: ItemStack? = null
    var turretControl: ItemStack? = null

    var drone: ItemStack? = null
    var droneControl: ItemStack? = null

    private const val CONTROLLER_IDENTIFIER = "§7RCD controller"

    /**
     * Item initialisation
     */
    fun init(pluginInstance: JavaPlugin?) {
        plugin = pluginInstance
        createItems()
    }

    /**
     * Creates all the custom items
     * Keep in mind all device items have an odd number for custom model data and every controller has an even number
     */
    private fun createItems() {
        turret = createDeviceItem("§eTurret",Turret.turretKey,"turret-health",1)
        turretControl = createController(name = "§aTurret Controller","turret",2)

        drone = createDeviceItem("§eDrone",Drone.DRONE_KEY,"drone-health",3)
        droneControl = createController(name = "§aDrone Controller","drone",4)
    }


    /**
     * Global function to create a device item
     * @param name The name of the item when created
     * @param deviceKey the unique Device key  for its data container
     * @param configHealth The string that links to its maxHealth in the config , every device should have this
     */
    private fun createDeviceItem(name: String , deviceKey : String , configHealth : String , modelData: Int): ItemStack {

        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            val maxHealth : Double? = plugin?.config?.getDouble(configHealth)

            meta.setCustomModelData(modelData)

            meta.setDisplayName(name)

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"idfk\"")
            lore.add("Health : $maxHealth")
            meta.lore = lore

            // Add a unique identifier to make the item non-stackable
            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, deviceKey)
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item

    }


    /**
     * Creates a controller item
     * @param name The controller item name
     * @param deviceName used for the unique namespaceKey and other things
     * @return the finalised item
     */
    private fun createController(name : String , deviceName: String , modelData: Int) : ItemStack{

        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta



        if (meta != null) {
            meta.setDisplayName(name) // Should start with §7

            meta.setCustomModelData(modelData)

            val lore: MutableList<String> = ArrayList()
            lore.add(CONTROLLER_IDENTIFIER)
            lore.add("§5\"No $deviceName paired\"") //The funni

            meta.lore = lore

            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, deviceName + "_control") //deviceName_control
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item

    }



    /**
     * A few simple checks to see if a player is holding a custom item
     * These methods are in the ItemManager because it simply makes more sense to me
     */

    /**
     * Checks if the player is holding a specific custom item.
     *
     * @param player the player whose held item is to be checked
     * @param item the custom item to check against
     * @return true if the player is holding the specified custom item, false otherwise
     */
    fun isHoldingCustomItem(player: Player, item: ItemStack? ): Boolean {

        if (item == null) return false

        val itemInHand: ItemStack = player.inventory.itemInMainHand

        return isCustomItem(item = itemInHand , itemToCheck = item)
    }

    /**
     *
     */
    fun isHoldingDeviceController(player: Player): Boolean {

        if (player.inventory.itemInMainHand.itemMeta == null) return false

        val lore = player.inventory.itemInMainHand.itemMeta.lore ?: return false

        return lore.contains(CONTROLLER_IDENTIFIER)
    }

    /**
     * Checks if a given item is a specific custom item.
     *
     * @param item the item to be checked
     * @param itemToCheck the custom item to check against
     * @return true if the given item matches the custom item, false otherwise
     */
    fun isCustomItem(item: ItemStack , itemToCheck : ItemStack?): Boolean {

        if (itemToCheck == null) {
            return false
        }

        if (item.type != itemToCheck.type) {
            return false
        }

        val itemMeta = item.itemMeta
        val customItemMeta = itemToCheck.itemMeta

        if (itemMeta == null || customItemMeta == null) {
            return false
        }

        if (itemMeta.displayName != customItemMeta.displayName) {
            return false
        }

        if (itemMeta.enchants != customItemMeta.enchants) {
            return false
        }

        return true
    }
}
