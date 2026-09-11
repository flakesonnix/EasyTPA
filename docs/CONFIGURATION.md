# Configuration — EasyTPA

`plugins/EasyTPA/config.yml`:

```yaml
language: en
database: { type: sqlite, sqlite: { file: database.db } }
tpa:
  request-expiry-seconds: 60
  cooldown-seconds: 5
  teleport-delay-seconds: 3
  cancel-on-move: true
```

Messages in `lang/en.yml` and `de.yml`. Edit DB type → restart. See `config.yml` for mysql/pg.
