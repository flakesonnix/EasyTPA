package com.easytpa.command

import com.easytpa.db.WhitelistRepository
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaWhitelistCommand(private val repo: WhitelistRepository) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /tpawhitelist <add|remove|list> [player]")
            return true
        }
        when (args[0].lowercase()) {
            "add" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /tpawhitelist add <player>")
                    return true
                }
                val t = sender.server.getPlayer(args[1]) ?: sender.server.getOfflinePlayer(args[1])
                val uuid = t.uniqueId
                val name = t.name ?: args[1]
                val ok = repo.add(sender.uniqueId, uuid, name)
                sender.sendMessage(if (ok) "§aAdded $name" else "§cAlready whitelisted")
            }
            "remove", "rm", "del" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /tpawhitelist remove <player>")
                    return true
                }
                val t = sender.server.getOfflinePlayer(args[1])
                val ok = repo.remove(sender.uniqueId, t.uniqueId)
                sender.sendMessage(if (ok) "§aRemoved ${args[1]}" else "§cNot in whitelist")
            }
            "list", "ls" -> {
                val list = repo.list(sender.uniqueId)
                if (list.isEmpty()) {
                    sender.sendMessage("§7Whitelist empty — everyone can send (unless mode=BLOCK)")
                } else {
                    sender.sendMessage("§bWhitelist (${list.size}):")
                    list.forEach { sender.sendMessage("§7- ${it.second} (${it.first})") }
                }
            }
            else -> sender.sendMessage("§cUnknown: add|remove|list")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> = when (args.size) {
        1 -> listOf("add", "remove", "list").filter { it.startsWith(args[0], true) }
        2 -> if (args[0].lowercase() in setOf("add", "remove")) {
            val p = args[1].lowercase()
            sender.server.onlinePlayers.map { it.name }.filter { it.lowercase().startsWith(p) }
        } else {
            emptyList()
        }
        else -> emptyList()
    }
}
