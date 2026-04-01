---
feature_id: user-permissions-cache
title: User Permissions Cache
status: active
updated: 02-02-2026
---

# User Permissions Cache

## What it does
Caches user permission lookups to avoid expensive database queries when checking what permissions a user has. The cache is tenant-scoped, using a cache key pattern of `{tenantId}:{userId}`. Cache entries are evicted automatically when permissions change via role assignments, capability assignments, or role definition modifications.

## Why it exists
User permission lookups require joining multiple database tables (user_roles, roles, role_capabilities, capabilities) and are frequently accessed during authorization checks. Without caching, each permission check triggers database queries that impact performance. The cache reduces database load while maintaining consistency through event-driven eviction when permissions change.

## Business rules and constraints
- Cache entries are scoped to a specific tenant and user combination
- Cache eviction only affects entries for the current tenant context
- Cache eviction is best-effort - failures are logged but do not throw exceptions
- Blank or null tenant IDs result in skipped eviction with a warning
- All eviction operations execute after the database transaction commits (AFTER_COMMIT phase)

## Caching

### What is cached
The complete list of FOLIO permission names for a user (all permissions inherited from directly and indirectly assigned roles and capabilities).

### Cache key structure
`{tenantId}:{userId}` - Cache entries are tenant-scoped, ensuring multi-tenant isolation.

### Cache eviction triggers
- **User-scoped eviction**: Published when a specific user's permissions change
  - Direct role assignment/removal for a user
  - Direct capability assignment/removal for a user
  - Direct capability-set assignment/removal for a user
- **Tenant-scoped eviction**: Published when tenant-wide permission changes occur
  - Role capability/capability-set modifications (affects all users with that role)
  - Capability registry changes (affects all users in tenant)

### Configuration variables controlling cache behavior
| Variable                          | Default | Purpose                                                                                                                                      |
|-----------------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `USER_PERMISSIONS_CACHE_TTL`      | 30s     | Time after which cache entries expire if not evicted. Should be set to average user session length + 10% threshold.                          |
| `USER_PERMISSIONS_CACHE_MAX_SIZE` | 1000    | Maximum number of cache entries. **This limit is shared across all tenants** - estimate based on concurrent active users across all tenants. |

## Dependencies and interactions
- **Consumed by**: `CapabilityService.getUserPermissions()` - the main entry point for permission lookups
- **Events published**:
  - `UserPermissionsChangedEvent` - Published by `UserRoleService`, `UserCapabilityService`, `UserCapabilitySetService`
  - `TenantPermissionsChangedEvent` - Published by `RoleCapabilityService`, `RoleCapabilitySetService`, `CapabilitySetService`, `RoleEntityService`

## Related features
- This cache is part of the broader permission/capability system
