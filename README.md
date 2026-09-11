# EasyTPA

Paper & Spigot — Kotlin — simple teleport requests.

`/tpa` to request, `/tpaccept`/`/tpdeny` to respond. Per-player settings, auto-accept, whitelist, cooldowns — all DB-persisted (HikariCP sqlite/mysql/pg via PurrCore).

```bash
gradle shadowJar
# → build/libs/easytpa-1.0.0.jar
```

Requires `PurrCore`.

## Commands

- `/tpa <player>` — send request (`easytpa.tpa`)
- `/tpaccept [player]` / `/tpdeny [player]` — accept/deny
- `/tpasettings [autoAccept|mode|status]` — your preferences
- `/tpawhitelist <add|remove|list> [player]` — who can send to you

Warmup (`teleport-delay-seconds: 3`) cancels on move. Cooldown & expiry configurable.

## Config

`plugins/EasyTPA/config.yml`:

```yaml
tpa:
  request-expiry-seconds: 60
  cooldown-seconds: 5
  teleport-delay-seconds: 3
  cancel-on-move: true
language: en # en, de
```

Messages in `lang/en.yml`. See `docs/` for DB and permissions.

## Dev

```bash
nix develop
gradle spotlessApply && nix fmt
gradle shadowJar
```
