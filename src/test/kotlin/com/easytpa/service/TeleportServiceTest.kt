package com.easytpa.service

import com.easytpa.db.Database
import com.easytpa.db.TpaSettingsRepository
import com.easytpa.db.WhitelistRepository
import com.easytpa.i18n.I18n
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TeleportServiceTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(): Triple<JavaPlugin, TpaSettingsRepository, WhitelistRepository> {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("database.type", "sqlite")
        config.set("database.sqlite.file", "test.db")
        config.set("database.pool.maximum-pool-size", 1)
        config.set("database.pool.minimum-idle", 1)
        config.set("language", "en")
        config.set("tpa.request-expiry-seconds", 60)
        config.set("tpa.cooldown-seconds", 0)
        config.set("tpa.auto-accept.enabled", false)
        config.set("tpa.teleport-delay-seconds", 0)
        every { plugin.config } returns config
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        every { plugin.getResource(any()) } answers { null }
        // mock server scheduler
        val server = mockk<Server>(relaxed = true)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTask(any(), any<Runnable>()) } answers {
            (secondArg<Runnable>()).run()
            mockk(relaxed = true)
        }
        every { scheduler.runTaskLater(any(), any<Runnable>(), any()) } answers { mockk(relaxed = true) }
        every { scheduler.runTaskTimer(any(), any<Runnable>(), any(), any()) } answers { mockk(relaxed = true) }

        val db = Database(plugin)
        db.connect()
        db.migrate()
        val settings = TpaSettingsRepository(db, mockk(relaxed = true))
        val whitelist = WhitelistRepository(db, mockk(relaxed = true))
        val i18n = I18n(plugin)
        // mock i18n load to avoid file
        every { plugin.getResource("lang/en.yml") } returns "prefix: \"[T] \"\nrequest-sent: \"sent {target}\"".byteInputStream()
        i18n.load()
        val service = TeleportService(plugin, settings, whitelist, i18n)
        return Triple(plugin, settings, whitelist)
    }

    @Test
    fun `canSend blocks self`() {
        val (plugin, _, _) = mockPlugin()
        val i18n = I18n(plugin).apply { load() }
        val db = Database(plugin).apply {
            connect()
            migrate()
        }
        val settings = TpaSettingsRepository(db, mockk(relaxed = true))
        val whitelist = WhitelistRepository(db, mockk(relaxed = true))
        val service = TeleportService(plugin, settings, whitelist, i18n)
        val p = mockk<Player>(relaxed = true)
        every { p.uniqueId } returns UUID.randomUUID()
        every { p.name } returns "Bob"
        // same player
        val res = service.canSend(p, p)
        assertNotNull(res)
        db.close()
    }

    @Test
    fun `canSend allows when no block`() {
        val (plugin, _, _) = mockPlugin()
        val i18n = I18n(plugin).apply { load() }
        val db = Database(plugin).apply {
            connect()
            migrate()
        }
        val settings = TpaSettingsRepository(db, mockk(relaxed = true))
        val whitelist = WhitelistRepository(db, mockk(relaxed = true))
        val service = TeleportService(plugin, settings, whitelist, i18n)
        val from = mockk<Player>(relaxed = true)
        val to = mockk<Player>(relaxed = true)
        every { from.uniqueId } returns UUID.randomUUID()
        every { to.uniqueId } returns UUID.randomUUID()
        every { from.name } returns "A"
        every { to.name } returns "B"
        every { from.hasPermission(any<String>()) } returns false
        every { to.hasPermission(any<String>()) } returns false
        val res = service.canSend(from, to)
        assertNull(res)
        db.close()
    }
}
