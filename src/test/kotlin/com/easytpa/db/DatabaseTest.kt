package com.easytpa.db

import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatabaseTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(): JavaPlugin {
        val p = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "test.db")
        c.set("database.pool.maximum-pool-size", 1)
        every { p.config } returns c
        every { p.dataFolder } returns tempDir
        every { p.logger } returns mockk(relaxed = true)
        return p
    }

    @Test
    fun `migrate creates tables`() {
        val db = Database(mockPlugin())
        db.connect()
        assertTrue(db.isConnected())
        db.migrate()
        db.getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='easytpa_settings'").use { rs ->
                    assertTrue(rs.next())
                }
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='easytpa_whitelist'").use { rs ->
                    assertTrue(rs.next())
                }
            }
        }
        db.close()
        assertFalse(db.isConnected())
    }

    @Test
    fun `isSqlite true for sqlite`() {
        val db = Database(mockPlugin())
        db.connect()
        assertTrue(db.isSqlite())
        db.close()
    }
}
