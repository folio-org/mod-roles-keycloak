# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## About This Module

`mod-roles-keycloak` is a FOLIO Spring Boot 4 service (Java 21) that manages roles, capabilities, policies, and permission migrations in the Eureka authorization model. It proxies role/policy management to Keycloak while storing metadata in Postgres. The module also handles async migration of legacy FOLIO permissions to the new roles-based model.

## Build & Test Commands

```bash
# Build (skip tests)
mvn package -DskipTests

# Run unit tests only
mvn test

# Run a single unit test class
mvn test -Dtest=RoleServiceTest

# Run a single unit test method
mvn test -Dtest=RoleServiceTest#positive_singleRole

# Run all tests (unit + integration)
mvn verify

# Run a single integration test class
mvn verify -Dit.test=RoleKeycloakIT

# Run a single integration test method
mvn verify -Dit.test=RoleKeycloakIT#createRole_positive

# Run with coverage (80% instruction minimum)
mvn verify -Pcoverage

# Skip checkstyle during development
mvn test -Dcheckstyle.skip=true

# Generate OpenAPI-derived sources
mvn generate-sources
```

## Architecture

### Request Flow

```
HTTP → Controller (implements OpenAPI-generated interface)
     → Service (business logic, orchestration)
         → Repository (Postgres/JPA) + KeycloakXxxService (Keycloak Admin API)

Kafka (capability events) → KafkaMessageListener → CapabilityKafkaEventHandler
     → CapabilityService / CapabilitySetService → Repository + KeycloakAuthorizationService

Tenant lifecycle (/_/tenant) → CustomTenantService
     → Liquibase + ReferenceDataLoader + LoadableRoleService + CapabilitiesMergeService
```

### Key Architectural Patterns

- **Dual-write consistency**: Every mutating operation writes to both Postgres and Keycloak. Failures trigger compensating rollbacks.
- **OpenAPI-first**: All REST interfaces are generated from `src/main/resources/swagger.api/mod-roles-keycloak.yaml`. Controllers implement the generated interfaces; DTOs live in `domain.dto` (generated, do not edit manually).
- **CQL query support**: All paginated list endpoints support FOLIO CQL queries via `BaseCqlJpaRepository`.
- **Caffeine caching**: Multiple caches for Keycloak data and user permissions, evicted via Spring application events on mutations.
- **Kafka retry**: Capability events retry infinitely at 1-second intervals by default.

### Package Map

| Package | Role |
|---|---|
| `controller/` | 14 REST controllers + global `ApiExceptionHandler` |
| `domain/entity/` | JPA entities; composite keys in `key/`; migration entities in `migration/` |
| `domain/dto/` | **Generated** — do not edit |
| `domain/model/` | Internal models (`PageResult`, Spring domain events) |
| `integration/kafka/` | Kafka listener + capability event processing pipeline |
| `integration/keyclock/` | Keycloak Admin client wrappers (roles, policies, permissions, users, realms) |
| `integration/permissions/` | HTTP client to FOLIO `mod-permissions` |
| `mapper/` | MapStruct mappers (entity ↔ DTO, Keycloak model ↔ DTO) |
| `repository/` | Spring Data JPA repos; `BaseCqlJpaRepository` adds CQL support |
| `service/capability/` | Capability + capability set CRUD, role/user capability assignments, user permissions cache |
| `service/migration/` | Async permission-migration job: role creation, user assignment, error tracking |
| `service/role/` | `RoleService` (dual-write), `UserRoleService`, `RoleEntityService` |
| `service/reference/` | Loads JSON reference data (roles, policies) on tenant init |
| `service/loadablerole/` | Default/loadable role management |
| `configuration/` | Spring `@Configuration` beans: caches, JPA auditing, Keycloak client, key generators |

## Testing Standards

Tests use JUnit 5 + Mockito. Two custom meta-annotations control Maven plugin routing:
- `@UnitTest` → tagged `unit` → runs with `mvn test` (Surefire)
- `@IntegrationTest` → tagged `integration` → runs with `mvn verify` (Failsafe), activates profile `it`

**Unit tests**: extend nothing; use `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`. Strict stubbing is enforced — `@MockitoSettings(strictness = LENIENT)` is forbidden. Name tests as `methodName_scenario_expectedBehavior`.

**Repository integration tests**: extend `BaseRepositoryTest` (`@DataJpaTest` + Testcontainers Postgres).

**Full integration tests**: extend `BaseIntegrationTest` (full Spring Boot context + Kafka + Postgres + WireMock + Keycloak TLS).

Test support utilities (factories, constants, builders) live in `src/test/java/.../support/`.

## Code Generation

Do not manually edit files in `src/main/java/org/folio/roles/domain/dto/` or `src/main/java/org/folio/roles/rest/resource/` — these are generated from `src/main/resources/swagger.api/mod-roles-keycloak.yaml` by the OpenAPI Generator Maven plugin during `generate-sources`.

## Database

Liquibase manages schema: `src/main/resources/changelog/changelog-master.xml` orchestrates 40+ changesets. Always add new changesets rather than modifying existing ones.

## Checkstyle

Uses `folio-checkstyle/checkstyle.xml` with suppression overrides in `checkstyle/`. Max 20 violations allowed before build fails. Suppress config is at `checkstyle/checkstyle-suppressions.xml`.
