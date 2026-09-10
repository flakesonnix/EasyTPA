package com.easytpa.service

import com.easytpa.db.TpaSettingsRepository
import com.easytpa.db.WhitelistRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * In-memory request manager + DB-backed settings/whitelist.
 * You code the teleport logic — skeleton ready for Paper & Spigot.
 */
class TeleportService(
    private val plugin: JavaPlugin,
    private val settingsRepo: TpaSettingsRepository,
    private val whitelistRepo: WhitelistRepository,
) {
    data class Request(
        val from: UUID,
        val fromName: String,
        val to: UUID,
        val toName: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiryMs: Long = 60_000,
    ) {
        fun isExpired() = System.currentTimeMillis() - createdAt > expiryMs
    }

    // toUuid -> Request (one pending per target for simplicity)
    private val pending = ConcurrentHashMap<UUID, Request>()
    private val cooldown = ConcurrentHashMap<UUID, Long>()

    fun canSend(from: Player, to: Player): String? {
        // TODO: implement checks — cooldown, target settings, whitelist, block mode
        // Example skeleton:
        val expirySec = plugin.config.getInt("tpa.request-expiry-seconds", 60)
        val cdSec = plugin.config.getInt("tpa.cooldown-seconds", 5)
        val now = System.currentTimeMillis()
        val last = cooldown[from.uniqueId] ?: 0
        if (now - last < cdSec * 1000L) {
            val left = (cdSec * 1000L - (now - last)) / 1000
            return plugin.config.getString("messages.cooldown")?.replace("{seconds}", left.toString()) ?: "Cooldown $left s"
        }
        val settings = settingsRepo.getOrDefault(to.uniqueId)
        if (settings.requestMode == "BLOCK") return plugin.config.getString("messages.blocked")?.replace("{target}", to.name) ?: "${to.name} blocks requests"
        if (settings.whitelistEnabled || settings.requestMode == "WHITELIST") {
            if (!whitelistRepo.isWhitelisted(to.uniqueId, from.uniqueId)) {
                return plugin.config.getString("messages.blocked")?.replace("{target}", to.name) ?: "Not whitelisted"
            }
        }
        if (pending.containsKey(to.uniqueId)) return plugin.config.getString("messages.target-has-request") ?: "Target busy"
        if (pending.values.any { it.from == from.uniqueId }) return plugin.config.getString("messages.already-has-request") ?: "You already sent one"
        // auto-accept check is done in send()
        return null // ok
    }

    fun send(from: Player, to: Player): String {
        val block = canSend(from, to)
        if (block != null) return block

        val settings = settingsRepo.getOrDefault(to.uniqueId)
        // auto-accept: if target has autoAccept and you are whitelisted/have perm
        val autoAcceptEnabled = plugin.config.getBoolean("tpa.auto-accept.enabled", true)
        val autoPerm = plugin.config.getString("tpa.auto-accept.permission", "easytpa.autoaccept")!!
        val shouldAuto = autoAcceptEnabled && settings.autoAccept && (from.hasPermission(autoPerm) || whitelistRepo.isWhitelisted(to.uniqueId, from.uniqueId))
        if (shouldAuto || settings.requestMode == "AUTO") {
            // TODO: teleport immediately — add delay/move-cancel if config
            pending.remove(to.uniqueId)
            // Example teleport (Paper & Spigot compatible):
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    from.teleport(to.location)
                    from.sendMessage(plugin.config.getString("messages.teleported") ?: "Teleported!")
                },
            )
            return plugin.config.getString("messages.accepted")?.replace("{player}", to.name) ?: "Auto-accepted"
        }

        val expiryMs = plugin.config.getInt("tpa.request-expiry-seconds", 60) * 1000L
        val req = Request(from.uniqueId, from.name, to.uniqueId, to.name, expiryMs = expiryMs)
        pending[to.uniqueId] = req
        cooldown[from.uniqueId] = System.currentTimeMillis()

        // expire task
        plugin.server.scheduler.runTaskLater(plugin, Runnable { pending.remove(to.uniqueId, req) }, expiryMs / 50)

        // notify target (Paper & Spigot)
        val prefix = plugin.config.getString("messages.prefix") ?: "[EasyTPA] "
        val msgReceived = plugin.config.getString("messages.request-received")?.replace("{requester}", from.name) ?: "${from.name} wants to tp"
        to.sendMessage(prefix + msgReceived)
        if (plugin.config.getBoolean("tpa.sound.on-request", true)) {
            // TODO: play sound if you want
        }
        return plugin.config.getString("messages.request-sent")?.replace("{target}", to.name) ?: "Sent to ${to.name}"
    }

    fun accept(target: Player, requesterName: String? = null): String {
        val req = if (requesterName != null) {
            val p = plugin.server.getPlayer(requesterName) ?: return plugin.config.getString("messages.no-request") ?: "No request"
            pending[target.uniqueId]?.takeIf { it.from == p.uniqueId }
        } else {
            pending[target.uniqueId]
        } ?: return plugin.config.getString("messages.no-request") ?: "No request"

        if (req.isExpired()) {
            pending.remove(target.uniqueId, req)
            return plugin.config.getString("messages.no-request") ?: "Expired"
        }
        pending.remove(target.uniqueId, req)
        val requester = plugin.server.getPlayer(req.from) ?: return "Requester offline"
        // TODO: delay & move check
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                requester.teleport(target.location)
                requester.sendMessage(plugin.config.getString("messages.accepted")?.replace("{player}", target.name) ?: "Accepted")
                target.sendMessage(plugin.config.getString("messages.teleported") ?: "Teleported")
            },
        )
        return "OK"
    }

    fun deny(target: Player, requesterName: String? = null): String {
        val req = if (requesterName != null) {
            val p = plugin.server.getPlayer(requesterName) ?: return plugin.config.getString("messages.no-request") ?: "No request"
            pending[target.uniqueId]?.takeIf { it.from == p.uniqueId }
        } else {
            pending[target.uniqueId]
        } ?: return plugin.config.getString("messages.no-request") ?: "No request"
        pending.remove(target.uniqueId, req)
        plugin.server.getPlayer(req.from)?.sendMessage(plugin.config.getString("messages.denied") ?: "Denied")
        return plugin.config.getString("messages.denied") ?: "Denied"
    }

    fun hasPending(to: UUID) = pending[to]?.let { !it.isExpired() } ?: false
}
