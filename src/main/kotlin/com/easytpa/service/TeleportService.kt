package com.easytpa.service

import com.easytpa.db.TpaSettingsRepository
import com.easytpa.db.WhitelistRepository
import com.easytpa.i18n.I18n
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Full EasyTPA service — Paper & Spigot compatible, i18n, DB-backed settings/whitelist.
 * Handles cooldown, block/whitelist/auto-accept, expiry, delay + move-cancel, sounds.
 */
class TeleportService(
    private val plugin: JavaPlugin,
    private val settingsRepo: TpaSettingsRepository,
    private val whitelistRepo: WhitelistRepository,
    private val i18n: I18n,
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

    private val pending = ConcurrentHashMap<UUID, Request>()
    private val cooldown = ConcurrentHashMap<UUID, Long>()
    private val teleporting = ConcurrentHashMap<UUID, Long>() // for move-cancel check

    fun canSend(from: Player, to: Player): String? {
        if (from == to) return i18n.t("self-request")
        val cdSec = plugin.config.getInt("tpa.cooldown-seconds", 5)
        if (cdSec > 0 && !from.hasPermission("easytpa.bypass.cooldown")) {
            val last = cooldown[from.uniqueId] ?: 0
            val now = System.currentTimeMillis()
            if (now - last < cdSec * 1000L) {
                val left = (cdSec * 1000L - (now - last)) / 1000
                return i18n.t("cooldown", "seconds" to left.toString())
            }
        }
        val settings = settingsRepo.getOrDefault(to.uniqueId)
        if (settings.requestMode == "BLOCK") return i18n.t("blocked", "target" to to.name)
        if (settings.whitelistEnabled || settings.requestMode == "WHITELIST") {
            if (!whitelistRepo.isWhitelisted(to.uniqueId, from.uniqueId)) {
                return i18n.t("whitelist-only", "target" to to.name)
            }
        }
        if (pending[to.uniqueId]?.let { !it.isExpired() } == true) return i18n.t("target-has-request", "target" to to.name)
        if (pending.values.any { it.from == from.uniqueId && !it.isExpired() }) return i18n.t("already-has-request")
        return null
    }

    fun send(from: Player, to: Player): String {
        val block = canSend(from, to)
        if (block != null) return block

        val settings = settingsRepo.getOrDefault(to.uniqueId)
        val autoEnabled = plugin.config.getBoolean("tpa.auto-accept.enabled", true)
        val autoPerm = plugin.config.getString("tpa.auto-accept.permission", "easytpa.autoaccept")!!
        val shouldAuto = autoEnabled && settings.autoAccept && (from.hasPermission(autoPerm) || whitelistRepo.isWhitelisted(to.uniqueId, from.uniqueId))
        if (shouldAuto || settings.requestMode == "AUTO") {
            // instant auto-accept → teleport with delay
            doTeleport(from, to, isAuto = true)
            return i18n.t("accepted", "player" to to.name)
        }

        val expiryMs = plugin.config.getInt("tpa.request-expiry-seconds", 60) * 1000L
        val req = Request(from.uniqueId, from.name, to.uniqueId, to.name, expiryMs = expiryMs)
        pending[to.uniqueId] = req
        cooldown[from.uniqueId] = System.currentTimeMillis()
        plugin.server.scheduler.runTaskLater(plugin, Runnable { pending.remove(to.uniqueId, req) }, expiryMs / 50)

        // notify
        to.sendMessage(i18n.tp("request-received", "requester" to from.name))
        playSound(to, "on-request")
        return i18n.t("request-sent", "target" to to.name)
    }

    fun accept(target: Player, requesterName: String? = null): String {
        val req = findRequest(target, requesterName) ?: return i18n.t("no-request")
        if (req.isExpired()) {
            pending.remove(target.uniqueId, req)
            return i18n.t("request-expired")
        }
        pending.remove(target.uniqueId, req)
        val requester = plugin.server.getPlayer(req.from) ?: run {
            target.sendMessage(i18n.tp("player-not-found", "player" to req.fromName))
            return i18n.t("player-not-found", "player" to req.fromName)
        }
        doTeleport(requester, target, isAuto = false)
        playSound(target, "on-accept")
        playSound(requester, "on-accept")
        return "OK"
    }

    fun deny(target: Player, requesterName: String? = null): String {
        val req = findRequest(target, requesterName) ?: return i18n.t("no-request")
        pending.remove(target.uniqueId, req)
        plugin.server.getPlayer(req.from)?.let {
            it.sendMessage(i18n.tp("denied"))
            playSound(it, "on-deny")
        }
        playSound(target, "on-deny")
        return i18n.t("denied")
    }

    private fun findRequest(target: Player, name: String?): Request? = if (name != null) {
        val p = plugin.server.getPlayer(name) ?: plugin.server.getOfflinePlayer(name)
        pending[target.uniqueId]?.takeIf { it.from == p.uniqueId }
    } else {
        pending[target.uniqueId]
    }

    private fun doTeleport(player: Player, target: Player, isAuto: Boolean) {
        val delaySec = plugin.config.getInt("tpa.teleport-delay-seconds", 3)
        val cancelOnMove = plugin.config.getBoolean("tpa.cancel-on-move", true)

        if (delaySec <= 0) {
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    player.teleport(target.location)
                    player.sendMessage(i18n.tp("teleported"))
                    if (!isAuto) target.sendMessage(i18n.tp("teleported"))
                },
            )
            return
        }

        player.sendMessage(i18n.tp("teleport-delay", "seconds" to delaySec.toString()))
        val startLoc = player.location.clone()
        val startTime = System.currentTimeMillis()
        teleporting[player.uniqueId] = startTime

        // check every 5 ticks for move
        val task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                val now = System.currentTimeMillis()
                // if new teleport started, cancel old
                if (teleporting[player.uniqueId] != startTime) return@Runnable
                if (cancelOnMove && hasMoved(startLoc, player.location)) {
                    teleporting.remove(player.uniqueId, startTime)
                    player.sendMessage(i18n.tp("teleport-cancelled-move"))
                    // cancel timer by scheduling immediate cancel? we use extra map check
                    // actual cancellation via removing from map; timer will still run but no-op
                }
            },
            0L,
            5L,
        )

        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                task.cancel()
                if (teleporting[player.uniqueId] != startTime) return@Runnable // moved/cancelled
                if (cancelOnMove && hasMoved(startLoc, player.location)) {
                    teleporting.remove(player.uniqueId)
                    player.sendMessage(i18n.tp("teleport-cancelled-move"))
                    return@Runnable
                }
                teleporting.remove(player.uniqueId)
                if (!player.isOnline || !target.isOnline) return@Runnable
                player.teleport(target.location)
                player.sendMessage(i18n.tp("teleported"))
                if (!isAuto) target.sendMessage(i18n.t("teleported"))
                playSound(player, "on-accept")
            },
            delaySec * 20L,
        )
    }

    private fun hasMoved(a: org.bukkit.Location, b: org.bukkit.Location): Boolean {
        if (a.world != b.world) return true
        return a.distanceSquared(b) > 0.25 // >0.5 blocks
    }

    private fun playSound(p: Player, key: String) {
        if (!plugin.config.getBoolean("tpa.sound.$key", true)) return
        try {
            // Paper & Spigot compatible sounds
            val sound = when (key) {
                "on-request" -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                "on-accept" -> Sound.ENTITY_ENDERMAN_TELEPORT
                "on-deny" -> Sound.ENTITY_VILLAGER_NO
                else -> Sound.BLOCK_NOTE_BLOCK_PLING
            }
            p.playSound(p.location, sound, 1f, 1f)
        } catch (_: Exception) {
            // ignore missing sound on older Spigot
        }
    }

    fun hasPending(to: UUID) = pending[to]?.let { !it.isExpired() } ?: false
    fun clearAll() {
        pending.clear()
        cooldown.clear()
        teleporting.clear()
    }
}
