package org.oreo.rcdplugin

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.oreo.rcdplugin.commands.TurretCommands
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.listeners.PlaceTurretListener
import org.oreo.rcdplugin.listeners.TurretControlListener
import org.oreo.rcdplugin.listeners.TurretInterationListener
import org.oreo.rcdplugin.turrets.BasicTurret

class RCD_plugin : JavaPlugin() {

    override fun onEnable() {
        server.pluginManager.registerEvents(PlaceTurretListener(), this)
        server.pluginManager.registerEvents(TurretInterationListener(), this)
        server.pluginManager.registerEvents(TurretControlListener(this),this)

        ItemManager.init(this)

        getCommand("turret")!!.setExecutor(TurretCommands(this))
    }

    companion object {
        /**
         * The simplest and fastest way to track al turrets
         */
        val activeTurrets: MutableMap<String,BasicTurret> = mutableMapOf()
        val controllingTurret: MutableMap<Player,MutableMap<Location,String>> = mutableMapOf()
        val inCooldown: MutableList<Player> = mutableListOf()
    }
}
