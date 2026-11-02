# mod-roles-keycloak

FOLIO Java/Spring Boot service managing roles, capabilities, policies, and permission migrations in the Eureka authorization model. Proxies role/policy management to Keycloak; stores metadata in PostgreSQL (Liquibase); consumes capability events from Kafka; handles async migration of legacy FOLIO permissions to the roles model.

## Build & Test

```bash
mvn package -DskipTests        # build, skip tests
mvn test                       # unit tests (@UnitTest)
mvn test -Dtest='RoleServiceTest$Create#positive_singleRole'   # single unit test in a @Nested class
mvn verify                     # unit + integration (@IntegrationTest); needs Docker
mvn verify -Dit.test=RoleKeycloakIT#createRole_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false   # single IT
mvn verify -Pcoverage          # JaCoCo report; the 80% instruction rule does not fail the build
mvn test -Dcheckstyle.skip=true        # skip checkstyle while developing
mvn generate-sources           # regenerate OpenAPI sources
```

Checkstyle: `folio-checkstyle/checkstyle.xml` from the `folio-java-checkstyle` artifact, local overrides in `checkstyle/checkstyle-checker.properties`, suppressions in `checkstyle/checkstyle-suppressions.xml`; max 20 violations before build fails.

## Architecture

**Request flow**:
```
HTTP → Controller (OpenAPI-generated interface) → Service → Repository (Postgres/JPA) + KeycloakXxxService (Keycloak Admin API)
Kafka (capability events) → KafkaMessageListener → CapabilityKafkaEventHandler → Capability(Set)Service → Repository + KeycloakAuthorizationService
Tenant lifecycle (/_/tenant) → CustomTenantService → Liquibase + ReferenceDataLoader + LoadableRoleService + CapabilitiesMergeService
```

**Patterns**:
- **Dual-write**: mutations write Postgres + Keycloak.
- **OpenAPI-first**: interfaces generated from `src/main/resources/swagger.api/mod-roles-keycloak.yaml`; DTOs in `domain.dto` (generated, do not edit).
- **CQL**: paginated list endpoints support FOLIO CQL via `BaseCqlJpaRepository`.
- **Caching**: Caffeine for Keycloak data + user permissions, with TTL (`CacheConfig`); `user-permissions` is evicted via Spring app events after commit.
- **Kafka retry**: capability events retry (by default every 1s, no practical limit) only on tenant-migration errors and unique-constraint violations; other errors are not retried (`KafkaConfiguration`).
- **Permission migration**: the async migration job loads users' legacy permissions from `mod-permissions` (`UserPermissionsLoader`).
- **Keycloak wrappers** live in `integration/keyclock/` (the package name is spelled `keyclock`).

## Codegen & DB

- Do not edit `domain/dto/` or `rest/resource/` — generated from the OpenAPI spec at `generate-sources`.
- Liquibase `src/main/resources/changelog/changelog-master.xml`; always add new changesets, never modify existing.

## Testing

JUnit + Mockito. `@UnitTest` → `mvn test` (Surefire); `@IntegrationTest` → `mvn verify` (Failsafe).
- **Unit**: `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`; strict stubbing enforced (`@MockitoSettings(strictness=LENIENT)` forbidden); name `methodName_scenario_expectedBehavior`.
- **Repository IT**: extend `BaseRepositoryTest` (`@DataJpaTest` + Testcontainers Postgres).
- **Full IT**: extend `BaseIntegrationTest` (full context + Kafka + Postgres + WireMock + Keycloak TLS).
- Support utilities in `src/test/java/.../support/`.
