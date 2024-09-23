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
import org.oreo.rcdplugin.data.TurretConfig
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
class Drone(location: Location, plugin: RCD_plugin, spawnHealth : Double? = null, spawnID : String? = null,
            spawnPlayer:Player? = null, turretItem : ItemStack? = null) : DeviceBase(location = location, plugin = plugin,
                 deviceType = DeviceEnum.TURRET) {


    companion object {

        val droneKey: String = "drone"
    }

}