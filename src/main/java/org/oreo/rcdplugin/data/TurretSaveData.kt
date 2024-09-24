package org.oreo.rcdplugin.data

import org.oreo.rcdplugin.objects.DeviceEnum

/**
 * This data class holds all the turret data that need to be stored on server shutdown
 for re-initialization when the server starts
 */
data class TurretSaveData(val deviceType : DeviceEnum,
                          val id: String,
                          val health: Double,
                          val x: Double,
                          val y: Double,
                          val z: Double )
