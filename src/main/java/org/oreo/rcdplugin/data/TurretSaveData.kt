package org.oreo.rcdplugin.data

/**
 * This data class holds all the turret data that need to be stored on server shutdown
 for re-initialization when the server starts
 */
data class TurretSaveData(val id: String,
                          val health: Double,
                          val x: Double,
                          val y: Double,
                          val z: Double )
