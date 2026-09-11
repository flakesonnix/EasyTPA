package com.easytpa.command

import com.easytpa.db.TpaSettingsRepository
import com.easytpa.db.WhitelistRepository
import com.easytpa.i18n.I18n
import com.easytpa.service.TeleportService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.bukkit.Server
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandsTest {

    private fun mockI18n(): I18n {
        val i18n = mockk<I18n>(relaxed = true)
        every { i18n.t(any(), *anyVararg()) } returns "msg"
        every { i18n.tp(any(), *anyVararg()) } returns "msg"
        return i18n
    }

    @Test
    fun `TpaCommand only players`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaCommand(service, i18n)
        val sender = mockk<org.bukkit.command.CommandSender>(relaxed = true)
        val res = cmd.onCommand(sender, mockk(relaxed = true), "tpa", arrayOf())
        assertTrue(res)
        verify { i18n.send(sender, "only-players") }
    }

    @Test
    fun `TpaCommand no args sends usage`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaCommand(service, i18n)
        val player = mockk<Player>(relaxed = true)
        cmd.onCommand(player, mockk(relaxed = true), "tpa", arrayOf())
        verify { i18n.send(player, "usage-tpa") }
    }

    @Test
    fun `TpaCommand self request`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaCommand(service, i18n)
        val player = mockk<Player>(relaxed = true)
        every { player.name } returns "Steve"
        val server = mockk<Server>(relaxed = true)
        every { player.server } returns server
        every { server.getPlayer("Steve") } returns player
        cmd.onCommand(player, mockk(relaxed = true), "tpa", arrayOf("Steve"))
        verify { i18n.send(player, "self-request") }
    }

    @Test
    fun `TpaCommand player not found`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaCommand(service, i18n)
        val player = mockk<Player>(relaxed = true)
        val server = mockk<Server>(relaxed = true)
        every { player.server } returns server
        every { server.getPlayer("NotExist") } returns null
        cmd.onCommand(player, mockk(relaxed = true), "tpa", arrayOf("NotExist"))
        verify { i18n.send(player, "player-not-found", any()) }
    }

    @Test
    fun `TpAccept only players`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpAcceptCommand(service, i18n)
        val sender = mockk<org.bukkit.command.CommandSender>(relaxed = true)
        cmd.onCommand(sender, mockk(relaxed = true), "tpaccept", arrayOf())
        verify { i18n.send(sender, "only-players") }
    }

    @Test
    fun `TpAccept delegates to service`() {
        val service = mockk<TeleportService>(relaxed = true)
        every { service.accept(any(), any()) } returns "OK"
        val i18n = mockI18n()
        val cmd = TpAcceptCommand(service, i18n)
        val player = mockk<Player>(relaxed = true)
        val res = cmd.onCommand(player, mockk(relaxed = true), "tpaccept", arrayOf("Bob"))
        assertTrue(res)
        verify { service.accept(player, "Bob") }
    }

    @Test
    fun `TpDeny delegates`() {
        val service = mockk<TeleportService>(relaxed = true)
        every { service.deny(any(), any()) } returns "msg"
        val i18n = mockI18n()
        val cmd = TpDenyCommand(service, i18n)
        val player = mockk<Player>(relaxed = true)
        cmd.onCommand(player, mockk(relaxed = true), "tpdeny", arrayOf())
        verify { service.deny(player, null) }
    }

    @Test
    fun `TpaSettings only players`() {
        val repo = mockk<TpaSettingsRepository>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaSettingsCommand(repo, i18n)
        val sender = mockk<org.bukkit.command.CommandSender>(relaxed = true)
        cmd.onCommand(sender, mockk(relaxed = true), "tpasettings", arrayOf())
        verify { i18n.send(sender, "only-players") }
    }

    @Test
    fun `TpaSettings shows status when empty`() {
        val repo = mockk<TpaSettingsRepository>(relaxed = true)
        every { repo.getOrDefault(any()) } returns TpaSettingsRepository.Settings(UUID.randomUUID())
        val i18n = mockI18n()
        val cmd = TpaSettingsCommand(repo, i18n)
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        cmd.onCommand(player, mockk(relaxed = true), "tpasettings", arrayOf())
        verify { repo.getOrDefault(any()) }
    }

    @Test
    fun `TpaWhitelist add and list`() {
        val repo = mockk<WhitelistRepository>(relaxed = true)
        every { repo.add(any(), any(), any()) } returns true
        every { repo.list(any()) } returns emptyList()
        val i18n = mockI18n()
        val cmd = TpaWhitelistCommand(repo, i18n)
        val player = mockk<Player>(relaxed = true)
        val uuid = UUID.randomUUID()
        every { player.uniqueId } returns uuid
        every { player.name } returns "Owner"
        val server = mockk<Server>(relaxed = true)
        every { player.server } returns server
        val target = mockk<Player>(relaxed = true)
        every { target.uniqueId } returns UUID.randomUUID()
        every { target.name } returns "Target"
        every { server.getPlayer("Target") } returns target
        every { server.getOfflinePlayer(any<String>()) } returns target as org.bukkit.OfflinePlayer
        cmd.onCommand(player, mockk(relaxed = true), "tpawhitelist", arrayOf("add", "Target"))
        verify { repo.add(uuid, any(), "Target") }
        cmd.onCommand(player, mockk(relaxed = true), "tpawhitelist", arrayOf("list"))
        verify { repo.list(uuid) }
    }

    @Test
    fun `tabComplete filters`() {
        val service = mockk<TeleportService>(relaxed = true)
        val i18n = mockI18n()
        val cmd = TpaCommand(service, i18n)
        val sender = mockk<Player>(relaxed = true)
        val server = mockk<Server>(relaxed = true)
        every { sender.server } returns server
        val p1 = mockk<Player>(relaxed = true)
        every { p1.name } returns "Alice"
        val p2 = mockk<Player>(relaxed = true)
        every { p2.name } returns "Bob"
        every { server.onlinePlayers } returns listOf(p1, p2)
        val res = cmd.onTabComplete(sender, mockk(relaxed = true), "tpa", arrayOf("A"))
        assertTrue(res.contains("Alice"))
        assertTrue(!res.contains("Bob"))
    }
}
