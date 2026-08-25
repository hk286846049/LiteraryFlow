# LiteraryFlow 2.0 Database

The new database is `literary_flow_2.db`.

Core tables:

- `lf2_tasks`: task configuration and schedule.
- `lf2_steps`: ordered task steps.
- `lf2_actions`: executable actions.
- `lf2_conditions`: perception conditions and branch actions.
- `lf2_run_records`: task run lifecycle.
- `lf2_step_records`: per-step lifecycle.
- `lf2_app_profiles`: app-specific recognition and orientation defaults.

Current Room schema version is 4. Migrations are kept explicit:

- v1 -> v2 adds task favorite/category/duration/run-count metadata.
- v2 -> v3 adds structured condition exclusion text.
- v3 -> v4 adds app-profile favorites.

Task list state (favorite, category, enabled, schedule) and app-profile favorites are persisted; the UI does not use prototype mock records.

The old `auto_info` database is read-only during first launch migration. `LegacyAutoInfoMapper` converts old values into normalized v2 records; it does not mutate the old schema.
