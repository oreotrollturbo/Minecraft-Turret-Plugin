package org.oreo.rcdplugin

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.oreo.rcdplugin.commands.RCDCommands
import org.oreo.rcdplugin.data.DeviceSaveData
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.listeners.controller.ControllerListeners
import org.oreo.rcdplugin.listeners.devices.drone.DroneControlListener
import org.oreo.rcdplugin.listeners.devices.general.*
import org.oreo.rcdplugin.listeners.devices.turret.TurretControlListener
import org.oreo.rcdplugin.listeners.projectiles.ProjectileHitListener
import org.oreo.rcdplugin.objects.Controller
import org.oreo.rcdplugin.objects.DeviceBase
import org.oreo.rcdplugin.objects.DeviceEnum
import org.oreo.rcdplugin.objects.Turret
import org.oreo.rcdplugin.utils.Utils
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

//TODO fix drone damage
//TODO turrets dont rotate

class RCD_plugin : JavaPlugin() {

    private lateinit var packetDetector: PacketDetector

    private var saveFile: File? = null
    private val gson = Gson()

    var devicesToLoad = ArrayList<DeviceSaveData>()

    private val deviceLoadDelay = config.getInt("device-load-delay")


    /**
     * PacketEvents API requires to be loaded up before the plugin being enabled
     */
    override fun onLoad() {

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .checkForUpdates(true)

        PacketEvents.getAPI().load()

        this.saveFile = File(dataFolder, "devices.json")
    }

    override fun onEnable() {

        /* We register our packet listeners before calling PacketEventsAPI#init
    because that method might already trigger some events. */
        PacketEvents.getAPI().eventManager.registerListener(
            PacketDetector(this), PacketListenerPriority.NORMAL
        )
        PacketEvents.getAPI().init()
        packetDetector = PacketDetector(this)

        loadAndCrateDevices()

        enableListeners()

        ItemManager.init(this)

        getCommand("device")!!.setExecutor(RCDCommands(this))

        enableTurretUpdateCycle()

        saveDefaultConfig()

    }

    /**
     * A function to handle listeners separately
     */
    private fun enableListeners(){
        server.pluginManager.registerEvents(PlaceDeviceListener(this), this)
        server.pluginManager.registerEvents(ProjectileHitListener(this), this)
        server.pluginManager.registerEvents(ModelEntityDeathListener(this),this)
        server.pluginManager.registerEvents(PlayerControlDevice(),this)

        server.pluginManager.registerEvents(ControllerListeners(this),this)
        server.pluginManager.registerEvents(DeviceDamageListener(),this)

        server.pluginManager.registerEvents(TurretControlListener(this),this)

        server.pluginManager.registerEvents(DroneControlListener(this),this)
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
        saveDeviceList()
        removeDeviceControllers()
    }

    /**
     * This runnable runs every tick updating every turret's rotation
     * This is the smoothest way I have found to do this
     */
    private fun enableTurretUpdateCycle(){
        object : BukkitRunnable() {
            override fun run() {

                // Call the update method of MovementHandler every tick
                for (controller in controllingDevice){
                    if (controller.deviceType != DeviceEnum.TURRET) {
                        return
                    }

                    val id = controller.deviceId
                    val turret = DeviceBase.getDeviceFromID(id) as Turret?
                    turret?.rotateTurret()
                }
            }
        }.runTaskTimer(this, 0L, 1L)
    }

    /**
     * Loads saved device data and schedules the creation of devices in the game world.
     *
     * This method first loads the saved device data from a file. It then logs the total
     * number of devices loaded. If no devices are loaded, it logs an additional message
     * questioning if this is correct.
     *
     * After loading the data, it schedules a task to run after a delay, which iterates
     * over the loaded device data and spawns each device in the game world at the specified
     * locations with the provided attributes such as ID, health, and device type.
     */
    private fun loadAndCrateDevices(){

        loadSavedData()

        val turretAmount = devicesToLoad.size

        logger.info("$turretAmount objects loaded")
        if (turretAmount == 0){
            logger.info("Is this right ?")
        }


        object : BukkitRunnable() {
            override fun run() {
                for (deviceData in devicesToLoad){
                    val world = Utils.getOverWorld()
                    val deviceLocation = Location(world,deviceData.x,deviceData.y,deviceData.z)

                    val deviceEnum = deviceData.deviceType

                    DeviceBase.serverSpawnDevice(
                        id = deviceData.id, plugin = this@RCD_plugin, spawnLocation = deviceLocation, spawnHealth = deviceData.health,
                        deviceType = deviceEnum
                    )
                }
            }
        }.runTaskLater(this, deviceLoadDelay.toLong())
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
                    val listType = object : TypeToken<List<DeviceSaveData>>() {}.type

                    devicesToLoad = gson.fromJson(reader, listType)
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
    private fun saveDeviceList() {

        devicesToLoad.clear()

        for (device in activeDevices.values){
            val deviceData = DeviceSaveData(
                deviceType = device.deviceType,
                id = device.id,
                health = device.health,
                x = device.main.location.x,
                y = device.main.location.y,
                z = device.main.location.z
            )

            devicesToLoad.add(deviceData)
        }

        //Cant be in the loop above due to the scary ConcurrentModificationException
        //So we copy it
        for (device in activeDevices.values.toTypedArray().copyOf()){

            device.deleteDevice(remoteDelete = false)

        }

        try {
            saveFile?.let {
                FileWriter(it).use { writer ->
                    gson.toJson(devicesToLoad, writer)
                    logger.info("Turrets list saved successfully.")
                }
            }
        } catch (e: IOException) {
            logger.info("Error saving objects list.")
            e.printStackTrace()
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

    /**
     * Removes all the players controlling from "control mode"
     * This is mainly used on server shutdown so people don't stay in spectator
     */
    private fun removeDeviceControllers(){
        for (device in activeDevices.values){

            if (device.controller == null){
                return
            }

            device.removeController()
        }
    }

    companion object {
        //Stores turret objects
        var activeDevices: MutableMap<String,DeviceBase> = mutableMapOf()

        //Stores all players that are controlling the turret along with their location before entering "control mode"
        // and the objects ID
        val controllingDevice: ArrayList<Controller> = arrayListOf()

        //Keeps track all players that are in remote cooldown
        val inCooldown: MutableList<Player> = mutableListOf()

        //Stores all the bullets currently in the world
        val currentBullets: MutableList<Snowball> = mutableListOf()

        //Stores all the bullets currently in the world
        val currentBombs: MutableList<Snowball> = mutableListOf()

        //Makes sure a player doesn't place two devices at once
        val placeCooldown: MutableList<Player> = mutableListOf()
    }
}
