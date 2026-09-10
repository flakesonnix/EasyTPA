package com.easytpa.command

import com.easytpa.service.TeleportService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TpAcceptCommand(private val service: TeleportService) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players")
            return true
        }
        val res = service.accept(sender, args.firstOrNull())
        if (res != "OK") sender.sendMessage(res)
        return true
    }
}
