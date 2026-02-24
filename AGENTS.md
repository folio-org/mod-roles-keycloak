# AGENTS.md - Coding Agent Guidelines for mod-roles-keycloak

## Project Overview

**mod-roles-keycloak** is a Spring Boot 3.5.7 microservice (Java 21) that implements role-based authorization for the FOLIO open library platform. It bridges legacy permission-based systems with a modern capability-driven authorization model (Eureka architecture) while integrating with Keycloak for authentication and authorization.

## Build & Development Commands

### Building the Project
```bash
# Clean build (includes auto-clean on initialize phase)
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Run only unit tests (tagged with @Tag("unit"))
mvn test

# Run integration tests (tagged with @Tag("integration"))
mvn verify

# Run tests with coverage report
mvn clean verify -Pcoverage

# Run checkstyle validation
mvn checkstyle:check
```

### Running Tests
```bash
# Run a single test class
mvn test -Dtest=RoleServiceTest

# Run a single test method
mvn test -Dtest=RoleServiceTest#testCreateRole

# Run integration tests only
mvn failsafe:integration-test

# Run with specific test group
mvn test -Dgroups=unit
```

### Code Quality
```bash
# Check code style violations
mvn checkstyle:check

# Generate coverage report (in target/site/jacoco-aggregate/)
mvn clean verify -Pcoverage

# View checkstyle results
cat target/checkstyle-result.xml
```

### API Documentation
```bash
# Generate API docs (HTML output in target/apidocs/)
mvn clean package
# View: target/apidocs/mod-roles-keycloak.html
```

### Local Development
The module requires:
- PostgreSQL database
- Keycloak server
- Kafka broker (for capability events)

Use docker-compose or testcontainers for local setup. Integration tests automatically start required containers via Testcontainers.

## Architecture Overview

### Core Domain Model

The system operates on three main abstractions:

**Capabilities**: Granular permissions representing a specific action on a resource
- Structure: `resource` + `action` + `type` (UI/PROCEDURAL/DATA_OPERATION)
- Example: "Users" resource + "create" action = users.create capability
- Stored in: `capability` table
- Endpoints protected: Stored in `capability_endpoint` table

**Roles**: Named groupings of capabilities assigned to users
- Types: REGULAR (user-created) or DEFAULT (system-provided)
- Stored in Keycloak realm + local `role` table for metadata
- Relationships: Many-to-many with capabilities via `role_capability` table

**Capability Sets**: Logical groupings of capabilities (similar to legacy permission sets)
- Used for migrating hierarchical permission structures
- Related to roles via `role_capability_set` table

### Key Integration Points

**Keycloak Integration** (src/main/java/org/folio/roles/integration/keyclock/):
- Keycloak serves as the source of truth for roles and authorization policies
- All role/policy operations are dual-written: Keycloak first, then local DB
- Rollback mechanism: If DB write fails after Keycloak succeeds, Keycloak changes are reverted
- Retry logic: Configurable via `KC_RETRY_MAX_ATTEMPTS` and `KC_RETRY_BACKOFF_DELAY_MS`
- Key services: `KeycloakRoleService`, `KeycloakPolicyService`, `KeycloakRolesUserService`

**Kafka Event Processing** (src/main/java/org/folio/roles/integration/kafka/):
- Listens to capability events from `mgr-tenant-entitlements` module
- Topic pattern: `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability`
- Flow: `KafkaMessageListener` → `CapabilityKafkaEventHandler` → `CapabilityEventProcessor` → `CapabilityService`
- Suspension: Kafka listeners are suspended during tenant initialization to prevent race conditions
- Retry: Configurable retry with delay for uninitialized tenants

**Multi-Tenancy**:
- Each tenant has a dedicated Keycloak realm
- Tenant context provided via `FolioExecutionContext` (from folio-spring-support)
- Thread-scoped execution with `FolioExecutionContextSetter`
- System operations use `SystemUserScopedExecutionService`

### Service Layer Architecture

**Primary Services** (src/main/java/org/folio/roles/service/):

- `RoleService`: Role lifecycle (CRUD, search with CQL)
  - Pattern: Keycloak first → Local DB → Rollback on failure
  - Delegates to `KeycloakRoleService` + `RoleEntityService`

- `CapabilityService`: Central hub for capability operations
  - Manages capability CRUD and search
  - Resolves user permissions (direct + role-based)
  - Publishes domain events on changes
  - Handles capability set expansion

- `UserRoleService`: User-role assignment management
  - Creates/updates/deletes user-role relationships
  - Publishes `UserPermissionsChangedEvent` for cache invalidation

- `PolicyService`: Policy management (time-based, role-based, user-based policies)
  - Delegates to `KeycloakPolicyService` for Keycloak operations

- `MigrationService`: Asynchronous permission-to-capability migration
  - Single-threaded executor (max 1 concurrent migration per tenant)
  - Job tracking with detailed error logging
  - Continues on individual failures, logs root causes

- `LoadableRoleService`: Manages DEFAULT type roles
  - Loads predefined roles during tenant initialization
  - Cleans up from Keycloak on tenant deletion

### Caching Strategy

**Caffeine Cache** (src/main/java/org/folio/roles/configuration/CacheConfig.java):

1. **User Permissions Cache** (`userPermissions`):
   - TTL: 1800s (configurable via `USER_PERMISSIONS_CACHE_TTL`)
   - Max size: 1000 entries (configurable via `USER_PERMISSIONS_CACHE_MAX_SIZE`)
   - Shared across all tenants
   - Evicted on: Role changes, capability changes, user-role assignments
   - Handler: `UserPermissionCacheEventHandler`

2. **Permission Mappings Cache** (`permissionMappings`):
   - TTL: 60s (configurable via `CACHE_PERMISSION_MAPPINGS_TTL`)
   - Stores custom permission-to-capability mappings from remote JSON file

3. **Keycloak User ID Cache** (`keycloakUserId`):
   - TTL: 180s (configurable via `KC_USER_ID_CACHE_TTL`)
   - Maps FOLIO user ID → Keycloak user ID

**Cache Invalidation Pattern**:
```
User Role Assignment Change
    ↓
UserPermissionsChangedEvent published (Spring @EventListener)
    ↓
UserPermissionCacheEventHandler receives event
    ↓
Cache evicted for specific user ID
```

### Migration System (src/main/java/org/folio/roles/service/migration/)

**Purpose**: Migrate legacy permission-based authorization to capability-based model

**Components**:
- `MigrationService`: Orchestrates async migration jobs
- `PermissionMigrationService`: Main migration workflow
- `MigrationRoleCreator`: Creates roles in Keycloak + DB with consistency
- `UserPermissionsLoader`: Loads legacy user permissions
- `RolePermissionAssignor`: Assigns migrated roles to users
- `MigrationErrorService`: Tracks and queries migration errors
- `CapabilitiesMergeService`: Removes duplicate capabilities during tenant init

**Migration Flow**:
1. Validate no active migration exists
2. Create job entity (status: IN_PROGRESS)
3. Load legacy permissions from old system
4. Create roles (Keycloak → DB with rollback)
5. Assign roles to users in batches
6. Update job status (COMPLETED/FAILED)
7. Store detailed errors for failed operations

**API Endpoints**:
- `POST /roles/migrations` - Start migration (returns job ID)
- `GET /roles/migrations/{jobId}` - Get job status
- `GET /roles/migrations` - List all jobs (supports CQL)
- `GET /roles/migrations/{jobId}/errors` - Query errors (supports CQL)
- `DELETE /roles/migrations/{jobId}` - Delete completed job

### Tenant Lifecycle (src/main/java/org/folio/roles/service/CustomTenantService.java)

**Initialization Flow**:
1. Liquibase runs DB migrations
2. Reference data loaded (`ReferenceDataLoader` implementations)
3. Loadable (DEFAULT) roles loaded
4. Duplicate capabilities merged (`CapabilitiesMergeService`)
5. Fresh Keycloak token issued

**Adding Duplicate Capability Pairs**:
Edit `CapabilitiesMergeService.mergeDuplicateCapabilities()` in src/main/java/org/folio/roles/service/migration/CapabilitiesMergeService.java:
```java
capabilityDuplicateMigrationService.migrate(
  "old_capability_name",
  "new_capability_name");
```

## Common Development Patterns

### Creating a New REST Endpoint

1. Define in OpenAPI spec: `src/main/resources/swagger.api/mod-roles-keycloak.yaml`
2. Run `mvn generate-sources` to generate API interfaces
3. Implement controller in `src/main/java/org/folio/roles/controller/`
4. Add service method in corresponding service layer
5. Write unit tests with `@Tag("unit")` annotation
6. Write integration tests with `@Tag("integration")` annotation

### Adding a New Domain Entity

1. Create entity class in `src/main/java/org/folio/roles/domain/entity/`
2. Extend `Auditable` for metadata fields (createdBy, createdDate, etc.)
3. Use `@GeneratedValue(generator = "folio-uuid")` for UUID generation
4. Create repository interface extending `BaseCqlJpaRepository<Entity, ID>`
5. Add Liquibase changelog in `src/main/resources/db/changelog/changes/`
6. Create MapStruct mapper in `src/main/java/org/folio/roles/mapper/entity/`
7. Add service layer methods

### Working with Keycloak

Always use the abstraction services in `src/main/java/org/folio/roles/integration/keyclock/`:
- Don't directly instantiate Keycloak clients
- Use `@Retryable` annotation for resilience
- Handle `KeycloakApiException` appropriately
- Implement dual-write pattern: Keycloak first, DB second, rollback on failure

Example pattern:
```java
// 1. Create in Keycloak
var keycloakRole = keycloakRoleService.create(tenantId, roleRequest);
try {
  // 2. Create in local DB
  var entity = roleEntityService.create(roleRequest);
  return entity;
} catch (Exception e) {
  // 3. Rollback from Keycloak
  keycloakRoleService.deleteById(tenantId, keycloakRole.getId());
  throw e;
}
```

### Publishing Domain Events

Use Spring's event publishing for cache invalidation and cross-component coordination:

```java
@Service
@RequiredArgsConstructor
public class MyService {
  private final ApplicationEventPublisher eventPublisher;

  public void updateRole(UUID roleId) {
    // ... update logic
    eventPublisher.publishEvent(
      DomainEvent.of(roleId, DomainEventType.UPDATE, Role.class));
  }
}
```

Listen to events with `@EventListener`:
```java
@EventListener
public void onRoleChanged(DomainEvent<Role> event) {
  // Handle event
}
```

### Testing with Testcontainers

Integration tests use Testcontainers for PostgreSQL and Keycloak:
```java
@Tag("integration")
@SpringBootTest
@Testcontainers
class MyIntegrationTest extends BaseIT {
  // Containers started automatically via BaseIT

  @Test
  void testFeature() {
    // Test implementation
  }
}
```

## Important Files & Directories

- `src/main/resources/swagger.api/mod-roles-keycloak.yaml` - OpenAPI specification
- `src/main/resources/db/changelog/` - Liquibase database migrations
- `src/main/resources/folio-permissions/mappings-overrides.json` - Custom permission mappings
- `src/main/resources/application.yaml` - Spring Boot configuration
- `descriptors/ModuleDescriptor-template.json` - FOLIO module descriptor
- `checkstyle/checkstyle-suppressions.xml` - Checkstyle rule suppressions
- `pom.xml` - Maven build configuration

## Code Organization

```
src/main/java/org/folio/roles/
├── configuration/          # Spring configuration classes
├── controller/            # REST API controllers (@RestController)
├── domain/
│   ├── dto/              # Data Transfer Objects (generated from OpenAPI)
│   ├── entity/           # JPA entities
│   ├── model/            # Domain models and events
├── exception/            # Custom exceptions
├── integration/
│   ├── kafka/           # Kafka event consumers
│   ├── keyclock/        # Keycloak integration services
│   └── permissions/     # Permission system integration
├── mapper/              # MapStruct entity-DTO mappers
├── repository/          # Spring Data JPA repositories
└── service/             # Business logic services
    ├── capability/      # Capability-related services
    ├── migration/       # Migration services
    ├── permission/      # Permission resolution services
    ├── policy/          # Policy management services
    ├── reference/       # Reference data loaders
    └── role/            # Role management services
```

## Key Environment Variables for Development

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_DATABASE=postgres

# Keycloak
KC_URL=http://keycloak:8080
KC_ADMIN_CLIENT_ID=folio-backend-admin-client
KC_LOGIN_CLIENT_SUFFIX=-login-application

# Caching
USER_PERMISSIONS_CACHE_TTL=1800s
USER_PERMISSIONS_CACHE_MAX_SIZE=1000
KC_USER_ID_CACHE_TTL=180s

# Kafka
KAFKA_CAPABILITIES_TOPIC_PATTERN=(local\.)(.*\.)mgr-tenant-entitlements.capability
CAPABILITY_TOPIC_RETRY_DELAY=1s

# FOLIO Platform (from folio-spring-support)
FOLIO_ENVIRONMENT=local
FOLIO_OKAPI_URL=http://okapi:9130
FOLIO_SYSTEM_USER_USERNAME=mod-roles-keycloak
FOLIO_SYSTEM_USER_PASSWORD=<system-user-password>
```

## Code Style & Best Practices

### Checkstyle
- Max allowed violations: 20
- Configuration: `folio-checkstyle/checkstyle.xml`
- Suppressions: `checkstyle/checkstyle-suppressions.xml`
- Run before committing: `mvn checkstyle:check`

### Test Coverage
- Target: 80% instruction coverage (enforced via JaCoCo)
- Exclusions: Domain classes, generated code, mappers, configuration
- Run with: `mvn clean verify -Pcoverage`
- Report: `target/site/jacoco-aggregate/index.html`

### Annotations
- **Unit tests**: `@Tag("unit")` + `@SpringBootTest` or `@ExtendWith(MockitoExtension.class)`
- **Integration tests**: `@Tag("integration")` + `@SpringBootTest` + `@Testcontainers`
- **Entity audit**: Extend `Auditable` for automatic `createdBy`, `updatedBy`, timestamps
- **Mappers**: Use MapStruct with `@Mapper(componentModel = "spring")`

### Lombok Usage
- Use `@RequiredArgsConstructor` for constructor injection
- Use `@Data` for simple DTOs (prefer `@Getter/@Setter` for entities)
- Use `@Slf4j` for logging
- Avoid `@Builder` on JPA entities (causes issues with proxies)

## Troubleshooting Common Issues

### Keycloak Connection Failures
- Check `KC_URL` environment variable
- Verify Keycloak server is running
- Check retry configuration: `KC_RETRY_MAX_ATTEMPTS`, `KC_RETRY_BACKOFF_DELAY_MS`
- Review TLS/SSL settings if using HTTPS

### Cache Not Invalidating
- Verify event publishing: `ApplicationEventPublisher.publishEvent()`
- Check `UserPermissionCacheEventHandler` is receiving events
- Review cache TTL settings
- Ensure tenant context is set correctly

### Kafka Events Not Processing
- Verify topic pattern matches: `KAFKA_CAPABILITIES_TOPIC_PATTERN`
- Check Kafka broker connectivity
- Review listener suspension during tenant init
- Check retry configuration: `CAPABILITY_TOPIC_RETRY_ATTEMPTS`, `CAPABILITY_TOPIC_RETRY_DELAY`

### Migration Job Stuck
- Only 1 migration can run concurrently per tenant
- Check job status: `GET /roles/migrations/{jobId}`
- Review errors: `GET /roles/migrations/{jobId}/errors`
- Delete stuck job: `DELETE /roles/migrations/{jobId}` (if status allows)

### Duplicate Capability Errors
- Add merge mapping in `CapabilitiesMergeService.mergeDuplicateCapabilities()`
- Re-run tenant initialization or upgrade
- Check logs for merge completion

## API Reference

Key REST endpoints:

**Roles**:
- `GET /roles` - List roles (CQL query support)
- `GET /roles/{roleId}` - Get role details
- `POST /roles` - Create single role
- `POST /roles/batch` - Create multiple roles
- `PUT /roles/{roleId}` - Update role
- `DELETE /roles/{roleId}` - Delete role

**User-Role Assignments**:
- `POST /users/{userId}/roles` - Assign roles to user
- `PUT /users/{userId}/roles` - Replace user's roles
- `DELETE /users/{userId}/roles/{roleId}` - Remove role from user
- `GET /users/{userId}/roles` - List user's roles

**Capabilities**:
- `GET /capabilities` - List capabilities (supports expand, query params)
- `GET /capabilities/{capabilityId}` - Get capability details
- `GET /capabilities/users/{userId}` - Get capabilities for user

**Permissions** (Cached):
- `GET /permissions/users/{userId}` - Get effective permissions for user

**Migrations**:
- `POST /roles/migrations` - Start migration job
- `GET /roles/migrations` - List migration jobs
- `GET /roles/migrations/{jobId}` - Get migration job details
- `GET /roles/migrations/{jobId}/errors` - Get migration errors
- `DELETE /roles/migrations/{jobId}` - Delete migration job

Full API documentation: `target/apidocs/mod-roles-keycloak.html` (after build)
