package com.easytpa.command

import com.easytpa.service.TeleportService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaCommand(private val service: TeleportService) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /tpa <player>")
            return true
        }
        val target = sender.server.getPlayer(args[0]) ?: run {
            sender.sendMessage("§cPlayer not found: ${args[0]}")
            return true
        }
        if (target == sender) {
            sender.sendMessage("§cCan't tpa to yourself")
            return true
        }
        val res = service.send(sender, target)
        sender.sendMessage(res)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val p = args[0].lowercase()
            return sender.server.onlinePlayers.map { it.name }.filter { it.lowercase().startsWith(p) }.sorted()
        }
        return emptyList()
    }
}
