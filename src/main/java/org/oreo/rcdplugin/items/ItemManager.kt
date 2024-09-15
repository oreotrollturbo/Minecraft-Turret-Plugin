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
import java.util.*

object ItemManager {
    private var plugin: JavaPlugin? = null
    var basicTurret: ItemStack? = null
    var turretControl: ItemStack? = null


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
        basicTurret = createBasicTurretItem()
        turretControl = createTurretControl()
    }

    /**
     * @return the item
     * Makes the basic turret item , gives it the enchantment glow description and lore
     */
    fun createBasicTurretItem(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            val maxHealth : Double? = plugin?.config?.getDouble("turret-health")

            meta.setDisplayName("§eBasic turret")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"idfk\"")
            lore.add("Health : $maxHealth")
            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS) //to add the enchant glint but not have it be visible

            // Add a unique identifier to make the item non-stackable
            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, "basic_turret")
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
     * A few simple checks to see if a player is holding a custom item
     * These methods are in the ItemManager because it simply makes more sense to me
     */

    /**
     * Checks for everything but the lore as that changes with the "Health" lore
     * This function could be improved/simplified, but it's not a priority currently
     */
    fun isHoldingBasicTurret(player: Player): Boolean {
        val itemInHand: ItemStack = player.inventory.itemInMainHand

        if (itemInHand.type != basicTurret?.type) {
            return false
        }

        val itemMeta = itemInHand.itemMeta
        val basicTurretMeta = basicTurret?.itemMeta

        if (itemMeta == null || basicTurretMeta == null) {
            return false
        }

        if (itemMeta.displayName != basicTurretMeta.displayName) {
            return false
        }

        if (itemMeta.enchants != basicTurretMeta.enchants) {
            return false
        }

        return true
    }

    fun isHoldingTurretControl(player: Player): Boolean {
        val itemInHand: ItemStack = player.inventory.itemInMainHand

        return isTurretControl(itemInHand) && player.gameMode != GameMode.SPECTATOR
    }

    fun isTurretControl(item: ItemStack?): Boolean {
        if (item == null || !item.hasItemMeta()) {
            return false
        }

        val meta = item.itemMeta
        val data = meta?.persistentDataContainer
        val key = NamespacedKey(plugin!!, "turret_control")

        return data?.has(key, PersistentDataType.STRING) == true
    }
}
