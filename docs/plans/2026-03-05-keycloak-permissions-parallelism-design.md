---
title: Keycloak Permissions Parallelism
date: 2026-03-05
status: proposed
---

# Keycloak Permissions Parallelism

## Context

Assignment flows for role and user capabilities/capability-sets compute endpoint changes and then call
`KeycloakAuthorizationService.createPermissions` / `deletePermissions`. The current implementation performs
per-endpoint Keycloak calls sequentially. This is a bottleneck for large assignments, and we want to parallelize
Keycloak calls while keeping transaction boundaries intact and fail-fast semantics.

## Goals

- Improve performance of permission assignment by parallelizing Keycloak calls.
- Preserve existing transaction boundaries (no after-commit async).
- Preserve fail-fast semantics and existing error behavior.
- Avoid Keycloak overload via bounded concurrency and batching.

## Non-Goals

- No changes to capability endpoint calculation logic.
- No changes to API behavior or response formats.
- No new async/background processing after commit.

## Approach Options

### Option 1: Parallelize only Keycloak permission calls (minimal changes)

Parallelize per-endpoint Keycloak create/delete inside `KeycloakAuthorizationService` with a bounded executor.
This keeps all service logic unchanged and is the smallest surface area change, but only accelerates the Keycloak
roundtrips (endpoint computation remains sequential).

### Option 2: Parallelize full assignment flow at permission boundary (recommended)

Keep endpoint computation and transaction flow unchanged, but run Keycloak permission create/delete concurrently
in bounded batches inside `KeycloakAuthorizationService`. This provides broader speedup while keeping transaction
boundaries and fail-fast semantics. Concurrency is controlled with configuration and context propagation.

### Option 3: Async after-commit processing

Defer Keycloak updates to background execution after DB commit. This improves response time but weakens
transactional semantics and introduces eventual consistency and error recovery complexity. Not chosen.

## Proposed Design

### Architecture

- Add a small executor helper (e.g., `KeycloakPermissionsExecutor`) that:
  - Splits endpoint lists into configurable batches.
  - Runs per-endpoint tasks in a bounded thread pool.
  - Propagates `FolioExecutionContext` into each task.
  - Waits for completion and fails fast on errors.
- `KeycloakAuthorizationService.createPermissions` / `deletePermissions` becomes the parallel boundary for all
  assignment flows.

### Components

- `KeycloakAuthorizationService` updated to use the executor helper and run permission calls in parallel.
- `KeycloakPermissionsExecutor` (new) encapsulates batching, concurrency, and context propagation.
- `KeycloakConfigurationProperties` extended with `permissions.parallelism` and `permissions.batchSize`.
- `application.yml` updated with defaults and env overrides.

### Data Flow

1. Assignment services compute `endpoints` as today.
2. `RolePermissionService` / `UserPermissionService` call `KeycloakAuthorizationService`.
3. `KeycloakAuthorizationService`:
   - Validates inputs and falls back to sequential behavior when `parallelism <= 1` or endpoints size <= 1.
   - Splits endpoints into batches.
   - For each batch, submits per-endpoint tasks to a bounded executor, then waits for completion.
   - Each task:
     - Sets the current Folio execution context.
     - Resolves resource and scope, builds permission, calls Keycloak, and closes the response.

### Error Handling

- Fail-fast: first error (non-409, non-skip conditions) cancels remaining tasks in the batch and throws.
- Missing scope is logged and skipped as today.
- 409 conflict is logged and treated as success as today.
- `EntityNotFoundException` for missing resource remains a failure.

### Testing

- Update `KeycloakAuthorizationServiceTest` to cover multiple endpoints without ordering assumptions.
- Add a fail-fast test for parallel execution.
- Add a small executor helper unit test for batch splitting and concurrency bounds.
- Ensure existing permission service tests remain unchanged.

## Configuration

Add to `application.yml`:

```
application:
  keycloak:
    permissions:
      parallelism: ${KC_PERMISSIONS_PARALLELISM:4}
      batch-size: ${KC_PERMISSIONS_BATCH_SIZE:50}
```

Defaults are conservative to avoid Keycloak overload, and can be tuned per environment.

## Risks and Mitigations

- **Keycloak overload**: bounded parallelism and batching limit in-flight calls.
- **Thread safety**: each task acquires its own authorization client via provider; no shared mutable state.
- **Context leakage**: explicit context propagation with `FolioExecutionContextSetter`.

## Open Questions

- None.
