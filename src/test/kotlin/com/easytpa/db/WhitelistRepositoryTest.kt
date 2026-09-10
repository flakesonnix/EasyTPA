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

class WhitelistRepositoryTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(): JavaPlugin {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "test.db")
        c.set("database.pool.maximum-pool-size", 1)
        c.set("database.pool.minimum-idle", 1)
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
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
    fun `add and isWhitelisted`() {
        val db = db()
        val repo = WhitelistRepository(db, mockk(relaxed = true))
        val owner = UUID.randomUUID()
        val allowed = UUID.randomUUID()
        assertFalse(repo.isWhitelisted(owner, allowed))
        assertTrue(repo.add(owner, allowed, "Test"))
        assertTrue(repo.isWhitelisted(owner, allowed))
        assertFalse(repo.add(owner, allowed, "Test"))
        db.close()
    }

    @Test
    fun `remove`() {
        val db = db()
        val repo = WhitelistRepository(db, mockk(relaxed = true))
        val owner = UUID.randomUUID()
        val allowed = UUID.randomUUID()
        repo.add(owner, allowed, "A")
        assertTrue(repo.remove(owner, allowed))
        assertFalse(repo.isWhitelisted(owner, allowed))
        assertFalse(repo.remove(owner, allowed))
        db.close()
    }

    @Test
    fun `list`() {
        val db = db()
        val repo = WhitelistRepository(db, mockk(relaxed = true))
        val owner = UUID.randomUUID()
        val a1 = UUID.randomUUID()
        val a2 = UUID.randomUUID()
        repo.add(owner, a1, "Alpha")
        repo.add(owner, a2, "Beta")
        val list = repo.list(owner)
        assertEquals(2, list.size)
        assertTrue(list.any { it.first == a1 })
        db.close()
    }
}
