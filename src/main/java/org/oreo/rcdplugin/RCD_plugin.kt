package org.oreo.rcdplugin

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.commands.TurretCommands
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.listeners.*
import org.oreo.rcdplugin.turrets.BasicTurret

class RCD_plugin : JavaPlugin() {

    private lateinit var packetDetector: PacketDetector

    override fun onLoad() {

        //PacketEvents require to be loaded up before the plugin being enabled

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .checkForUpdates(true)

        PacketEvents.getAPI().load()
    }

    override fun onEnable() {

        /* We register our packet listeners before calling PacketEventsAPI#init
    because that method might already trigger some events. */
        PacketEvents.getAPI().eventManager.registerListener(
            PacketDetector(this), PacketListenerPriority.NORMAL
        )

        PacketEvents.getAPI().init()

        packetDetector = PacketDetector(this)

        enableListeners()

        ItemManager.init(this)

        getCommand("turret")!!.setExecutor(TurretCommands(this))

        enableTurretUpdateCycle()
    }

    fun enableListeners(){
        server.pluginManager.registerEvents(PlaceTurretListener(this), this)
        server.pluginManager.registerEvents(TurretInterationListener(), this)
        server.pluginManager.registerEvents(BulletHitListener(this), this)
        server.pluginManager.registerEvents(TurretControlListener(this),this)
    }

    override fun onDisable() {
        //Terminate the instance (clean up process)
        PacketEvents.getAPI().terminate()
    }

    private fun enableTurretUpdateCycle(){
        object : BukkitRunnable() {
            override fun run() {
                // Call the update method of MovementHandler every tick
                for (map : MutableMap<Location,String> in controllingTurret.values){
                    val id = map.values.first()
                    val turret = BasicTurret.getTurretFromID(id)
                    if (turret != null) {
                        turret.rotateTurret()
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L) // 0L delay, 1L period means it runs every tick
    }

    //TODO get people out of spectator when they log off/server shuts down

    companion object {
        //Stores turret objects
        val activeTurrets: MutableMap<String,BasicTurret> = mutableMapOf()

        //Stores all players that are controlling the turret along with their location before entering "control mode"
        // and the turrets ID
        val controllingTurret: MutableMap<Player,MutableMap<Location,String>> = mutableMapOf()

        //Keeps track all players that are in remote cooldown
        val inCooldown: MutableList<Player> = mutableListOf()

        //Stores all the bullets currently in the world
        val currentBullets: MutableList<Snowball> = mutableListOf()
    }
}
