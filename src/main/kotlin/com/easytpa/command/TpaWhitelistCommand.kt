package com.easytpa.command

import com.easytpa.db.WhitelistRepository
import com.easytpa.i18n.I18n
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaWhitelistCommand(private val repo: WhitelistRepository, private val i18n: I18n) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            i18n.send(sender, "only-players")
            return true
        }
        if (args.isEmpty()) {
            i18n.send(sender, "usage-whitelist")
            return true
        }
        when (args[0].lowercase()) {
            "add" -> {
                if (args.size < 2) {
                    i18n.send(sender, "usage-whitelist")
                    return true
                }
                val t = sender.server.getPlayer(args[1]) ?: sender.server.getOfflinePlayer(args[1])
                val uuid = t.uniqueId
                val name = t.name ?: args[1]
                val ok = repo.add(sender.uniqueId, uuid, name)
                i18n.send(sender, if (ok) "whitelist-added" else "whitelist-already", "player" to name)
            }
            "remove", "rm", "del" -> {
                if (args.size < 2) {
                    i18n.send(sender, "usage-whitelist")
                    return true
                }
                val t = sender.server.getOfflinePlayer(args[1])
                val ok = repo.remove(sender.uniqueId, t.uniqueId)
                i18n.send(sender, if (ok) "whitelist-removed" else "whitelist-not-found", "player" to args[1])
            }
            "list", "ls" -> {
                val list = repo.list(sender.uniqueId)
                if (list.isEmpty()) {
                    i18n.send(sender, "whitelist-empty")
                } else {
                    i18n.send(sender, "whitelist-list-header", "size" to list.size.toString())
                    list.forEach { i18n.sendRaw(sender, "whitelist-list-entry", "player" to it.second, "uuid" to it.first.toString()) }
                }
            }
            else -> i18n.send(sender, "usage-whitelist")
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
