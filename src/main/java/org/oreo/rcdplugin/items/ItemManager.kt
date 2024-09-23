package org.oreo.rcdplugin.items

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
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

    /**
     * Item initialisation
     */
    fun init(pluginInstance: JavaPlugin?) {
        plugin = pluginInstance
        createItems()
    }

    /**
     * Creates the item
     */
    private fun createItems() {
        turret = createTurretItem()
        turretControl = createTurretControl()

        drone = createDroneItem()
        droneControl = createDroneControl()
    }

    /**
     * @return the item
     * Makes the basic turret item , gives it the enchantment glow description and lore
     */
    private fun createTurretItem(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            val maxHealth : Double? = plugin?.config?.getDouble("turret-health")

            meta.setDisplayName("§eTurret")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"idfk\"")
            lore.add("Health : $maxHealth")
            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS) //to add the enchant glint but not have it be visible

            // Add a unique identifier to make the item non-stackable
            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, Turret.turretKey)
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item
    }

    private fun createTurretControl(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            meta.setDisplayName("§eTurret control")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"No turret paired\"") //The funni

            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, "turret_control")
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item
    }


    /**
     * @return the item
     * Makes the drone item , gives it the enchantment glow description and lore
     */
    private fun createDroneItem(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            val maxHealth : Double? = plugin?.config?.getDouble("drone-health")

            meta.setDisplayName("§eDrone")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"idfk\"")
            lore.add("Health : $maxHealth")
            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS) //to add the enchant glint but not have it be visible

            // Add a unique identifier to make the item non-stackable
            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, Drone.droneKey)
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item
    }


    private fun createDroneControl(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            meta.setDisplayName("§Drone control")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"No drone paired\"") //The funni

            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, "drone_control")
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
     * Checks for everything but the lore as that changes with the "Health" lore
     * This function could be improved/simplified, but it's not a priority currently
     */

    fun isHoldingCustomItem(player: Player, item: ItemStack ): Boolean {
        val itemInHand: ItemStack = player.inventory.itemInMainHand

        return isCustomItem(item = itemInHand)
    }

    fun isCustomItem(item: ItemStack): Boolean {

        if (item.type != item.type) {
            return false
        }

        val itemMeta = item.itemMeta
        val customItemMeta = item.itemMeta

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
