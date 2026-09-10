package com.easytpa.db

import java.sql.SQLException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

class WhitelistRepository(private val db: Database, private val log: Logger) {
    fun isWhitelisted(owner: UUID, allowed: UUID): Boolean {
        val sql = "SELECT 1 FROM easytpa_whitelist WHERE owner_uuid=? AND allowed_uuid=? LIMIT 1"
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, owner.toString())
                    ps.setString(2, allowed.toString())
                    ps.executeQuery().use { rs -> return rs.next() }
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "whitelist check failed", e)
        }
        return false
    }

    fun add(owner: UUID, allowed: UUID, allowedName: String): Boolean {
        val sql = "INSERT INTO easytpa_whitelist(owner_uuid, allowed_uuid, allowed_name) VALUES (?,?,?)"
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, owner.toString())
                    ps.setString(2, allowed.toString())
                    ps.setString(3, allowedName)
                    ps.executeUpdate()
                    return true
                }
            }
        } catch (e: SQLException) {
            // duplicate → already whitelisted
            if (e.message?.contains("Duplicate") == true || e.message?.contains("UNIQUE") == true) return false
            log.log(Level.WARNING, "whitelist add failed", e)
        }
        return false
    }

    fun remove(owner: UUID, allowed: UUID): Boolean {
        val sql = "DELETE FROM easytpa_whitelist WHERE owner_uuid=? AND allowed_uuid=?"
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, owner.toString())
                    ps.setString(2, allowed.toString())
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "whitelist remove failed", e)
        }
        return false
    }

    fun list(owner: UUID): List<Pair<UUID, String>> {
        val sql = "SELECT allowed_uuid, allowed_name FROM easytpa_whitelist WHERE owner_uuid=? ORDER BY allowed_name"
        val out = mutableListOf<Pair<UUID, String>>()
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, owner.toString())
                    ps.executeQuery().use { rs ->
                        while (rs.next()) out += UUID.fromString(rs.getString(1)) to rs.getString(2)
                    }
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "whitelist list failed", e)
        }
        return out
    }
}
