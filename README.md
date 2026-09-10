# EasyTPA — Paper & Spigot

[![CI](https://github.com/YOU/EasyTPA/actions/workflows/ci.yml/badge.svg)](https://github.com/YOU/EasyTPA/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.26.2-0288D1?logo=minecraft)](https://papermc.io)
[![Spigot](https://img.shields.io/badge/Spigot-compatible-ED8B00)](https://www.spigotmc.org)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org)

Lightweight, configurable teleport request plugin for **Paper and Spigot**. Players send/accept/deny requests, control how they receive them. Supports **per-player settings**, **auto-accept**, **whitelist**, all **persistently stored in DB** (SQLite/MySQL/PostgreSQL via HikariCP).

> Built from [`flakesonnix/paper-kotlin-template`](https://github.com/flakesonnix/paper-kotlin-template) — Kotlin 2.0.21, Nix flake, Spotless/ktlint, IDEA, CI. You code — this is skeleton.

## Features (skeleton ready — you implement logic)

- `/tpa <player>` — send request (cooldown, expiry, block mode, whitelist check)
- `/tpaccept [player]` / `/tpdeny [player]` — accept/deny (teleport with delay/move-cancel)
- `/tpasettings autoAccept <on|off> | mode <ASK|BLOCK|AUTO|WHITELIST> | status` — per-player, DB persistent
- `/tpawhitelist add|remove|list [player]` — whitelist persistent
- Auto-accept + whitelist logic in `TeleportService`
- DB: `easytpa_settings`, `easytpa_whitelist`, `easytpa_history` (HikariCP, sqlite default)
- Paper & Spigot compatible (`plugin.yml` + `paper-plugin.yml`, only Bukkit API)
- Lightweight, async DB, configurable `config.yml`

## Quick Start

```bash
# via Nix (recommended)
nix develop
gradle shadowJar # → build/libs/easytpa-1.0.0.jar

# without Nix
gradle shadowJar # JDK 21 required
```

Copy `build/libs/easytpa-1.0.0.jar` → `plugins/` → restart. SQLite creates `plugins/EasyTPA/database.db`.

Test: `/tpa <player>` → target sees request → `/tpaccept`.

## Commands & Permissions

| Command | Permission | Default |
|---------|------------|---------|
| `/tpa <player>` | `easytpa.tpa` | true |
| `/tpaccept [player]` | `easytpa.tpaccept` | true |
| `/tpdeny [player]` | `easytpa.tpdeny` | true |
| `/tpasettings ...` | `easytpa.settings` | true |
| `/tpawhitelist ...` | `easytpa.whitelist` | true |
| `easytpa.admin` | op |

## Configuration

`src/main/resources/config.yml` → `plugins/EasyTPA/config.yml`:

```yaml
database: { type: sqlite, sqlite: {file: database.db} } # or mysql/postgresql
tpa:
  request-expiry-seconds: 60
  cooldown-seconds: 5
  teleport-delay-seconds: 3
  cancel-on-move: true
  auto-accept: { enabled: true, permission: easytpa.autoaccept }
  whitelist: { enabled: true }
```

See [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) + [`docker-compose.yml`](docker-compose.yml) for MySQL/Postgres via Docker.

## Development — you code

Skeleton is in `src/main/kotlin/com/easytpa/`:

- `EasyTPAPlugin.kt` — wires DB + repos + `TeleportService` + commands
- `service/TeleportService.kt` — `send/canSend/accept/deny`, expiry, cooldown, auto-accept/whitelist checks — **TODO: teleport delay + move cancel**
- `db/TpaSettingsRepository.kt` — `getOrDefault/upsert` — **TODO: you add toggles**
- `db/WhitelistRepository.kt` — `isWhitelisted/add/remove/list`
- `command/*` — `TpaCommand`, `TpAcceptCommand`, etc. — stub, you extend

Build via flake: `nix develop -c gradle shadowJar` (Kotlin-JVM, Paper API). Formatter: `gradle spotlessApply` (ktlint 1.5.0), `nix fmt` (nixfmt). IDEA: `nix run .#idea`.

## Project Structure

```
EasyTPA/
├── flake.nix (jdk21, gradle, idea, nixfmt)
├── build.gradle.kts (kotlin 2.0.21, spotless, paper-api, Hikari)
├── src/main/kotlin/com/easytpa/{EasyTPAPlugin,service,command,db}
├── src/main/resources/{plugin.yml,paper-plugin.yml,config.yml}
├── .idea/ (gradle.xml, misc.xml JDK21, codeStyles, runConfigs)
└── docs/ (DATABASE, CONFIGURATION, etc.)
```

## From Template

This was created via `gh repo create EasyTPA --template flakesonnix/paper-kotlin-template`. See template's [GETTING_STARTED](docs/GETTING_STARTED.md) for rename steps.

## License

MIT — see `LICENSE`.

*You code — template handles boilerplate.*
