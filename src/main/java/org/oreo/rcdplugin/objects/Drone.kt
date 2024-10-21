package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activeDevices
import org.oreo.rcdplugin.data.DroneConfigs
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.utils.Utils

/**
 * The main turret object that handles all internal logic
 * The first two parameters are required whereas the other two are optional and default to null
 * @param spawnHealth Parameter for when a turret is initialised by the server after a shutdown/restart
 * @param spawnPlayer Is used when a player spawns a turret to know who to give a controller to
 * @param droneItem Is used when a player spawns a turret it is used to check if the turret item it was spawned with
 has health inscribed in it
 */
class Drone(location: Location, plugin: RCD_plugin, spawnHealth : Double? = null, spawnID : String? = null,
            spawnPlayer:Player? = null, droneItem : ItemStack? = null) : PermanentDeviceBase(location = location, plugin = plugin,
                 deviceType = DeviceEnum.DRONE) {

    private val config = DroneConfigs.fromConfig(plugin)

    private val droneEnum = DeviceEnum.DRONE

    //weather the drone can shoot
    private var inCooldown = false

    override val maxHealth: Double = config.maxHealth


    init {
        main.location.chunk.isForceLoaded = true

        if (spawnHealth != null){
            health = spawnHealth
        } else {
            checkDeviceHealthFromController(droneItem)
        }

        //THIS IS THE ONLY INSTANCE WHEN CHANGING THE ID SHOULD BE DONE
        if (spawnID != null){
            id = spawnID
        }

        setUpMain()

        //Initialising the objects models using ModelEngine's API
        setUpModel()


        givePlayerDeviceControl(spawnPlayer, droneEnum)

        activeDevices[id] = this

        main.location.chunk.isForceLoaded = false
    }

    /**
     * sets up the main armorstands settings
     */
    private fun setUpMain(){
        main.setBasePlate(false)
        main.isVisible = false
        main.customName = "Drone"
        Utils.setMetadata(main, id, DRONE_KEY)
    }

    /**
     * Handles model creation using Model Engines API
     */
    private fun setUpModel(){
        val modeLedeMain = ModelEngineAPI.createModeledEntity(main)
        activeModel = ModelEngineAPI.createActiveModel(plugin.config.getString("drone-model-name"))

        modeLedeMain.isBaseEntityVisible = false
        activeModel.setCanHurt(false)
        modeLedeMain.addModel(activeModel,true)
    }

    /**
     * Add any drone specific deletion operations here
     */
    override fun deleteChildDevice(){
        updateTask?.cancel()
    }

    /**
     * Moves the drone to a specified location.
     *
     * @param location The target location to which the drone should be moved.
     */
    private fun moveDrone(location: Location) {
        main.teleport(location)
    }


    /**
     * Handles everything to do with entering "control mode"
     */
    override fun addController(player:Player){

        val teleportLocation = main.location.clone().add(0.0, -1.0 * config.teleportOffset, 0.0)

        startUpdateTask()

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controller = Controller(player = player , location = teleportLocation ,deviceId = id, deviceType = droneEnum, plugin = plugin,)

        // Move the drone up a bit to avoid getting stuck in the ground
        moveDrone(main.location.clone().add(0.0, 1.1, 0.0))

        addShootCooldown()
    }

    /**
     * Calls the bomb function
     */
    override fun handleRightClick() {
        //Nothing....
    }

    /**
     * Checks if the drone is in cooldown
     if not it drops a snowball plays a sound and adds the snowball to the "bomb" list to be detected by a Listener
     */
    fun bomb(){

        if (inCooldown) return

        //The entity added has to be synced with the main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val snowball = main.world.spawnEntity(main.location.clone().add(0.0,-0.2,0.0), EntityType.SNOWBALL) as Snowball
            world.playSound(snowball.location,Sound.ENTITY_FIREWORK_ROCKET_BLAST,0.1f,0.4f)

            //Add it to the list to be picked up by a listener
            RCD_plugin.currentBombs.add(snowball)
        })

        addShootCooldown()
    }

    /**
     * Adds shooting cooldown to prevent the player from spamming bombs
     * This is configurable in the config.yml
     */
    private fun addShootCooldown(){
        inCooldown = true
        controller!!.player.inventory.setItem(2, reloadingBombItem)
        object : BukkitRunnable() { //Handles the shooting delay
            override fun run() {

                inCooldown = false

                if (controller == null) return
                Utils.sendActionBarMessage(player = controller!!.player , message = "Drone Reloaded", color = ChatColor.GREEN)
                controller!!.player.inventory.setItem(2, bombItem)

            }
        }.runTaskLater(plugin, config.bombCooldown.toLong())
    }

    /**
     * Makes the drone explode and destroys it
     */
    fun selfDestruct(){

        world.createExplosion(main,5f)
        deleteDevice()
    }

    override fun removeChildController() {}
    override fun destroyChildDevice() {}

    /**
     * Not much right now just plays a sound
     */
    override fun damageChild(damage: Double) {
        world.playSound(main.location,Sound.BLOCK_SMITHING_TABLE_USE,0.5f,0.7f)
    }

    /**
     * Starts the update cycle and adds it to its designated variable
     for easy access and cancellation in the future
     */
    override fun startUpdateTask(){

        updateTask =

            object : BukkitRunnable() {

                override fun run() {

                    if (controller == null) cancel()

                    moveDrone(controller?.player?.location?.clone()?.add(0.0, config.teleportOffset, 0.0)!!)

                }
            }.runTaskTimer(plugin, 0L,1L)

    }

    override fun isHoldingController(player: Player): Boolean {
        return ItemManager.isCustomItem(player.inventory.itemInMainHand, ItemManager.droneControl)
    }


    companion object {

        const val DRONE_KEY: String = "drone"

        //the objects id key that is used for most functions here
        private val droneIdKey = NamespacedKey("rcd", DRONE_KEY)

        /**
         * Check if the armorstand has turret metadata
         */
        fun hasDroneMetadata(armorStand: ArmorStand): Boolean {
            val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
            return dataContainer.has(droneIdKey, PersistentDataType.STRING)
        }


        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getDroneFromArmorstand(stand: ArmorStand) : Drone?{

            for (drone in activeDevices.values){
                if (drone !is Drone) {
                    continue
                }

                if (drone.main == stand){
                    return drone
                }
            }

            return null
        }

        val selfDestructItem = Utils.createCustomItem(Material.RED_CONCRETE, "Self Destruct",
            "Destroy your drone in an epic explosion")

        val exitItem = Utils.createCustomItem(Material.BLUE_CONCRETE, "Exit drone",
            "Snap back to reality")

        val bombItem = Utils.createCustomItem(Material.GREEN_CONCRETE, "Bomb",
            "Drop a bomb from your drone")

        val reloadingBombItem = Utils.createCustomItem(Material.GRAY_CONCRETE, "Reloading",)

        /**
         * Gives the player the "buttons" to use on the drone
         */
        fun giveControlItems(player: Player){

            player.inventory.setItem(2, bombItem)

            player.inventory.setItem(4,exitItem)

            player.inventory.setItem(6, selfDestructItem)
        }
    }

}