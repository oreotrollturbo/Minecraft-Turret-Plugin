package org.oreo.rcdplugin

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.commands.TurretCommands
import org.oreo.rcdplugin.data.TurretSaveData
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.listeners.*
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.Turret
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


class RCD_plugin : JavaPlugin() {

    private lateinit var packetDetector: PacketDetector

    private var saveFile: File? = null
    private val gson = Gson()

    var turretsToLoad = ArrayList<TurretSaveData>()

    private val turretLoadDelay = config.getInt("turret-load-delay")

    /**
     * PacketEvents API requires to be loaded up before the plugin being enabled
     */
    override fun onLoad() {

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .checkForUpdates(true)

        PacketEvents.getAPI().load()

        this.saveFile = File(dataFolder, "objects.json")
    }

    override fun onEnable() {

        /* We register our packet listeners before calling PacketEventsAPI#init
    because that method might already trigger some events. */
        PacketEvents.getAPI().eventManager.registerListener(
            PacketDetector(this), PacketListenerPriority.NORMAL
        )
        PacketEvents.getAPI().init()
        packetDetector = PacketDetector(this)

        loadAndCrateTurrets()

        enableListeners()

        ItemManager.init(this)

        getCommand("turret")!!.setExecutor(TurretCommands(this))

        enableTurretUpdateCycle()

        saveDefaultConfig()
    }

    /**
     * A function to handle listeners separately
     */
    private fun enableListeners(){
        server.pluginManager.registerEvents(PlaceTurretListener(this), this)
        server.pluginManager.registerEvents(TurretInterationListener(), this)
        server.pluginManager.registerEvents(BulletHitListener(this), this)
        server.pluginManager.registerEvents(TurretControlListener(this),this)
        server.pluginManager.registerEvents(ModelEntityDeathListener(this),this)
    }

    override fun onDisable() {
        //Terminate the instance (clean up process)
        PacketEvents.getAPI().terminate()

        handleTurretDisabling()

    }

    /**
     * Serialises the basic components of a turret : health , location(x,y,z) and ID
     * Then saves them in a JSON file to be loaded back when the server comes on
     */
    private fun handleTurretDisabling(){
        saveTurretList()
        removeTurretControllers()
    }

    /**
     * This runnable runs every tick updating every turret's rotation
     * This is the smoothest way I have found to do this
     */
    private fun enableTurretUpdateCycle(){
        object : BukkitRunnable() { //TODO consider instead of using a global update cycle to run a "private" one when a player is in a turret
            override fun run() {
                // Call the update method of MovementHandler every tick
                for (map : MutableMap<Location,String> in controllingTurret.values){
                    val id = map.values.first()
                    val turret = Turret.getTurretFromID(id)
                    turret?.rotateTurret()
                }
            }
        }.runTaskTimer(this, 0L, 1L)
    }

    private fun loadAndCrateTurrets(){

        loadSavedData()

        val turretAmount = turretsToLoad.size

        logger.info("$turretAmount objects loaded")
        if (turretAmount == 0){
            logger.info("Is this right ?")
        }


        object : BukkitRunnable() { //TODO consider instead of using a global update cycle to run a "private" one when a player is in a turret
            override fun run() {
                for (turretData in turretsToLoad){
                    val world = getOverWorld()
                    val turretLocation = Location(world,turretData.x,turretData.y,turretData.z)

                    Turret.serverSpawnTurret(
                        id = turretData.id, plugin = this@RCD_plugin, spawnLocation = turretLocation, spawnHealth = turretData.health
                    )
                }
            }
        }.runTaskLater(this, turretLoadDelay.toLong())
    }

    /**
     * The only function Gson-related that is called on server startup
     * Everything else should be called by it if needed
     */
    private fun loadSavedData() {
        if (!saveFile?.exists()!!) {
            initializeSaveFile() // Create the file with default content if it does not exist
        }

        try {
            saveFile?.let {
                FileReader(it).use { reader ->
                    val listType = object : TypeToken<List<TurretSaveData>>() {}.type

                    turretsToLoad = gson.fromJson(reader, listType)
                }
            }
        } catch (e: IOException) {
            logger.info("File not found, creating save file.")
            initializeSaveFile()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
    }


    /**
     * Saves all active objects to the file and then deletes all the in game objects
     * It first wipes the active turret list in case any residuals are present
     */
    fun saveTurretList() {

        turretsToLoad.clear()

        for (turret in activeTurrets.values){
            val turretData = TurretSaveData(
                id = turret.id,
                health = turret.health,
                x = turret.main.location.x,
                y = turret.main.location.y,
                z = turret.main.location.z
            )

            turretsToLoad.add(turretData)
        }

        //Cant be in the loop above due to the scary ConcurrentModificationException
        for (turret in activeTurrets.values.toTypedArray().copyOf()){
            turret.deleteTurret(false)
        }

        try {
            saveFile?.let {
                FileWriter(it).use { writer ->
                    gson.toJson(turretsToLoad, writer)
                    logger.info("Turrets list saved successfully.")
                }
            }
        } catch (e: IOException) {
            logger.info("Error saving objects list.")
            e.printStackTrace()
        }
    }

    /**
     * Removes all the players controlling from "control mode"
     * This is mainly used on server shutdown so people don't stay in spectator
     */
    private fun removeTurretControllers(){
        for (turret in activeTurrets.values){

            if (turret.controller == null){
                return
            }

            turret.removeController()
        }
    }


    /**
     * Sets up the turret save file using Gson if it doesn't exist
     */
    private fun initializeSaveFile() {
        if (!saveFile?.exists()!!) {
            try {
                if (saveFile?.createNewFile()!!) {
                    logger.info("Created new file at: " + saveFile!!.absolutePath)
                    saveFile?.let {
                        FileWriter(it).use { writer ->
                            writer.write("[]") // Write an empty JSON array to the file
                            loadSavedData()
                        }
                    }
                }
            } catch (e: IOException) {
                logger.info("Unable to create save file.")
                e.printStackTrace()
            }
        } else {
            logger.info("Save file found.")
        }
    }

    companion object {
        //Stores turret objects
        var activeTurrets: MutableMap<String,Turret> = mutableMapOf()

        //Stores all players that are controlling the turret along with their location before entering "control mode"
        // and the objects ID
        val controllingTurret: ArrayList<Controller> = arrayListOf()

        //Keeps track all players that are in remote cooldown
        val inCooldown: MutableList<Player> = mutableListOf()

        //Stores all the bullets currently in the world
        val currentBullets: MutableList<Snowball> = mutableListOf()

        fun getOverWorld() : World? {
            for (world in Bukkit.getWorlds()) {
                if (world.environment == Environment.NORMAL) {
                    return world
                }
            }

            return null
        }
    }
}
