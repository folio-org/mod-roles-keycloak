---
feature_id: tenant-initialization-kafka-suspension
title: Kafka Listener Suspension During Tenant Initialization
status: active
updated: 2025-01-14
---

# Kafka Listener Suspension During Tenant Initialization

## What it does

Suspends all Kafka message listeners for the entire duration of the FOLIO Tenant API operation (`POST /_/tenant`). Kafka listeners are stopped immediately when the request arrives and restarted in a `finally` block after all tenant operations complete, regardless of success or failure. This ensures that no Kafka events are processed while database schema migrations (Liquibase) are running.

## Why it exists

Prevents a race condition between Kafka event processing and Liquibase database migrations. Without this protection, Kafka consumers may attempt to process capability events before database tables, columns, or types exist, causing transient SQL errors:

- `ERROR: relation "capability" does not exist`
- `ERROR: column "application_id" does not exist`
- `ERROR: type "capability_action" does not exist`

The Liquibase migration window varies based on schema complexity and database performance. By suspending Kafka listeners during this period, incoming events are buffered in Kafka and processed only after the database schema is ready.

## Endpoint(s)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/_/tenant` | FOLIO Tenant API - enables/upgrades/disables a tenant for this module |

## FOLIO Tenant API Operation Sequence

When `POST /_/tenant` is called, the following sequence executes:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    POST /_/tenant Request                           │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ TRY BLOCK                                                           │
│                                                                     │
│ 1. STOP KAFKA LISTENERS                                             │
│    kafkaAdminService.stopKafkaListeners()                           │
│    - All Kafka consumers pause immediately                          │
│    - Events buffer in Kafka until restart                           │
│                                                                     │
│ 2. TENANT OPERATION                                                 │
│                                                                     │
│    If DISABLE (moduleTo blank + purge=true):                        │
│    └── deleteTenant()                                               │
│        ├── cleanupDefaultRolesFromKeycloak()                        │
│        └── Drop tenant database schema                              │
│                                                                     │
│    If ENABLE/UPGRADE:                                               │
│    └── createOrUpdateTenant()                                       │
│        ├── beforeTenantUpdate()                                     │
│        ├── beforeLiquibaseUpdate()                                  │
│        ├── ══════════════════════════════════════                   │
│        │   LIQUIBASE MIGRATIONS RUN HERE                            │
│        │   (Tables, columns, types created)                         │
│        ├── ══════════════════════════════════════                   │
│        ├── afterLiquibaseUpdate()                                   │
│        └── afterTenantUpdate()                                      │
│            ├── Refresh Keycloak token                               │
│            └── Merge duplicate capabilities                         │
│                                                                     │
│    Then (if parameters present):                                    │
│    ├── loadReferenceData() [if loadReference=true]                  │
│    │   └── Load roles, policies from JSON files                     │
│    └── loadSampleData() [if loadSample=true]                        │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ FINALLY BLOCK (ALWAYS executes)                                     │
│                                                                     │
│ 3. START KAFKA LISTENERS (with error handling)                      │
│    kafkaAdminService.startKafkaListeners()                          │
│    - All Kafka consumers resume                                     │
│    - Buffered events begin processing                               │
│    - Database schema now ready for all operations                   │
│    - If restart fails: error logged, manual intervention required   │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    HTTP 204 No Content                              │
└─────────────────────────────────────────────────────────────────────┘
```

## Business rules and constraints

- Kafka listeners are stopped **globally** for all tenants during any single tenant's initialization
- Stop/start operations are **idempotent** - calling stop on an already-stopped listener is a no-op
- Listeners are **always** restarted via `finally` block, even if tenant operation fails with an exception
- Message backlog during suspension is acceptable - events buffer in Kafka and process after restart
- If `startKafkaListeners()` fails in finally block, error is logged but does not mask the original exception

## Operations performed during tenant initialization

| Phase | Operation | Purpose |
|-------|-----------|---------|
| Pre-migration | `beforeTenantUpdate()` | Custom preparation before tenant changes |
| Pre-migration | `beforeLiquibaseUpdate()` | Prepare for schema changes |
| **Migration** | **Liquibase migrations** | **Create/alter tables, columns, types, indexes** |
| Post-migration | `afterLiquibaseUpdate()` | React to completed schema changes |
| Post-migration | `afterTenantUpdate()` | Refresh Keycloak token, merge duplicate capabilities |
| Data loading | `loadReferenceData()` | Load predefined roles and policies from JSON |
| Data loading | `loadSampleData()` | Load sample/test data (if requested) |

## Dependencies and interactions

- **Consumed by**: FOLIO Okapi (module enablement), `mgr-tenant-entitlements` (tenant orchestration)
- **Depends on**:
  - `KafkaAdminService` - provides `stopKafkaListeners()` and `startKafkaListeners()` methods
  - `TenantController` from folio-spring-support - parent class handling tenant lifecycle
  - `FolioSpringLiquibase` - executes database migrations
- **Events affected**: All Kafka events from `mgr-tenant-entitlements.capability` topic are paused during tenant operations

## Multi-tenant behavior

- Kafka is **global/shared** across all tenants in the application instance
- Each tenant has its own **database schema** but shares Kafka infrastructure
- Stopping Kafka globally during ANY tenant's migration temporarily affects all tenants

## Problem this feature solves

Before this feature, Kafka listeners would continue processing events during Liquibase migrations, causing errors like:

```
org.postgresql.util.PSQLException: ERROR: relation "capability" does not exist
  Position: 13
    at org.folio.roles.integration.kafka.CapabilityEventProcessor.process(...)
```

The fix ensures the database schema is fully ready before any Kafka event processing occurs.
