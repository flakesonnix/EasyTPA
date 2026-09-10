package com.easytpa

import com.easytpa.command.TpAcceptCommand
import com.easytpa.command.TpDenyCommand
import com.easytpa.command.TpaCommand
import com.easytpa.command.TpaSettingsCommand
import com.easytpa.command.TpaWhitelistCommand
import com.easytpa.db.Database
import com.easytpa.db.TpaSettingsRepository
import com.easytpa.db.WhitelistRepository
import com.easytpa.service.TeleportService
import org.bukkit.plugin.java.JavaPlugin

class EasyTPAPlugin : JavaPlugin() {
    lateinit var database: Database
        private set
    lateinit var settingsRepo: TpaSettingsRepository
        private set
    lateinit var whitelistRepo: WhitelistRepository
        private set
    lateinit var teleportService: TeleportService
        private set

    override fun onEnable() {
        saveDefaultConfig()
        // DB
        database = Database(this)
        try {
            database.connect()
            database.migrate()
        } catch (e: Exception) {
            logger.severe("DB init failed — plugin still runs but DB disabled: ${e.message}")
            e.printStackTrace()
        }
        settingsRepo = TpaSettingsRepository(database, logger)
        whitelistRepo = WhitelistRepository(database, logger)
        teleportService = TeleportService(this, settingsRepo, whitelistRepo)

        // Commands — Paper & Spigot compatible (plugin.yml)
        getCommand("tpa")?.let {
            val c = TpaCommand(teleportService)
            it.setExecutor(c)
            it.tabCompleter = c
        }
        getCommand("tpaccept")?.setExecutor(TpAcceptCommand(teleportService))
        getCommand("tpdeny")?.setExecutor(TpDenyCommand(teleportService))
        getCommand("tpasettings")?.let {
            val c = TpaSettingsCommand(settingsRepo)
            it.setExecutor(c)
            it.tabCompleter = c
        }
        getCommand("tpawhitelist")?.let {
            val c = TpaWhitelistCommand(whitelistRepo)
            it.setExecutor(c)
            it.tabCompleter = c
        }

        logger.info("EasyTPA enabled (DB=${if (database.isConnected()) "ok" else "off"}) — Paper & Spigot")
    }

    override fun onDisable() {
        if (::database.isInitialized) database.close()
        logger.info("EasyTPA disabled")
    }
}
