---
feature_id: entitled-user-permission-filtering
title: Entitled User Permission Filtering
updated: 2026-03-24
---

# Entitled User Permission Filtering

## What it does
When `entitledOnly=true` is passed to the user permissions endpoint, the response contains only permissions that belong to applications currently entitled for the calling tenant. Permissions belonging to applications that are not entitled — or permissions that cannot be mapped to any application — are excluded from the response.

## Why it exists
In a multi-application FOLIO deployment a tenant may have capabilities registered from applications that are not currently active (entitled) for that tenant. Returning permissions from un-entitled applications can cause authorization checks to succeed for functionality the tenant has not paid for or provisioned. The `entitledOnly` filter allows callers to restrict permissions strictly to the tenant's active application estate.

## Entry point(s)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/permissions/users/{id}` | Returns user permissions; pass `entitledOnly=true` to filter to entitled applications only |

**Query parameter added by this feature:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `entitledOnly` | boolean | `false` | When `true`, restrict the response to permissions from entitled applications only |

## Business rules and constraints
- `entitledOnly=true` is applied after `onlyVisible` and `desiredPermissions` filtering. It narrows an already-filtered permission set.
- A permission is retained only when its owning application ID appears in the set of entitled applications for the current tenant.
- A permission with no application ID mapping is always dropped when `entitledOnly=true` (logged at DEBUG level).
- `entitledOnly=false` (the default) leaves existing behavior completely unchanged.
- Entitled application IDs are resolved by querying `mgr-tenant-entitlements` using limit=500, offset=0. If a tenant has more than 500 entitled applications, excess applications will not be included in the filter; permissions from those applications will be dropped.

## Error behavior
- If the `mgr-tenant-entitlements` call fails for any reason (network error, timeout, HTTP error), filtering is **skipped** and the full (unfiltered) permission set is returned. The failure is logged at WARN level. This is an intentional availability-over-correctness trade-off.

## Caching
Entitled application IDs per tenant are cached in the `tenant-entitled-applications` Caffeine cache to avoid repeated MTE calls within the same TTL window. Cache correctness relies on the TTL; there is no event-driven eviction for this cache.

| Variable | Purpose |
|----------|---------|
| `TENANT_ENTITLED_APPLICATIONS_CACHE_TTL` | How long entitled application sets are cached per tenant (default: `60s`) |
| `TENANT_ENTITLED_APPLICATIONS_CACHE_MAX_SIZE` | Maximum number of tenant cache entries (default: `1000`) |

## Configuration

| Variable | Purpose |
|----------|---------|
| `MTE_URL` | Base URL of the mgr-tenant-entitlements service (default: `http://mgr-tenant-entitlements:8081`) |
| `MTE_CLIENT_TLS_ENABLED` | Enables TLS for the MTE HTTP client (default: `false`) |
| `MTE_CLIENT_TLS_TRUSTSTORE_PATH` | Truststore file path for the MTE client |
| `MTE_CLIENT_TLS_TRUSTSTORE_PASSWORD` | Truststore password for the MTE client |
| `MTE_CLIENT_TLS_TRUSTSTORE_TYPE` | Truststore file type (e.g., `JKS`, `PKCS12`) |

## Dependencies and interactions
- **Depends on:** `mgr-tenant-entitlements` — `GET /entitlements/{tenantName}/applications` — to resolve the set of application IDs currently entitled for the tenant.
