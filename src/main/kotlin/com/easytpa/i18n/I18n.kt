package com.easytpa.i18n

import java.io.File
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * Lightweight i18n — loads lang/<code>.yml from jar or dataFolder, supports placeholders {key}.
 * Paper & Spigot compatible (Bukkit ChatColor).
 * Usage: i18n.t("request-sent", "target" to player.name)
 */
class I18n(private val plugin: JavaPlugin) {
    private var lang: String = "en"
    private var messages: YamlConfiguration = YamlConfiguration()
    private var fallback: YamlConfiguration = YamlConfiguration()

    fun load() {
        lang = plugin.config.getString("language", "en")!!.lowercase()
        // ensure lang files exist in dataFolder/lang/
        val langDir = File(plugin.dataFolder, "lang")
        langDir.mkdirs()
        // copy default langs from resources if missing
        for (code in listOf("en", "de")) {
            val out = File(langDir, "$code.yml")
            if (!out.exists()) {
                plugin.getResource("lang/$code.yml")?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        // load fallback (en) first
        plugin.getResource("lang/en.yml")?.use { input ->
            fallback = YamlConfiguration.loadConfiguration(input.reader())
        }
        // load selected lang: dataFolder overrides jar
        val selectedFile = File(langDir, "$lang.yml")
        messages = if (selectedFile.exists()) {
            YamlConfiguration.loadConfiguration(selectedFile)
        } else {
            plugin.getResource("lang/$lang.yml")?.use { input ->
                YamlConfiguration.loadConfiguration(input.reader())
            } ?: fallback
        }
        plugin.logger.info("I18n loaded lang=$lang (${messages.getKeys(false).size} keys)")
    }

    fun t(key: String, vararg placeholders: Pair<String, String>): String {
        var raw = messages.getString(key) ?: fallback.getString(key) ?: "[$key]"
        for ((k, v) in placeholders) {
            raw = raw.replace("{$k}", v)
        }
        // also support legacy config messages fallback
        return color(raw)
    }

    fun tp(key: String, vararg placeholders: Pair<String, String>): String {
        val prefix = messages.getString("prefix") ?: fallback.getString("prefix") ?: ""
        return color(prefix) + t(key, *placeholders)
    }

    fun send(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        sender.sendMessage(tp(key, *placeholders))
    }

    fun sendRaw(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        sender.sendMessage(t(key, *placeholders))
    }

    private fun color(s: String): String = ChatColor.translateAlternateColorCodes('&', s)

    fun reload() = load()

    fun availableLangs(): List<String> {
        val dir = File(plugin.dataFolder, "lang")
        return dir.listFiles()?.mapNotNull { it.nameWithoutExtension } ?: listOf("en", "de")
    }
}
