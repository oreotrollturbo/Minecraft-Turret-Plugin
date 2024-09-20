package org.oreo.rcdplugin.objects

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.oreo.rcdplugin.RCD_plugin


/**
 * Reusable "controller" object
 * This is to be used for all vehicles instead of adding all relevant data manually
 * @param player The player instance is the core of this object and the most important , the object draws
 other information from it on initialisation like previous location, previous game mode etc.
 */
class Controller(val player: Player , val id: String) {

    private var prevGameMode : GameMode = player.gameMode
    private val prevLocation : Location = player.location



    // Property access syntax for player location
    val location: Location
        get() = player.location


    fun removeFromDevice(){
        player.gameMode = prevGameMode
        player.teleport(prevLocation)
        RCD_plugin.controllingTurret.remove(this)
    }

    fun addToDevice(location: Location){
        player.gameMode = GameMode.SPECTATOR

        player.teleport(location)
    }

}