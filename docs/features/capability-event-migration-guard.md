---
feature_id: capability-event-migration-guard
title: Capability Event Migration Guard
updated: 2026-03-12
---

# Capability Event Migration Guard

## What it does

Before processing any Kafka capability event, the consumer checks whether a Liquibase database migration is currently
running by inspecting the Liquibase lock table. If a migration is in progress, the event is rejected immediately and
retried with the configured back-off delay. This ensures capability events are never processed against an incomplete
database schema.

## Why it exists

During tenant initialization, Liquibase runs database migrations that create or alter tables, columns, and types.
If a Kafka capability event arrives during this window and proceeds to database operations, it encounters missing
schema objects and fails with SQL errors such as:

- `ERROR: relation "capability" does not exist`
- `ERROR: column "application_id" does not exist`
- `ERROR: type "capability_action" does not exist`

The migration guard catches this window proactively — before touching the database — and defers event processing
until the schema is ready. Events are not lost; they are retried according to the retry configuration and
processed successfully once the migration completes.

## Entry point(s)

**Kafka Consumer**

| Topic pattern                                                            | Event types                  | Listener method                              |
|--------------------------------------------------------------------------|------------------------------|----------------------------------------------|
| `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability` | `CREATE`, `UPDATE`, `DELETE` | `KafkaMessageListener#handleCapabilityEvent` |

The guard executes as the first step inside `handleCapabilityEvent`, before any tenant context or database
operations are initiated.

## Business rules and constraints

- The migration check occurs **before** any database operation for every incoming capability event.
- If `isMigrationRunning()` returns `true`, a `LiquibaseMigrationException` is thrown, which the error handler
  treats as a retryable condition with fixed back-off.
- If the Liquibase lock table does not yet exist (schema completely uninitialized), `isMigrationRunning()` returns
  `true` — the service treats a missing lock table as "migration in progress" and a `LiquibaseMigrationException`
  is thrown, triggering the same retry path.
- If querying the lock table fails for an unrelated reason (no "does not exist" in any cause), the service itself
  throws `LiquibaseMigrationException("Failed to determine Liquibase migration state")`, also triggering retry.
- `SQLGrammarException`/`PSQLException` containing `"does not exist"` acts as a secondary reactive safety net for
  any edge case where schema objects are missing but the lock check did not catch it first.
- Both `LiquibaseMigrationException` (proactive check) and `SQLGrammarException`/`PSQLException` containing
  `"does not exist"` (reactive detection) trigger identical retry behavior — they use the same `FixedBackOff`.
- Events are retried indefinitely by default (`Long.MAX_VALUE` attempts) until the migration completes.

## Error behavior

| Condition                                                                                                     | Result                                                      |
|---------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `LiquibaseMigrationException` (migration lock detected)                                                       | Retried with `FixedBackOff(retryDelay, retryAttempts)`      |
| `SQLGrammarException` wrapping `PSQLException` — message starts with `ERROR:` and contains `"does not exist"` | Retried with the same `FixedBackOff` (secondary safety net) |
| All other exceptions                                                                                          | Skipped immediately (`FixedBackOff(0, 0)`); failure logged  |

After all retry attempts are exhausted, the event is logged as a failed record and dropped.

## Configuration

| Variable                            | Purpose                                                                         |
|-------------------------------------|---------------------------------------------------------------------------------|
| `CAPABILITY_TOPIC_RETRY_ATTEMPTS`   | Maximum number of retry attempts for a single event (default: `Long.MAX_VALUE`) |
| `CAPABILITY_TOPIC_RETRY_DELAY`      | Delay between retry attempts (default: `1s`)                                    |

## Dependencies and interactions

- **`LiquibaseMigrationLockService`** (folio-spring-base) — queries the `DATABASECHANGELOGLOCK` table to determine
  whether a Liquibase lock is currently held; provides the `isMigrationRunning()` method.
- **`KafkaConfiguration`** — defines the `DefaultErrorHandler` that maps `LiquibaseMigrationException` to
  `FixedBackOff`; also maps SQL grammar errors to the same back-off as a secondary guard.
- **`KafkaAdminService`** — stops and starts Kafka listeners around reference data loading
  (`loadReferenceData()`) to prevent a separate race condition during that phase of tenant initialization;
  this complements the migration guard but is a distinct mechanism.
