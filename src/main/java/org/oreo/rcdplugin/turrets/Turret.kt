package org.oreo.rcdplugin.turrets

import com.ticxo.modelengine.api.ModelEngineAPI
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
import org.oreo.rcdplugin.RCD_plugin.Companion.controllingTurret
import org.oreo.rcdplugin.items.ItemManager
import java.util.*


class Turret(location: Location, private var controler:Player?, turretItem : ItemStack, private val plugin: RCD_plugin) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat
     * for now I haven't seen any duplicate ID's, but it might happen after the server restarts ?
     * if it does , I will have to migrate to locally storing UUIDs in a JSON file and then check when creating a UUID
     */
    val id: String = UUID.randomUUID().toString()

    private val world : World = location.world

    //We spawn the stand one block above so that it isn't in the block
    private val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    //The main armorstand is the core of turret
    val main : ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    /**
     * The "hitbox" armorstand is used to detect the player Right-clicking in spectator mode
     * The client doesn't send the right click-packet unless its on an entity that's why this is needed
     */
    private val hitboxLocation = spawnLocation.clone().add(0.0, 0.4 , 0.0) //0.4

    val hitbhox : ArmorStand = world.spawn(hitboxLocation, ArmorStand::class.java)

    //Configs
    private val maxHealth : Double= plugin.config.getDouble("turret-health")
    private val turretSelfDestructEnabled : Boolean = plugin.config.getBoolean("turret-explode")
    private val selfDestructPower : Float = plugin.config.getInt("turret-explode-strength").toFloat()
    private val controllerHeightOffset : Double = plugin.config.getDouble("controller-height-offset")

    //The turrets health is defined by the max health which is configurable
    private var health : Double = maxHealth

    //The gamemode of the player controlling before he enters is stored to take him back to it when he exits
    private var controllerGameMode : GameMode? = null

    /**
     * This variable is used for shooting bullets from the models head
     * Any model works as long as its head isn't close enough to the ground so that the snowballs hit it and break
     */
    private var headBone : ModelBone? = null

    init {

        checkTurretHealth(turretItem)

        main.setBasePlate(false)
        main.isVisible = false
        main.customName = "Turret"
        setMetadata(main, id)

        hitbhox.isInvulnerable = true
        hitbhox.isInvisible = true
        hitbhox.isSmall = true
        hitbhox.setBasePlate(false)
        setMetadata(hitbhox, id)


        givePlayerTurretControl()


        RCD_plugin.activeTurrets.put(id, this)


        //Initialising the turrets models using ModelEngine's API

        val modeLedeMain = ModelEngineAPI.createModeledEntity(main)
        val activeModel = ModelEngineAPI.createActiveModel(plugin.config.getString("turret-model-name"))

        modeLedeMain.isBaseEntityVisible = false

        modeLedeMain.addModel(activeModel,true)

        headBone = activeModel.bones.get("headbone")
    }

    private fun checkTurretHealth(item:ItemStack){

        val meta = item.itemMeta

        val healthLore : String

        try {
            healthLore = meta.lore?.get(2) ?: return
        } catch (error: IndexOutOfBoundsException){
            return
        }


        val regex = """Health\s*:\s*(\d+(\.\d+)?)""".toRegex()
        val matchResult = regex.find(healthLore)

        val itemHealth: Double? = matchResult?.groupValues?.get(1)?.toDoubleOrNull()

        if (itemHealth == null){
            plugin.logger.info("§cHealth for turret not found")
            return
        }


        health = itemHealth
    }



    /**
     * This method creates the turret control item and sets its lore as the turrets unique UUID
     * this way it will be very easy to get the turret it's connected to
     */
    private fun givePlayerTurretControl(){

        if (controler == null){
            return
        }

        val turretControl = ItemManager.turretControl

        if (turretControl == null) {// Just in case
            controler!!.sendMessage("§cSomething went wrong, cannot give you the turret control item")
            return
        }

        // Get the current items metadata
        val meta = turretControl.itemMeta

        if (meta != null) { // Add the metadata just in case if there isn't any

            val lore = meta.lore ?: mutableListOf()

            if (lore.size > 1) {
                // Override the second lore line
                lore[1] = id
            } else {
                // If there's less than two lines, add a new line
                lore.add(id)
            }

            // Set the updated lore back to the meta
            meta.lore = lore

            // Apply the updated meta to the item
            turretControl.itemMeta = meta

            controler!!.inventory.addItem(turretControl)
        }

        controler = null
    }

    /**
     * This function is called by a synced thread that checks where the player is looking constantly
     */
    fun rotateTurret(){

        if (controler == null){
            return
        }

        val location = main.location
        val hitboxLocation = hitbhox.location

        //We pre-calculate the next armorstands locations and then apply them

        if  (controler!!.location.pitch > 10){
            location.pitch = 10f
        }else{
            location.pitch = controler!!.location.pitch
        }

        location.yaw = controler!!.location.yaw
        hitboxLocation.yaw = controler!!.location.yaw


        //We use the teleport function to move the armorstands to the pre-calculated locations
        main.teleport(location)
        hitbhox.teleport(hitboxLocation)
    }

    /**
     * Gets the armorstands position and shoots a snowball from it
     * This projectile is offset and the added to a list that is tracked via the BulletHitListener
     */
    fun shoot(){

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
    }

    /**
     * This function sets the metadata for the armorstand with its unique ID and other identifiers
     */
    private fun setMetadata(armorStand: ArmorStand, turretID: String) {
        val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
        val key = NamespacedKey("rcd", "basic_turret")
        dataContainer.set(key, PersistentDataType.STRING, turretID)
    }

    /**
     * Kills the entities that are parts of the turret and removes the turret object
     */
    fun deleteTurret() {
        deleteRemote()
        main.remove()
        hitbhox.remove()
        RCD_plugin.activeTurrets.remove(id)
    }

    /**
     * Loops through all the players inventories to find the remote of the turret
     * if its found it deletes it and informs the player the turret has been destroyed
     */
    fun deleteRemote(){

        for (player in Bukkit.getOnlinePlayers()) {

            val inventory = player.inventory

            for (item in inventory) {
                if (item != null && ItemManager.isTurretControl(item)) {

                    val turretID = item.itemMeta.lore?.get(1).toString()

                    if (id == turretID){ //if the turrets ID matches the items inscribed turret ID
                        item.amount -= 1
                        player.sendMessage("§cYour turret has been destroyed")
                        return
                    }
                }
            }
        }

    }

    /**
     * Drops the turret as an item
     * If the turret has been damaged it adds a new line defining the new health
     * Whenever a new turret is placed this is checked
     */
    fun dropTurret(){ //TODO finish this
        val turretItem = ItemManager.basicTurret?.clone()

        if (health != maxHealth){
            val meta = turretItem?.itemMeta

            val lore = if (meta!!.hasLore()) meta.lore else ArrayList()

            lore?.add("Health : $health")

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

        controler = player

        val map : MutableMap<Location,String> = mutableMapOf()

        map[player.location] = id

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controllerGameMode = player.gameMode
        controllingTurret.put(player,map)
        player.gameMode = GameMode.SPECTATOR
        player.teleport(main.location.clone().add(0.0,controllerHeightOffset,0.0))

        // the hitboxes location is offset from the player, so I have to manually make up for it here
        // NOTE : I am unsure weather this offset will work for any model height
        hitbhox.teleport(main.location.clone().add(0.0, controllerHeightOffset + 0.9 , 0.0))

        //Adds a cooldown so that players cant spam enter and leave the turret
        RCD_plugin.inCooldown.add(player)
        object : BukkitRunnable() {
            override fun run() {
                RCD_plugin.inCooldown.remove(player)
            }
        }.runTaskLater(plugin, 20 * 3) // 60 ticks = 3 seconds
    }

    /**
     * Removes the player from controlling the turret
     */
    fun removeController(player: Player){
        controllingTurret[player]?.keys?.let { player.teleport(it.first()) }

        controllingTurret.remove(player)
        if (controllerGameMode != null){
            player.gameMode = controllerGameMode as GameMode
        } else {
            player.gameMode = GameMode.SURVIVAL //Default to survival if anything goes wrong
        }

        controler = null
    }


    /**
     * Damages the turret and destroys it if the health goes to zero or bellow
     */
    fun damageTurret(damage : Double){

        health -= damage
        if (health <= 0){

            println(turretSelfDestructEnabled)
            println(selfDestructPower)

            if (turretSelfDestructEnabled){
                world.createExplosion(main,selfDestructPower)
            }

            world.playSound(main.location,Sound.BLOCK_SMITHING_TABLE_USE,0.5f,0.7f)
            world.playSound(main.location,Sound.ENTITY_GENERIC_EXPLODE,1f,0.7f)

            deleteTurret()
            return
        }

        //This section plays a sound whose pitch depends on the turrets health

        val healthRatio = health / maxHealth

        val pitch : Double = (0.5f + (0.5f * healthRatio)).coerceIn(0.4, 1.0)

        world.playSound(main.location,Sound.ENTITY_ITEM_BREAK,0.7f,pitch.toFloat())
    }

    companion object{
        //the turrets id key that is used for most functions here
        private val turretIDKey = NamespacedKey("rcd", "basic_turret")

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
            return RCD_plugin.activeTurrets[id]
        }

        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getTurretFromArmorStand(stand: ArmorStand) : Turret?{

            val turretsToDelete = ArrayList(RCD_plugin.activeTurrets.values)

            for (turret in turretsToDelete) {
                if (turret != null) {
                    if (turret.main == stand || turret.hitbhox == stand){
                        return turret
                    }
                }
            }

            return null
        }

        /**
         * This function finds what turret a player is in and removes him from it
         * This is to avoid writing logic to find the turret instance within the listeners
         */
        fun removePlayerFromControlling(player: Player){

            if (controllingTurret.keys.contains(player)){

                val turret = controllingTurret[player]?.values?.toList()?.get(0)
                    ?.let { getTurretFromID(it) }

                if (turret != null) {
                    turret.removeController(player)
                }
            }
        }
    }

}