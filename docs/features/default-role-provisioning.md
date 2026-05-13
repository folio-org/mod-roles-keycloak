---
feature_id: default-role-provisioning
title: Default Role Provisioning
updated: 2026-05-13
---

# Default Role Provisioning

## What it does
`PUT /loadable-roles` creates or updates a default role from a loadable-role payload and returns the stored role representation. Submitted permissions are linked to matching capabilities or capability sets immediately when those records already exist, and unresolved permissions are completed later as matching capability data becomes available.

## Why it exists
Default roles are not managed through the generic `/roles` API in this module. This feature provides the supported path for provisioning default roles while still allowing role setup to succeed before all referenced capabilities or capability sets have been registered.

## Entry point(s)
| Method | Path | Description |
|--------|------|-------------|
| PUT | /loadable-roles | Creates or updates a default role from a loadable-role payload |

## Business rules and constraints
- The request body uses the `loadableRole` schema. `name` is required, and when permissions are supplied each permission entry must include `permissionName`.
- The service always stores the role as type `DEFAULT`, regardless of the incoming `type` value.
- If the submitted `id` or `name` matches an existing role, the existing role ID is reused and the role is updated instead of creating a second default role.
- On update, the stored permission set is reconciled against the submitted list: newly submitted permissions are added, and previously stored permissions omitted from the request are removed.
- If matching capabilities or capability sets already exist for submitted permissions, they are linked during the upsert request.
- If matching capability data does not exist yet, the request can still succeed; unresolved permissions are retried after commit and are also repaired when later capability events are processed.
- Default roles cannot be created, updated, or deleted through the `/roles` API; that API rejects default-role requests with `400 Bad Request`.

## Error behavior
- Request validation failures and malformed request bodies return `400 Bad Request`.
- Service-level failures wrapped as `ServiceException` are returned as `400 Bad Request`.
- Uncaught exceptions fall back to `500 Internal Server Error`.

## Dependencies and interactions
- Keycloak role record: a successful upsert creates or updates the corresponding role in Keycloak. For newly created default roles, the Keycloak role ID becomes the persisted role ID; if the database write then fails, the service deletes the newly created Keycloak role as rollback.
- Capability and capability-set records: during the upsert request, submitted permission names are matched against existing capabilities and capability sets. Matches create role-to-capability or role-to-capability-set links immediately and populate stored `capabilityId` or `capabilitySetId` values for the affected loadable permissions.
- Kafka capability events: if a submitted permission is still unresolved when the request commits, later capability `CREATE` and `UPDATE` events consumed from the `mgr-tenant-entitlements.capability` topic pattern are used to backfill the missing role-capability links for matching permissions.
