# Architecture — EasyTPA

```
Paper/Spigot
  └─ EasyTPAPlugin
       ├─ Database — HikariCP
       ├─ TpaSettingsRepository — per-player autoAccept/mode
       ├─ WhitelistRepository — per-player whitelist
       ├─ TeleportService — requests map, expiry, cooldown, warmup
       └─ Commands — /tpa, /tpaccept, /tpdeny, /tpasettings, /tpawhitelist
```

Flow: `/tpa target` → checks cooldown/whitelist/mode → store request → target runs `/tpaccept` → warmup 3s (cancel on move) → teleport.

All DB writes async. Whitelist and settings persisted.
