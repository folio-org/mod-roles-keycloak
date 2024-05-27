## Version `v1.5.0` (in progress)

## Version `v1.4.0` (25.05.2024)
### Changes:
* Update dependencies

## Version `v1.3.1` (07.05.2024)
### Changes:
* Added permission mapping for `invoices.bypass-acquisition-units` (EUREKA-96)
* Capability mapping overrides (MODROLESKC-186)

## Version `v1.3.0` (16.04.2024)
### Changes:
* Added onDelete cascade constraint to role_capability_set with role association (MODROLESKC-157)
* Set updatedBy to the same value as createdBy in migration script (MODROLESKC-8)
* Supported TLS for Keycloak clients (MODROLESKC-160)
* Adjusted `permissions/user/{userid}` endpoint with desiredPermission parameter (MODUSERSKC-29).

## Version `v1.2.0` (26.03.2024)
* Update capabilities instead of skipping (MODROLESKC-171)
* Add folio-permission name to capability-set (MODROLESKC-172)

---
## Version `v1.1.1` (08.03.2024)
### Changes:
* Added default/reference roles (MODROLESKC-7).

---
## Version `v1.1.0` (27.02.2024)
### Changes:
* Added onDelete cascade constraints to role_capability, user_roles, policy_roles associations (MODROLESKC-157).
* Updated mappings-overrides.json and include additional permissions (MODROLESKC-154).
* Fixed loading reference data multiple times (MODROLESKC-150).
