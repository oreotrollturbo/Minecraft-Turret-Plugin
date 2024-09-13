package org.oreo.rcdplugin.turrets

import com.ticxo.modelengine.api.ModelEngineAPI
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.RCD_plugin.Companion.controllingTurret
import org.oreo.rcdplugin.items.ItemManager
import java.util.*

class BasicTurret(location: Location, private var controler:Player?, private val plugin: RCD_plugin) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat
     * for now I haven't seen any duplicate ID's, but it might happen after every server restarts ?
     * if it does , I will have to migrate to locally storing UUIDs in a JSON file and then checking when creating a UUID
     */
    val id: String = UUID.randomUUID().toString()

    private val world : World = location.world

    //We spawn the stand one block above so that it isn't in the block
    private val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    //We need the armorstand of course
    val main : ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    //The "hitbox" armorstand is used to detect the player Right-clicking in spectator mode
    // The client doesn't send the right click-packet unless its on an entity that's why this is needed
    private val hitboxLocation = spawnLocation.clone().add(0.0, 0.4 , 0.0) //0.4

    //Spawn the armorstand
    val hitbhox : ArmorStand = world.spawn(hitboxLocation, ArmorStand::class.java)

    private val maxHealth : Double= plugin.config.getDouble("turret-health")

    private val turretSelfDestructEnabled : Boolean = plugin.config.getBoolean("turret-explode")

    private val selfDestructPower : Float = plugin.config.getInt("turret-explode-strength").toFloat()

    //The turrets health is defined by the max health
    private var health : Double = maxHealth

    private var controllerGameMode : GameMode? = null

    init {
        //Basic settings for the armorstand
        main.setBasePlate(false)
        main.isVisible = false
        main.customName = "Turret"
        setMetadata(main, id)

        //Settings for the hitbox that is used for spectator right-clicking
        hitbhox.isInvulnerable = true
        hitbhox.isInvisible = true
        hitbhox.isSmall = true
        hitbhox.setBasePlate(false)
        setMetadata(hitbhox, id)

        //Gives the player the turret control item
        giveTurretControl()

        //Add the turret id of course
        RCD_plugin.activeTurrets.put(id, this)


        //Initialising the turrets models using ModelEngine's API

        val modeLedeMain = ModelEngineAPI.createModeledEntity(main)
        val activeModel = ModelEngineAPI.createActiveModel("turret")

        modeLedeMain.isBaseEntityVisible = false

        modeLedeMain.addModel(activeModel,true)
    }



    /**
     * This method creates the turret control item and sets its lore as the turrets unique UUID
     * this way it will be very easy to get the turret it's connected to
     */
    private fun giveTurretControl(){

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
    fun rotateTurret(){ //TODO change this to be relative to the item model

        if (controler == null){
            return
        }

        val location = main.location
        val hitboxLocation = hitbhox.location

        //We pre-calculate the next armorstands locations and then applies them

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

        // Adjust the spawn location of the projectile
        val projectileLocation: Location = main.location.add(direction.multiply(1)).add(0.0, 0.5, 0.0)

        //This makes sure when you add an entity its synced
        //It is impossible otherwise
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val snowball = main.world.spawnEntity(projectileLocation, EntityType.SNOWBALL) as Snowball
            world.playSound(snowball.location,Sound.ENTITY_FIREWORK_ROCKET_BLAST,0.1f,0.4f)
            //snowball.isVisibleByDefault = false

            // Set the velocity of the projectile
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
     * Kills the entities and removes the object
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
     * Drops the turret as an item and deletes it
     */
    fun dropTurret(){
        ItemManager.basicTurret?.let { world.dropItem(main.location, it) }

        deleteTurret()
    }

    /**
     * Handles everything to do with entering "control mode"
     */
    fun addController(player:Player){

        hitbhox.teleport(main.location.clone().add(0.0, 0.4 , 0.0)) //Exit and re-enter to fix hitbox mismatch
        // mainly for when the turret falls
        controler = player

        val map : MutableMap<Location,String> = mutableMapOf()

        map[player.location] = id

        //Add the player to "control mode" sets the players mode to spectator
        // Then teleports the player to the armorstand
        controllerGameMode = player.gameMode
        controllingTurret.put(player,map)
        player.gameMode = GameMode.SPECTATOR
        player.teleport(main.location.clone().add(0.0,-0.5,0.0)) // 0.55

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
        controllingTurret.get(player)?.keys?.let { player.teleport(it.first()) }

        player.sendMessage("Exited a turret") //This was originally a debug message, but I might keep it
        controllingTurret.remove(player)
        if (controllerGameMode != null){
            player.gameMode = controllerGameMode as GameMode
        } else {
            player.gameMode = GameMode.SURVIVAL //Default to survival if anything goes wrong
        }

        controler = null
    }


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

        val healthRatio = health / maxHealth

        val pitch : Double = (0.5f + (0.5f * healthRatio)).coerceIn(0.4, 1.0)

        world.playSound(main.location,Sound.ENTITY_ITEM_BREAK,0.7f,pitch.toFloat())
    }

    companion object{
        //the turrets id key that is used for all companion object
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
        fun getTurretFromID(id:String): BasicTurret? {
            return RCD_plugin.activeTurrets[id]
        }

        /**
         * Gets a turret object from an armorstand
         * returns null if not found
         */
        fun getTurretFromArmorStand(stand: ArmorStand) : BasicTurret?{

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