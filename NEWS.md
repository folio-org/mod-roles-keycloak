## Version `v2.0.0` (01.11.2024)
* Increase keycloak-admin-client to v25.0.6 (KEYCLOAK-24)
* Fix setting role type from payload (MODROLESKC-226)
* Get PermissionData from mapping overrides first during capability set creation (MODUSERSKC-52)
* Create capability set with its permission for UI modules (MODUSERSKC-52)
* Permission replacement support (MODROLESKC-214)

## Version `v1.4.6` (24.09.2024)
* Added dynamic permission mapping (MODROLESKC-210)
* Extend Okapi mapping overrides (MODOKAPFAC-4)
* Implemented api to update roleCapabilities and roleCapabilitySets by name (MODROLESKC-211)
* Fixed overlapping capabilities for mod-agreement (EUREKA-287)
* Cannot enable app-platform-minimal with reference data on a new tenant (EUREKA-338)
* Cannot save a role after unchecking all capability/set checkboxes (UIROLES - 114)
* Make all enum values in endpoints response with upper case as stored in DB (MODROLESKC-216)

## Version `v1.4.5` (30.08.2024)
* Implement async migration (MODROLESKC-207)

## Version `v1.4.4` (15.08.2024)
* Fix liquibase error connected with new source type

## Version `v1.4.3` (14.08.2024)
* Handle application version upgrade in capabilities (MODROLESKC-200)
* Enable docker build in Jenkinsfile
* EPC. Remove old approach and switch to Docker Hub
* Extend the Policy with an additional flag to determine a system-generated entity (MODROLESKC-205)
* Implement upgrade operation for default roles (MODROLESKC-168)
* Use permission utils for capability creation (EUREKA-240)
* Fix(deps): bump the prod-deps group across 1 directory with 12 updates
* Add Source field to Policy entity with migration script by @Saba-Zedginidze-EPAM in #118 (MODROLESKC-202)
* Follow-up changes to main implementation: ignore technical capabilities, safely delete roles (MODROLESKC-168)
* Remove error on update operation (MODROLESKC-206)
* Ensure Keycloak user existence on creation of user-role/user-capability/user-capabilityset relation (MODROLESKC-176)

## Version `v1.4.2` (07.10.2024)
* Upgrade keycloak client to v25.0.1 (KEYCLOAK-11)

---
## Version `v1.4.1` (20.06.2024)
* Clean default roles in Keycloak during the tenant disable with purge=true (MODROLESKC-196)
* Implement upgrade operation for capabilities (MODROLESKC-138)
* Pack application to Docker Image and upload it to ECR (RANCHER-1515)
* Add mapping for invoice.item.cancel (EUREKA-96)
* Fix metadata field names (MODROLESKC-189)
* Raise unit test code coverage (MODROLESKC-138)
* Add mapping for ui-finance.fund-budget.recalculateTotals (EUREKA-174)
* Add mapping for lists.item.export.post (EUREKA-143)
* Clean default roles in Keycloak during the tenant disable with purge=true (MODROLESKC-196)

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
