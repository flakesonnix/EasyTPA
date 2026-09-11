# Database — EasyTPA

Tables: `tpa_settings` (uuid, auto_accept, mode) and `tpa_whitelist` (owner, allowed).

Repos: `TpaSettingsRepository` and `WhitelistRepository` — simple upsert/select/delete via HikariCP.

SQLite default pool 1, mysql/pg pool 10. Requests themselves are in-memory (expiry), not DB.
