---
feature_id: keycloak-assignment-compensation
title: Keycloak Assignment Compensation
updated: 2026-02-23
---

# Keycloak Assignment Compensation

## What it does

When a role or capability assignment is created, updated, or deleted, the module first applies the
change in Keycloak, then commits it to the database. If the database operation fails, the module
automatically reverses the Keycloak change (compensation). If the enclosing Spring transaction rolls
back for any subsequent reason, the same reversal is triggered via a registered transaction
synchronization.

Permission-creation and permission-deletion calls against Keycloak are processed concurrently per
resource path using virtual threads, reducing the wall-clock time of bulk assignment operations.

## Why it exists

Keycloak and the module's PostgreSQL database are separate systems with no shared transaction
boundary. Previously, a failure in the database step after a successful Keycloak call left both
systems inconsistent (e.g., permissions registered in Keycloak but the assignment absent from the
DB). The compensation mechanism restores consistency by undoing the Keycloak side-effect when the DB
write cannot be committed. Concurrent processing reduces latency for operations that touch many
endpoints.

## Entry point(s)

All REST endpoints that modify role or user assignments trigger this behavior:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/roles/{roleId}/capabilities` | Assign capabilities to a role |
| PUT | `/roles/{roleId}/capabilities` | Update capabilities assigned to a role |
| DELETE | `/roles/{roleId}/capabilities` | Remove all capability assignments from a role |
| DELETE | `/roles/{roleId}/capabilities/{capabilityId}` | Remove a single capability from a role |
| POST | `/roles/{roleId}/capability-sets` | Assign capability sets to a role |
| PUT | `/roles/{roleId}/capability-sets` | Update capability sets assigned to a role |
| DELETE | `/roles/{roleId}/capability-sets` | Remove all capability set assignments from a role |
| DELETE | `/roles/{roleId}/capability-sets/{capabilitySetId}` | Remove a single capability set from a role |
| POST | `/users/{userId}/capabilities` | Assign capabilities to a user |
| PUT | `/users/{userId}/capabilities` | Update capabilities assigned to a user |
| DELETE | `/users/{userId}/capabilities` | Remove all capability assignments from a user |
| DELETE | `/users/{userId}/capabilities/{capabilityId}` | Remove a single capability from a user |
| POST | `/users/{userId}/capability-sets` | Assign capability sets to a user |
| PUT | `/users/{userId}/capability-sets` | Update capability sets assigned to a user |
| DELETE | `/users/{userId}/capability-sets` | Remove all capability set assignments from a user |
| DELETE | `/users/{userId}/capability-sets/{capabilitySetId}` | Remove a single capability set from a user |
| POST | `/users/roles` | Assign roles to a user |
| PUT | `/users/roles` | Update roles assigned to a user |
| DELETE | `/users/{userId}/roles` | Remove all role assignments from a user |

## Business rules and constraints

- **Keycloak-first ordering**: the Keycloak operation always runs before the DB write. The
  compensation action is the logical inverse (e.g., if Keycloak permissions were created, the
  compensation deletes them).
- **Compensation on DB failure (no active transaction)**: if the DB write fails and no Spring
  transaction is active, the compensation runs synchronously and its result is logged; any
  compensation failure is attached as a suppressed exception on the primary DB exception.
- **Compensation on transaction rollback (active transaction)**: when a Spring transaction is
  active, a `TransactionSynchronization` is registered after the DB write succeeds. If the
  enclosing transaction is later rolled back (by any cause), `afterCompletion(STATUS_ROLLED_BACK)`
  fires the compensation. The compensation exception is caught and logged; it does not propagate.
- **Unknown transaction status**: if `afterCompletion` receives `STATUS_UNKNOWN` (e.g., connection
  reset during commit), compensation is intentionally skipped. The event is logged at ERROR level
  for manual reconciliation.
- **Concurrent permission operations**: `createPermissions` groups endpoints by resource path and
  submits one virtual-thread task per distinct path. `deletePermissions` submits one task per
  endpoint. All tasks must complete before the method returns; if any task fails, all failures are
  collected and the first is thrown with the rest as suppressed exceptions.
- **Blank-path endpoints skipped**: endpoints with a blank path are silently ignored by both
  `createPermissions` and `deletePermissions`.
- **Idempotency of compensation**: compensation actions (`assignRolesToUser`,
  `createPermissions`, etc.) are safe to call when the Keycloak state already reflects the
  desired outcome, because Keycloak ignores duplicate assignments.

## Error behavior (if applicable)

- If the Keycloak action itself fails, the DB write is never attempted and the exception propagates
  to the caller unchanged; no compensation is registered.
- If the compensation action fails during a transaction rollback, the exception is swallowed and
  logged at ERROR level with the message
  `"CRITICAL: Keycloak compensation action failed during transaction rollback! System may be in an inconsistent state."`.
- If the compensation action fails when running synchronously (no active transaction), it is logged
  at ERROR level and added as a suppressed exception on the DB exception before the DB exception is
  rethrown.
- If multiple concurrent Keycloak tasks fail, all exceptions are collected; the first exception is
  thrown with the rest attached as suppressed exceptions.

## Dependencies and interactions

- **Keycloak Admin API** — `KeycloakAuthorizationService` calls the Keycloak scope-permissions
  resource to create/delete permissions; `KeycloakRolesUserService` calls the Keycloak role
  assignment resource to link/unlink roles to users.
- **PostgreSQL (via Spring Data JPA)** — capability, role, and user assignment records are
  persisted in the module's own database; the DB write is the operation that is compensated on
  failure.
- **Spring transaction infrastructure** — `TransactionSynchronizationManager` is used to hook
  compensation into the Spring transaction lifecycle.
