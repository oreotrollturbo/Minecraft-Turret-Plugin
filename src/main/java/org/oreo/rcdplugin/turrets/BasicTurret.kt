package org.oreo.rcdplugin.turrets

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

class BasicTurret(location: Location, var controler: Player, private val plugin: RCD_plugin) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat
     * for now I haven't seen any duplicate ID's, but it might happen after every server restarts ?
     * if it does , I will have to migrate to locally storing UUIDs in a JSON file and then checking when creating a UUID
     */
    val id: String = UUID.randomUUID().toString()

    private val world : World = location.world

    private val spawnLocation = location.clone().add(0.0, 1.0, 0.0)

    //We need the armorstand of course
    val main : ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    private val hitboxLocation = spawnLocation.clone().add(0.0, 1.4 , 0.0)

    val hitbhox : ArmorStand = world.spawn(hitboxLocation, ArmorStand::class.java)

    init {
        //main.isInvulnerable = true
        main.setBasePlate(false)
        setMetadata(main, id)

        hitbhox.setGravity(false)
        hitbhox.isInvulnerable = true
        hitbhox.isInvisible = true
        hitbhox.isSmall = true
        hitbhox.setBasePlate(false)
        setMetadata(hitbhox, id)


        giveTurretControl()

        RCD_plugin.activeTurrets.put(id, this)
    }

    /**
     * This method creates the turret control item and sets its lore as the turrets unique UUID
     * this way it will be very easy to get the turret it's connected to
     */
    private fun giveTurretControl(){
        val turretControl = ItemManager.turretControl

        if (turretControl == null) {
            controler.sendMessage("§cSomething went wrong, cannot give you the turret control item")
            return
        }

        // Get the current item meta
        val meta = turretControl.itemMeta

        if (meta != null) {

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

            controler.inventory.addItem(turretControl)
        }
    }

    fun rotateTurret(){
        val location = main.location
        val hitboxLocation = hitbhox.location

        if  (controler.location.pitch > -2){
            location.pitch = -2f
        }else{
            location.pitch = controler.location.pitch
        }

        location.yaw = controler.location.yaw
        hitboxLocation.yaw = controler.location.yaw

        main.teleport(location)
    }

    fun shoot(){
        val direction: Vector = main.location.direction.normalize()

        // Adjust the spawn location of the projectile
        val projectileLocation: Location = main.location.add(direction.multiply(1))

        //This makes sure when you add an entity its synced
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val snowball = main.world.spawnEntity(projectileLocation, EntityType.SNOWBALL) as Snowball

            // Set the velocity of the projectile
            snowball.velocity = direction.multiply(3) // Adjust the speed as needed

            RCD_plugin.currentBullets.add(snowball)
        })
    }

    private fun setMetadata(armorStand: ArmorStand, turretID: String) {
        val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
        val key = NamespacedKey("rcd", "basic_turret")
        dataContainer.set(key, PersistentDataType.STRING, turretID)
    }

    fun deleteTurret() {
        main.remove()
        hitbhox.remove()
        RCD_plugin.activeTurrets.remove(id)
    }

    fun dropTurret(){
        ItemManager.basicTurret?.let { world.dropItem(main.location, it) }

        deleteTurret()
    }

    fun addController(player:Player){

        controler = player

        val map : MutableMap<Location,String> = mutableMapOf()

        map[player.location] = id

        controllingTurret.put(player,map)
        player.gameMode = GameMode.SPECTATOR
        player.teleport(main.location.clone().add(0.0,0.55,0.0))

        RCD_plugin.inCooldown.add(player)
        object : BukkitRunnable() {
            override fun run() {
                RCD_plugin.inCooldown.remove(player)
            }
        }.runTaskLater(plugin, 20 * 1) // 200 ticks = 10 seconds
    }

    companion object{
        private val turretIDKey = NamespacedKey("rcd", "basic_turret")

        fun hasTurretMetadata(armorStand: ArmorStand): Boolean {
            val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
            return dataContainer.has(turretIDKey, PersistentDataType.STRING)
        }

        fun getTurretFromID(id:String): BasicTurret? {
            return RCD_plugin.activeTurrets[id]
        }

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
    }

}