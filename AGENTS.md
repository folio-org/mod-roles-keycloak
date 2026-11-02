# mod-roles-keycloak

Roles, policies, and role/user capability assignments for the FOLIO Eureka authorization model, stored in Postgres and mirrored to Keycloak. Capabilities arrive as Kafka events from mgr-tenant-entitlements.

## Commands

```bash
mvn clean package -DskipTests  # build
mvn test                       # unit tests (@UnitTest)
mvn verify                     # unit and integration tests (@IntegrationTest, *IT.java); needs Docker
mvn test -Dtest='RoleServiceTest$Create#positive_singleRole'  # one unit test in a @Nested class; quotes keep the $
mvn verify -Dit.test=RoleKeycloakIT#createRole_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # one IT, unit tests skipped
mvn verify -Pcoverage          # JaCoCo report; the 80% instruction rule does not fail the build
mvn test -Dcheckstyle.skip=true  # skip checkstyle while iterating
```

Integration tests start Postgres, Kafka, and Keycloak (TLS) with Testcontainers; WireMock stubs other modules.

Checkstyle runs at `process-classes`, so every build above checks main and test sources. The base rules come from `folio-java-checkstyle`. This repository allows 23-line methods in main code and turns the method-length check off for tests (`checkstyle/`). The build fails only above 20 violations, so a passing build can still add new ones: read the console output.

## Conventions

- The OpenAPI spec `src/main/resources/swagger.api/mod-roles-keycloak.yaml` generates `org.folio.roles.domain.dto` and `org.folio.roles.rest.resource` into `target/generated-sources`. Change the spec, not the generated code. A new stored field also needs the entity and a Liquibase changeset.
- Liquibase changesets live in `src/main/resources/changelog/changes/` and are included from `changelog/changelog-master.xml`. Add a new changeset instead of editing an applied one, because Liquibase validates checksums.

## Gotchas

- Only `RoleService` (create, update, delete) and `RoleMigrationService` compensate Keycloak writes, and only for errors thrown inside the method. Role SQL runs at commit, after the Keycloak call, so a commit failure is not compensated. In `POST /roles/batch`, a failure rolls back all rows but keeps the Keycloak roles already created for earlier items.
- Policy, user-role, and role/user capability services call Keycloak inside the database transaction without compensation: a failure rolls back only the database.
- Capability events are applied in two steps. `CapabilityKafkaEventHandler.handleEvent` saves capabilities and commits. Then `@TransactionalEventListener` handlers (`CapabilityEventHandler`, `CapabilitySetEventHandler`, `LoadableRoleCapabilityAssignmentProcessor`) update Keycloak permissions and loadable roles, and delete removed capabilities, in new transactions. Spring logs and drops exceptions from these after-commit handlers, so such a failure is not retried and the event is still reported as successful. `LoadableRoleCapabilityAssignmentProcessor` rethrows per-role failures, but the exception never reaches the Kafka error handler.
- The Kafka error handler (`KafkaConfiguration`) retries a capability event only while a tenant migration is in progress (Liquibase lock, or a "does not exist" SQL error) or on a unique-constraint violation (SQL state 23505). By default it retries every 1 s with no practical limit (`CAPABILITY_TOPIC_RETRY_*`). Any other error is logged and published as a failed result, without retry. The javadoc in `CapabilityEventRetryConfiguration` talks about system user creation; it is a copy-paste leftover.
- `KeycloakRoleService`, `KeycloakPolicyService`, and `KeycloakAuthorizationService` retry only `KeycloakApiException` with a 5xx status (up to 3 retries, 10 s apart, `KC_RETRY_*`), while the database transaction stays open.
- Creating a Keycloak policy or permission treats 409 as success, so repeating an assignment is safe. If an endpoint's method has no scope on the Keycloak resource, the permission is skipped with a warning; a missing resource fails the request.
- Permission migration (`PermissionMigrationService.migratePermissions`) runs in one database transaction on a background thread. If it fails, the database rolls back, including the migration error rows, but roles, user links, and permissions already created in Keycloak stay.
- `POST /policies/batch` creates each policy in its own transaction and skips failures with a warning; the response lists only the created policies.
- Tenant enable with `loadReference=true` stops all Kafka listener containers, for every tenant, while reference data loads. After every tenant update the containers restart so the topic pattern picks up new topics. Tenant purge logs and ignores a failure to remove default roles from Keycloak.
- Keycloak caches (`CacheConfig`, TTLs in `application.yml`) expire by TTL only, so a change made directly in Keycloak can stay hidden until the TTL ends. The exceptions: login and authorization clients are evicted on tenant purge, and `user-permissions` is evicted after commit on assignment changes and after every capability event.
