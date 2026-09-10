package com.easytpa.command

import com.easytpa.i18n.I18n
import com.easytpa.service.TeleportService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaCommand(private val service: TeleportService, private val i18n: I18n) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            i18n.send(sender, "only-players")
            return true
        }
        if (args.isEmpty()) {
            i18n.send(sender, "usage-tpa")
            return true
        }
        val target = sender.server.getPlayer(args[0]) ?: run {
            i18n.send(sender, "player-not-found", "player" to args[0])
            return true
        }
        if (target == sender) {
            i18n.send(sender, "self-request")
            return true
        }
        val res = service.send(sender, target)
        // service already returns i18n message (with prefix for some) — send as is
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
