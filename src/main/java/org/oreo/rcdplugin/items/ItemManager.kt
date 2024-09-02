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
            meta.setDisplayName("§eBasic turret")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"idfk\"") //The funni
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

    fun createTurretControl(): ItemStack {
        val item = ItemStack(Material.PHANTOM_MEMBRANE, 1)
        val meta = item.itemMeta

        if (meta != null) {
            meta.setDisplayName("§eTurret control")

            val lore: MutableList<String> = ArrayList()
            lore.add("§7experimetntal")
            lore.add("§5\"No turret paired\"") //The funni
            meta.lore = lore

            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS) //to add the enchant glint but not have it be visible

            // Add a unique identifier to make the item non-stackable
            val data = meta.persistentDataContainer
            val key = NamespacedKey(plugin!!, "turret_control")
            data.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())

            item.setItemMeta(meta)
        }
        return item
    }

    /**
     * These methods are in the ItemManager because it simply makes more sense to me
     */
    fun isHoldingBasicTurret(player: Player): Boolean {
        val itemInHand: ItemStack = player.inventory.itemInMainHand

        return itemInHand.itemMeta == basicTurret?.itemMeta
    }

    fun isHoldingTurretControl(player: Player): Boolean {
        val itemInHand: ItemStack = player.inventory.itemInMainHand

        return itemInHand.itemMeta == turretControl?.itemMeta && player.gameMode != GameMode.SPECTATOR
    }
}
