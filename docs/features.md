# Module Features

This module provides the following features:

| Feature | Description | Status |
|---------|-------------|--------|
| [Kafka Listener Suspension During Tenant Initialization](features/tenant-initialization-kafka-suspension.md) | Stops Kafka listeners during `POST /_/tenant` to prevent race conditions with Liquibase database migrations | Active |
| [User Permissions Cache](features/user-permissions-cache.md) | Caches user permission lookups with tenant-scoped eviction on role/capability changes | Active |
| [Capability Event Deduplication](features/capability-event-deduplication.md) | De-duplicates capabilities by generated name and merges single-endpoint PUT/PATCH pairs for the same path | Active |
| [Keycloak Assignment Compensation](features/keycloak-assignment-compensation.md) | Reverses Keycloak side-effects when a DB write fails during role/capability assignment; concurrent per-path permission operations reduce latency for bulk assignments | Active |

## Quick Reference

- **Caching**:
  - `user-permissions`: Caches user permission lookups with 30-minute TTL, 1000 entry max
  - `keycloak-users`: Keycloak user lookup cache (180s TTL, 250 max)
  - `keycloak-user-id`: Keycloak user ID cache (180s TTL, 250 max)
  - `authorization-client-cache`: Keycloak authorization client cache (3600s TTL, 100 max)

- **Events**:
  - **Published**: `UserPermissionsChangedEvent`, `TenantPermissionsChangedEvent`
  - **Consumed**: Kafka events from `mgr-tenant-entitlements.capability` topic (CREATE, UPDATE, DELETE for capabilities)

- **External APIs**:
  - Keycloak Admin API (for role/policy/user management)
  - FOLIO permission mapping source (via `FOLIO_PERMISSIONS_MAPPING_SOURCE_PATH`)
