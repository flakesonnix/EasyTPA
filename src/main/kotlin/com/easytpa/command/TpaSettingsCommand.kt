package com.easytpa.command

import com.easytpa.db.TpaSettingsRepository
import com.easytpa.i18n.I18n
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaSettingsCommand(private val repo: TpaSettingsRepository, private val i18n: I18n) :
    CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            i18n.send(sender, "only-players")
            return true
        }
        if (args.isEmpty()) {
            val s = repo.getOrDefault(sender.uniqueId)
            i18n.send(sender, "settings-header", "autoAccept" to s.autoAccept.toString(), "mode" to s.requestMode, "whitelist" to s.whitelistEnabled.toString())
            sender.sendMessage(i18n.t("usage-tpasettings"))
            return true
        }
        // reload lang
        if (args[0].lowercase() == "reload" && sender.hasPermission("easytpa.admin")) {
            i18n.reload()
            sender.sendMessage("§aLanguage reloaded: ${i18n.t("settings-status")}")
            return true
        }
        val s = repo.getOrDefault(sender.uniqueId)
        when (args[0].lowercase()) {
            "autoaccept", "auto_accept" -> {
                if (args.size < 2) {
                    i18n.send(sender, "usage-tpasettings")
                    return true
                }
                val on = args[1].lowercase() in setOf("on", "true", "yes", "1")
                repo.upsert(s.copy(autoAccept = on))
                i18n.send(sender, if (on) "settings-autoAccept-on" else "settings-autoAccept-off")
            }
            "mode" -> {
                if (args.size < 2) {
                    i18n.send(sender, "usage-tpasettings")
                    return true
                }
                val mode = args[1].uppercase()
                if (mode !in setOf("ASK", "BLOCK", "AUTO", "WHITELIST")) {
                    sender.sendMessage("§cInvalid mode")
                    return true
                }
                repo.upsert(s.copy(requestMode = mode))
                i18n.send(sender, "settings-mode-set", "mode" to mode)
            }
            "whitelist" -> {
                val on = if (args.size >= 2) args[1].lowercase() in setOf("on", "true", "1") else !s.whitelistEnabled
                repo.upsert(s.copy(whitelistEnabled = on))
                i18n.send(sender, if (on) "settings-whitelist-on" else "settings-whitelist-off")
            }
            "status" -> {
                i18n.send(sender, "settings-status", "autoAccept" to s.autoAccept.toString(), "mode" to s.requestMode, "whitelist" to s.whitelistEnabled.toString())
            }
            else -> i18n.send(sender, "usage-tpasettings")
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
