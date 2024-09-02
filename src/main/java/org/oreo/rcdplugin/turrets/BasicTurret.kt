package org.oreo.rcdplugin.turrets

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager
import java.util.*

class BasicTurret(location: Location, val owner: Player) {

    /**
     * Java has a built-in library to give things random UUID's that don't repeat
     * for now I haven't seen any duplicate ID's, but it might have issues after every server restart ?
     * if it does , I will have to migrate to locally storing UUIDs in a JSON file
     */
    val id: String = UUID.randomUUID().toString()

    private val world : World = location.world

    private val spawnLocation = location.clone().add(0.0, 1.0, 0.0)
    //We need the armorstand of course
    val main : ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)

    init {
        main.isInvulnerable = true
        main.setBasePlate(false)
        setMetadata(main, id)

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
            owner.sendMessage("§cSomething went wrong, cannot give you the turret control item")
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

            owner.inventory.addItem(turretControl)
        }
    }

    fun rotateTurret(pitch:Float , yaw:Float){
        val location = main.location

        location.pitch = pitch
        location.yaw = yaw

        main.teleport(location)
    }

    fun shoot(){ //TODO make cannon go boom boom
        val direction: Vector = main.location.direction.normalize()

        // Adjust the spawn location of the projectile
        val projectileLocation: Location = main.location.add(direction.multiply(1))

        val arrow = main.world.spawnEntity(projectileLocation, EntityType.SNOWBALL)

        // Set the velocity of the projectile
        arrow.velocity = direction.multiply(30) // Adjust the speed as needed
    }

    private fun setMetadata(armorStand: ArmorStand, turretID: String) {
        val dataContainer: PersistentDataContainer = armorStand.persistentDataContainer
        val key = NamespacedKey("rcd", "basic_turret")
        dataContainer.set(key, PersistentDataType.STRING, turretID)
    }

    fun deleteTurret() {
        main.remove()
        RCD_plugin.activeTurrets.remove(id)
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
    }

}