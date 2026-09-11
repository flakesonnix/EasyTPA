package com.easytpa.db

import java.sql.SQLException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Per-player teleport settings.
 * You code the logic — this is persistence skeleton.
 */
class TpaSettingsRepository(private val db: Database, private val log: Logger) {
    data class Settings(
        val uuid: UUID,
        val autoAccept: Boolean = false,
        val requestMode: String = "ASK", // ASK, BLOCK, AUTO, WHITELIST
        val whitelistEnabled: Boolean = false,
    )

    fun getOrDefault(uuid: UUID): Settings {
        val sql = "SELECT auto_accept, request_mode, whitelist_enabled FROM easytpa_settings WHERE uuid=?"
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, uuid.toString())
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            return Settings(uuid, rs.getBoolean(1), rs.getString(2), rs.getBoolean(3))
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "get settings failed", e)
        }
        return Settings(uuid)
    }

    fun upsert(s: Settings) {
        val sql = if (db.isSqlite()) {
            "INSERT INTO easytpa_settings(uuid, auto_accept, request_mode, whitelist_enabled) VALUES (?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET auto_accept=excluded.auto_accept, request_mode=excluded.request_mode, whitelist_enabled=excluded.whitelist_enabled, updated_at=CURRENT_TIMESTAMP"
        } else {
            "INSERT INTO easytpa_settings(uuid, auto_accept, request_mode, whitelist_enabled) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE auto_accept=VALUES(auto_accept), request_mode=VALUES(request_mode), whitelist_enabled=VALUES(whitelist_enabled)"
        }
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, s.uuid.toString())
                    ps.setBoolean(2, s.autoAccept)
                    ps.setString(3, s.requestMode)
                    ps.setBoolean(4, s.whitelistEnabled)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "upsert settings failed", e)
        }
    }

    // Toggle helper functions for individual settings
    fun setAutoAccept(uuid: UUID, enabled: Boolean) = upsert(getOrDefault(uuid).copy(autoAccept = enabled))

    fun setRequestMode(uuid: UUID, mode: String) = upsert(getOrDefault(uuid).copy(requestMode = mode))

    fun setWhitelistEnabled(uuid: UUID, enabled: Boolean) = upsert(getOrDefault(uuid).copy(whitelistEnabled = enabled))
}
