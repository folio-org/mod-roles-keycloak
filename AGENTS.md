# mod-roles-keycloak

FOLIO service (Java 21, Spring Boot) managing roles, capabilities, policies, and permission migrations in the Eureka authorization model. Proxies role/policy management to Keycloak; stores metadata in Postgres; handles async migration of legacy FOLIO permissions to the roles model.

## Build & Test

```bash
mvn package -DskipTests        # build, skip tests
mvn test                       # unit tests (@UnitTest)
mvn test -Dtest='RoleServiceTest$Create#positive_singleRole'   # single unit test (@Nested class; single quotes keep the $)
mvn verify                     # unit + integration (@IntegrationTest; `it` Spring profile comes from BaseIntegrationTest)
mvn verify -Dit.test=RoleKeycloakIT#createRole_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false   # single IT, skips unit tests
mvn verify -Pcoverage          # JaCoCo report; the 80% instruction rule does not fail the build
mvn test -Dcheckstyle.skip=true        # skip checkstyle while developing
mvn generate-sources           # regenerate OpenAPI sources
```

## Style

Match the repository's existing style: use maintained implementations and tests as templates for new code, and keep the structure, naming, and conventions of neighboring files. Mockito strict stubbing is enforced (`@MockitoSettings(strictness = LENIENT)` forbidden); name tests `methodName_positive_scenario` / `methodName_negative_scenario`, and inside a `@Nested` class named after the method, drop the method name (`positive_scenario`).

## Pitfalls

- **Dual-write is not uniform**: `RoleService` and `RoleMigrationService` undo Keycloak writes on failure; other services rely on the Postgres rollback only. Capability updates and deletions reach Keycloak after commit, via `@TransactionalEventListener` handlers; do not expect an in-transaction rollback of Keycloak there.
- **Kafka retry is selective**: capability events retry only for migration-in-progress errors and unique-constraint violations; every other error goes straight to the recoverer (`KafkaConfiguration`). Do not tune a retry mechanism for the other cases.
- **Keycloak caches expire by TTL only** (`CacheConfig`): a change made directly in Keycloak can stay hidden until the TTL ends. Only `user-permissions` is actively evicted on mutations (`UserPermissionsCacheEvictor`).
- Do not edit `domain/dto/` or `rest/resource/`; both are generated from the OpenAPI spec at `generate-sources`.
- Liquibase `src/main/resources/changelog/changelog-master.xml`: always add new changesets, never modify existing ones.
- Paginated list endpoints support FOLIO CQL via `BaseCqlJpaRepository`.

## Testing

- Repository ITs extend `BaseRepositoryTest` (`@DataJpaTest` + Testcontainers Postgres); full ITs extend `BaseIntegrationTest` (full context + Kafka + Postgres + WireMock + Keycloak TLS).
- Support utilities live in `src/test/java/.../support/`.
