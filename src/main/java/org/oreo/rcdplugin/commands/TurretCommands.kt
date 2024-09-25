package org.oreo.rcdplugin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.oreo.rcdplugin.RCD_plugin
import org.oreo.rcdplugin.items.ItemManager


class TurretCommands(private val plugin: RCD_plugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command")
            return true
        }


        if (!sender.isOp) {
            sender.sendMessage("§c Only server operators can use this command")
            return true
        }

        val player: Player = sender

        if (args.isEmpty()) {
            player.sendMessage("§c Please specify a subcommand: turret , drone or delete")
            return true
        }

        when (args[0].lowercase()) {

            "turret" -> {
                ItemManager.turret?.let { player.inventory.addItem(it) }
                player.sendMessage("Gave you a turret successfully")
            }
            "drone" -> {
                ItemManager.drone?.let { player.inventory.addItem(it) }
                player.sendMessage("Gave you a drone :)")
            }
            "delete" -> {
                val devicesToDelete = ArrayList(RCD_plugin.activeDevices.values)

                for (device in devicesToDelete) {
                    device.deleteChildDevice()
                }
            }
            else -> {
                player.sendMessage("§c Unknown subcommand. Use 'turret' , 'drone' or 'delete'.")
            }
        }

        return true
    }


    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (!sender.isOp){
            return emptyList()
        }
        if (args.size == 1) {
            val subCommands = listOf("turret","drone" , "delete")
            return subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return null
    }
}

