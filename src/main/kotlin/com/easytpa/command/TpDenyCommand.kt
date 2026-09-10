package com.easytpa.command

import com.easytpa.service.TeleportService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TpDenyCommand(private val service: TeleportService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players")
            return true
        }
        val res = service.deny(sender, args.firstOrNull())
        if (res != "OK") sender.sendMessage(res) else sender.sendMessage("§cDenied.")
        return true
    }
}
