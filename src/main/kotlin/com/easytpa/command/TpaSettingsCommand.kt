package com.easytpa.command

import com.easytpa.db.TpaSettingsRepository
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaSettingsCommand(private val repo: TpaSettingsRepository) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players")
            return true
        }
        if (args.isEmpty()) {
            val s = repo.getOrDefault(sender.uniqueId)
            sender.sendMessage("§b[EasyTPA] §7autoAccept=${s.autoAccept} mode=${s.requestMode} whitelist=${s.whitelistEnabled}")
            sender.sendMessage("§7/tpasettings autoAccept <on|off> | mode <ASK|BLOCK|AUTO|WHITELIST>")
            return true
        }
        val s = repo.getOrDefault(sender.uniqueId)
        when (args[0].lowercase()) {
            "autoaccept", "auto_accept" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /tpasettings autoAccept <on|off>")
                    return true
                }
                val on = args[1].lowercase() in setOf("on", "true", "yes", "1")
                repo.upsert(s.copy(autoAccept = on))
                sender.sendMessage("§aautoAccept → $on")
            }
            "mode" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /tpasettings mode <ASK|BLOCK|AUTO|WHITELIST>")
                    return true
                }
                val mode = args[1].uppercase()
                if (mode !in setOf("ASK", "BLOCK", "AUTO", "WHITELIST")) {
                    sender.sendMessage("§cInvalid mode")
                    return true
                }
                repo.upsert(s.copy(requestMode = mode))
                sender.sendMessage("§amode → $mode")
            }
            "whitelist" -> {
                val on = if (args.size >= 2) args[1].lowercase() in setOf("on", "true", "1") else !s.whitelistEnabled
                repo.upsert(s.copy(whitelistEnabled = on))
                sender.sendMessage("§awhitelist → $on")
            }
            "status" -> {
                sender.sendMessage("§b$ s")
            }
            else -> sender.sendMessage("§cUnknown subcommand")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> = when (args.size) {
        1 -> listOf("autoAccept", "mode", "whitelist", "status").filter { it.startsWith(args[0], true) }
        2 -> when (args[0].lowercase()) {
            "autoaccept" -> listOf("on", "off")
            "mode" -> listOf("ASK", "BLOCK", "AUTO", "WHITELIST")
            "whitelist" -> listOf("on", "off")
            else -> emptyList()
        }
        else -> emptyList()
    }
}
