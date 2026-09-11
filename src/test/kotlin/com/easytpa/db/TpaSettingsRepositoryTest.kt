package com.easytpa.db

import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TpaSettingsRepositoryTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(): JavaPlugin {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("database.type", "sqlite")
        config.set("database.sqlite.file", "test.db")
        config.set("database.pool.maximum-pool-size", 1)
        config.set("database.pool.minimum-idle", 1)
        config.set("language", "en")
        every { plugin.config } returns config
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        every { plugin.getResource(any()) } answers { null }
        return plugin
    }

    private fun db(): Database {
        val p = mockPlugin()
        val db = Database(p)
        db.connect()
        db.migrate()
        return db
    }

    @Test
    fun `getOrDefault returns defaults when not exists`() {
        val db = db()
        val repo = TpaSettingsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        val s = repo.getOrDefault(uuid)
        assertEquals(uuid, s.uuid)
        assertFalse(s.autoAccept)
        assertEquals("ASK", s.requestMode)
        db.close()
    }

    @Test
    fun `upsert and get`() {
        val db = db()
        val repo = TpaSettingsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        repo.upsert(TpaSettingsRepository.Settings(uuid, true, "AUTO", true))
        val loaded = repo.getOrDefault(uuid)
        assertTrue(loaded.autoAccept)
        assertEquals("AUTO", loaded.requestMode)
        assertTrue(loaded.whitelistEnabled)
        db.close()
    }

    @Test
    fun `upsert updates existing`() {
        val db = db()
        val repo = TpaSettingsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        repo.upsert(TpaSettingsRepository.Settings(uuid, false, "ASK", false))
        repo.upsert(TpaSettingsRepository.Settings(uuid, true, "BLOCK", true))
        val loaded = repo.getOrDefault(uuid)
        assertTrue(loaded.autoAccept)
        assertEquals("BLOCK", loaded.requestMode)
        db.close()
    }
}
