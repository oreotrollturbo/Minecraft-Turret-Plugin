package org.oreo.rcdplugin.objects

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.oreo.rcdplugin.RCD_plugin

abstract class TemporaryDeviceBase (location: Location, val plugin: RCD_plugin, val deviceType: DeviceEnum)
    : RemoteControlledDevice(location = location){

    override val spawnLocation: Location = location

    override val main: ArmorStand = world.spawn(spawnLocation, ArmorStand::class.java)



}