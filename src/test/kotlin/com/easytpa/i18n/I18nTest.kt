package com.easytpa.i18n

import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class I18nTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(lang: String): JavaPlugin {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("language", lang)
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.getResource(any()) } answers {
            when (firstArg<String>()) {
                "lang/en.yml" -> "prefix: \"&b[EasyTPA] \"\nhello: \"Hello {player}\"".byteInputStream()
                "lang/de.yml" -> "prefix: \"&b[EasyTPA-DE] \"\nhallo: \"Hallo {player}\"".byteInputStream()
                else -> null
            }
        }
        every { plugin.logger } returns mockk(relaxed = true)
        return plugin
    }

    @Test
    fun `t replaces and colors`() {
        val i18n = I18n(mockPlugin("en"))
        i18n.load()
        val out = i18n.t("hello", "player" to "Bob")
        assertTrue(out.contains("Hello") && out.contains("Bob"))
    }

    @Test
    fun `tp adds prefix`() {
        val i18n = I18n(mockPlugin("en"))
        i18n.load()
        val out = i18n.tp("hello", "player" to "Bob")
        assertTrue(out.contains("EasyTPA") || out.contains("Bob"))
    }
}
