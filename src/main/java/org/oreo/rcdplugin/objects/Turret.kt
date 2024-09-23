package org.oreo.rcdplugin.objects

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.bone.ModelBone
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.activeDevices
import org.oreo.rcdplugin.data.TurretConfigs
import org.oreo.rcdplugin.items.ItemManager
import org.oreo.rcdplugin.utils.Utils
import java.util.*

/**
 * The main turret object that handles all internal logic
 * The first two parameters are required whereas the other two are optional and default to null
 * @param spawnHealth Parameter for when a turret is initialised by the server after a shutdown/restart
 * @param spawnPlayer Is used when a player spawns a turret to know who to give a controller to
 * @param turretItem Is used when a player spawns a turret it is used to check if the turret item it was spawned with
 has health inscribed in it
 */
class Turret(location: Location, plugin: RCD_plugin, spawnHealth : Double? = null, spawnID : String? = null,
             spawnPlayer:Player? = null, turretItem : ItemStack? = null) : DeviceBase(location = location, plugin = plugin,
                 deviceType = DeviceEnum.TURRET) {


    /**
     * The "hitbox" armorstand is used to detect the player Right-clicking in spectator mode
     * The client doesn't send the right click-packet unless its on an entity that's why this is needed
     */
    private val hitboxLocation = spawnLocation.clone().add(0.0, 0.4 , 0.0) //0.4

    val hitbox : ArmorStand = world.spawn(hitboxLocation, ArmorStand::class.java)

    private val configs : TurretConfigs = TurretConfigs.fromConfig(plugin)

    //This detects if the turret can shoot or not
    var isInshootCooldown = false

    private val turretEnum = DeviceEnum.TURRET

    /**
     * This variable is used for shooting bullets from the models head
     * Any model works as long as its head isn't close enough to the ground so that the snowballs hit it and break
     */
    private var headBone : ModelBone? = null

    init {

        //Set health that is part of BaseDevice
        health = configs.maxHealth

        //Force load the chunk to avoid model issues
        main.location.chunk.isForceLoaded = true

        if (spawnHealth != null){
            health = spawnHealth
        } else {
            checkDeviceHealthFromController(turretItem)
        }

        //THIS IS THE ONLY INSTANCE WHEN CHANGING THE ID SHOULD BE DONE
        if (spawnID != null){
            id = spawnID
        }


        main.setBasePlate(false)
        main.isVisible = false
        main.customName = "Turret"
        Utils.setMetadata(main, id, turretKey)

        hitbox.isInvulnerable = true
        hitbox.isInvisible = true
        hitbox.isSmall = true
        hitbox.setBasePlate(false)
        Utils.setMetadata(hitbox, id, turretKey)

        givePlayerDeviceControl(spawnPlayer, turretEnum)

        activeDevices[id] = this


        //Initialising the objects models using ModelEngine's API

        val modeLedeMain = ModelEngineAPI.createModeledEntity(main)
        activeModel = ModelEngineAPI.createActiveModel(plugin.config.getString("turret-model-name"))

        modeLedeMain.isBaseEntityVisible = false
        activeModel.setCanHurt(false)

        modeLedeMain.addModel(activeModel,true)

        headBone = activeModel.bones["headbone"]

        main.location.chunk.isForceLoaded = false
    }


    /**
     * This function is called by a synced thread that checks where the player is looking constantly
     */
    fun rotateTurret() {
        val controller = controller ?: return
        val player = controller.player
        val location = main.location
        val hitboxLocation = hitbox.location

        // We pre-calculate the next armor stands locations and then apply them
        location.pitch = when {
            player.location.pitch > configs.maxTurretPitch -> {
                configs.maxTurretPitch.toFloat()
            }
            player.location.pitch < configs.minTurretPitch -> {
                configs.minTurretPitch.toFloat()
            }
            else -> {
                player.location.pitch
            }
        }


        location.yaw = player.location.yaw
        hitboxLocation.yaw = player.location.yaw

        // We use the teleport function to move the armor stands to the pre-calculated locations
        main.teleport(location)
        hitbox.teleport(hitboxLocation)
    }

    /**
     * Gets the armorstands position and shoots a snowball from it
     * This projectile is offset and the added to a list that is tracked via the BulletHitListener
     */
    fun shoot(){

        if (isInshootCooldown){
            return
        }

        val direction: Vector = main.location.direction.normalize()

        if (headBone?.location == null){
            plugin.logger.info("§c ERROR Turret head bone not found")
            return
        }
        val projectileLocation: Location = headBone?.location!!.add(direction.multiply(1))

        //The entity added has to be synced with the main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val snowball = main.world.spawnEntity(projectileLocation, EntityType.SNOWBALL) as Snowball
            world.playSound(snowball.location,Sound.ENTITY_FIREWORK_ROCKET_BLAST,0.1f,0.4f)

            snowball.velocity = direction.multiply(3.7) // Adjust the speed as needed

            //Add it to the list to be picked up by a listener
            RCD_plugin.currentBullets.add(snowball)

            //Spawn a particle that looks like muzzle smoke
            world.spawnParticle(Particle.SPIT, snowball.location, 1, 0.0, 0.0, 0.0,0.0)
        })

        //Handles the shooting cooldown for the turret

        isInshootCooldown = true

        object : BukkitRunnable() {
            override fun run() {
               isInshootCooldown = false
            }
        }.runTaskLater(plugin, configs.shootCooldown.toLong()) //Half a second

    }

    /**
     * Kills the entities that are parts of the turret and removes the turret object
     * @param deleteRemote We give the option to delete the remote too in case someone doesn't want to
     */
    fun deleteTurret(deleteRemote: Boolean = true) {
        if (deleteRemote) {
            deleteRemote(deviceType = deviceType)
        }

        if (controller != null){
            removeController()
        }

        activeModel.isRemoved = true
        main.remove()
        hitbox.remove()
        activeDevices.remove(id)
    }

    /**
     * Drops the turret as an item
     * If the turret has been damaged it adds a new line defining the new health
     * Whenever a new turret is placed this is checked
     */
    private fun dropTurret(){
        val turretItem = ItemManager.turret?.clone()

        if (health != configs.maxHealth){
            val meta = turretItem?.itemMeta

            val lore = if (meta!!.hasLore()) meta.lore else ArrayList()

            if (lore != null) {
                lore[2] = ("Health : $health")
            }

            meta.lore = lore

            turretItem.setItemMeta(meta)
        }

        turretItem.let {
            if (it != null) {
                world.dropItem(main.location, it)
            }
        }

        deleteTurret()
    }

    /**
     * Handles everything to do with entering "control mode"
     */
    fun addController(player:Player){

        val teleportLocation = main.location.clone().add(0.0,configs.controllerHeightOffset,0.0)

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controller = Controller(player = player , location = teleportLocation ,deviceId = id, deviceType = turretEnum)


        // the hitboxes location is offset from the player, so I have to manually make up for it here
        // NOTE : I am unsure weather this offset will work for any model height
        hitbox.teleport(main.location.clone().add(0.0, configs.controllerHeightOffset + 0.9 , 0.0))

        //Adds a cooldown so that players cant spam enter and leave the turret
        RCD_plugin.inCooldown.add(player)
        object : BukkitRunnable() {
            override fun run() {
                RCD_plugin.inCooldown.remove(player)
            }
        }.runTaskLater(plugin, 20 * 3) // 60 ticks = 3 seconds
    }


    /**
     * Handles any melee hit by a player , this is for dropping and damaging the turret so far
     */
    fun handleMeleeHit(player : Player){

        if (!ItemManager.isCustomItem(player.inventory.itemInMainHand, ItemManager.turretControl)){
            damageTurret(10.0)
            return
        }

        val turretID = player.inventory.itemInMainHand.itemMeta.lore?.get(1)

        if (id != turretID){ //Compare the ID inscribed in the item with the objects
            player.sendMessage("§cWrong controller")
            return
        }

        //delete the remote and drop the turret
        player.inventory.itemInMainHand.amount -= 1
        dropTurret()
        player.sendMessage("Turret dropped successfully")
    }


    /**
     * Damages the turret and destroys it if the health goes to zero or bellow
     */
    fun damageTurret(damage : Double){

        health -= damage
        if (health <= 0){

            if (configs.turretSelfDestructEnabled){
                world.createExplosion(main,configs.selfDestructPower)
            }

            world.playSound(main.location,Sound.BLOCK_SMITHING_TABLE_USE,0.5f,0.7f)
            world.playSound(main.location,Sound.ENTITY_GENERIC_EXPLODE,1f,0.7f)

            deleteTurret()
            return
        }

        //This section plays a sound whose pitch depends on the objects health
        val healthRatio = health / configs.maxHealth

        val pitch : Double = (0.5f + (0.5f * healthRatio)).coerceIn(0.4, 1.0)

        world.playSound(main.location,Sound.ENTITY_ITEM_BREAK,0.7f,pitch.toFloat())
    }

    companion object{

        val turretKey : String = "turret"

        //the objects id key that is used for most functions here
        private val turretIDKey = NamespacedKey("rcd", turretKey)

        /**
         * The first two functions bellow are used to create turret objects
         * This is because there is a bunch of optional parameters in order for the player and the server to be able to
         spawn a turret. To avoid getting confused with all the optional parameters the turret creation has been abstracted
         into two different functions .
         */

        /**
         * Spawns a turret by a player
         */
        fun playerSpawnTurret(plugin: RCD_plugin , player: Player,placeLocation : Location){
            Turret(placeLocation.add(0.0, -2.0, 0.0), plugin = plugin, spawnPlayer = player
                , turretItem =  player.inventory.itemInMainHand)
        }

        /**
         * Spawns a turret by the server
         */
        fun serverSpawnTurret(spawnLocation: Location,plugin: RCD_plugin, spawnHealth: Double, id: String ){
            Turret(spawnLocation.add(0.0, 0.0, 0.0), plugin = plugin, spawnHealth = spawnHealth, spawnID = id)
        }

        /**
         * Check if the armorstand has turret metadata
         */
        fun hasTurretMetadata(armorStand: ArmorStand): Boolean {
            val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
            return dataContainer.has(turretIDKey, PersistentDataType.STRING)
        }

        /**
         * Gets the turret object from its ID
         */
        fun getTurretFromID(id:String): Turret? {
            if (!activeDevices.containsKey(id) || activeDevices[id] !is Turret){
                return null
            }
            return activeDevices[id] as Turret
        }

        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getTurretFromArmorStand(stand: ArmorStand) : Turret?{

            val turrets = ArrayList<Turret>()

            for (turret in activeDevices.values){
                if (turret is Turret) {
                    turrets.add(turret)
                }
            }

            for (turret in turrets) {
                if (turret.main == stand || turret.hitbox == stand) {
                    return turret
                }
            }

            return null
        }
    }

}